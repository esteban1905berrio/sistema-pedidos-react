package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.ExtractionDiscovery;
import com.crystal.mcp.sapserver.model.ExtractionDiscovery.DiscoveredObject;
import com.crystal.mcp.sapserver.model.ExtractionDiscovery.DiscoverySource;
import com.crystal.mcp.sapserver.model.ExtractionDiscovery.ObjectTypeInfo;
import com.crystal.mcp.sapserver.model.ExtractionScope;
import com.crystal.mcp.sapserver.model.PackageHierarchyResult;
import com.crystal.mcp.sapserver.model.PackageObjectsResult;
import com.crystal.mcp.sapserver.model.SearchResult;
import com.crystal.mcp.sapserver.model.TransportObjectsResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for ABAP object discovery and extraction operations.
 *
 * <p>
 * This service implements the ABAP Extraction Tool (Phase 6) providing
 * discovery and extraction for 4 scopes:
 * <ul>
 * <li>{@link ExtractionScope#USER} - Objects by AUTHOR in TADIR</li>
 * <li>{@link ExtractionScope#PACKAGE} - Objects from package hierarchy
 * (recursive)</li>
 * <li>{@link ExtractionScope#TRANSPORT} - Objects from transport
 * request(s)</li>
 * <li>{@link ExtractionScope#LIST} - Specific objects by name</li>
 * </ul>
 *
 * <p>
 * <strong>Workflow Pattern:</strong>
 * <ol>
 * <li>Discovery phase: Identify objects to extract (returns summary for
 * approval)</li>
 * <li>User approval: User reviews summary before extraction</li>
 * <li>Extraction phase: Fetch source code and save to filesystem</li>
 * </ol>
 *
 * <p>
 * <strong>Thread Safety:</strong> Stateless service, thread-safe via injected
 * services.
 *
 * @see ExtractionScope
 * @see ExtractionDiscovery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbapExtractionService {

    private final PackageHierarchyService packageHierarchyService;
    private final NavigationService navigationService;
    private final TransportService transportService;
    private final SearchService searchService;
    private final RfcAdapter rfcAdapter;

    // Estimated tokens per object type for size estimation
    private static final Map<String, Double> KB_PER_OBJECT_TYPE = Map.of(
            "CLAS", 15.0,
            "PROG", 8.0,
            "FUGR", 20.0,
            "FUNC", 5.0,
            "INTF", 3.0,
            "TABL", 2.0,
            "DTEL", 0.5,
            "DOMA", 0.5,
            "DDLS", 4.0,
            "ENHO", 3.0);
    private static final double DEFAULT_KB_PER_OBJECT = 5.0;

    /**
     * Discover objects created/modified by a specific user.
     *
     * <p>
     * <strong>Implementation Strategy (ADT only):</strong>
     * <ol>
     * <li>Uses ADT REST API:
     * /sap/bc/adt/repository/informationsystem/search?userName=...</li>
     * </ol>
     *
     * <p>
     * The FM fallback (ZCX_UTIL_GET_USER_OBJECTS) has been removed as ADT covers
     * this capability.
     *
     * @param username SAP username (null for current user)
     * @return ExtractionDiscovery with discovered objects
     */
    public ExtractionDiscovery discoverUserObjects(String username) {
        String effectiveUser = (username != null && !username.trim().isEmpty())
                ? username.trim().toUpperCase()
                : null;

        log.info("Discovering objects for user: {}", effectiveUser != null ? effectiveUser : "(current)");

        List<String> warnings = new ArrayList<>();
        List<DiscoveredObject> objects;

        // Step 1: Use ADT REST API
        objects = discoverUserObjectsViaAdt(effectiveUser, warnings);

        return buildDiscoveryResult(
                ExtractionScope.USER,
                effectiveUser != null ? effectiveUser : "(current)",
                List.of(new DiscoverySource("user", effectiveUser != null ? effectiveUser : "(current)",
                        objects.size())),
                objects,
                warnings);
    }

    /**
     * Discover user objects via ADT REST API.
     *
     * <p>
     * Uses endpoint: GET /sap/bc/adt/repository/informationsystem/search
     * with parameters: operation=quickSearch, query=*, userName={username}
     *
     * @param username username to search for (null for current user)
     * @param warnings list to add any warnings to
     * @return list of discovered objects
     */
    private List<DiscoveredObject> discoverUserObjectsViaAdt(String username, List<String> warnings) {
        List<DiscoveredObject> objects = new ArrayList<>();

        try {
            String uri = "/sap/bc/adt/repository/informationsystem/search";
            Map<String, String> params = new HashMap<>();
            params.put("operation", "quickSearch");
            params.put("query", "*"); // Wildcard to get all objects

            if (username != null) {
                params.put("userName", username);
            }

            RfcAdapter.RfcResponse response = rfcAdapter.request(
                    uri, "GET", null, params, "", "application/xml");

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                objects = parseUserSearchResults(response.text(), username);
                log.info("ADT search returned {} objects for user {}", objects.size(), username);
            } else {
                String warning = String.format("ADT search returned status %d",
                        response.statusCode());
                warnings.add(warning);
                log.warn(warning);
            }

        } catch (Exception e) {
            String warning = "ADT search failed: " + e.getMessage();
            warnings.add(warning);
            log.warn(warning, e);
        }

        return objects;
    }

    /**
     * Parse ADT search results XML to list of DiscoveredObjects.
     *
     * <p>
     * Expected XML format:
     * 
     * <pre>{@code
     * <adtcore:objectReferences xmlns:adtcore="http://www.sap.com/adt/core">
     *   <adtcore:objectReference
     *     adtcore:uri=
    "/sap/bc/adt/functions/groups/y_fg_test/includes/ly_fg_testtop"
     *     adtcore:type="FUGR/I"
     *     adtcore:name="LY_FG_TESTTOP"
     *     adtcore:packageName="$TMP"
     *     adtcore:description="Description"/>
     * </adtcore:objectReferences>
     * }</pre>
     *
     * @param xmlText  XML response from ADT search
     * @param username source username for discovered objects
     * @return list of discovered objects
     */
    private List<DiscoveredObject> parseUserSearchResults(String xmlText, String username) {
        List<DiscoveredObject> objects = new ArrayList<>();

        if (xmlText == null || xmlText.isBlank()) {
            return objects;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8)));

            String ADTCORE_NS = "http://www.sap.com/adt/core";
            NodeList objectRefs = doc.getElementsByTagNameNS("*", "objectReference");

            for (int i = 0; i < objectRefs.getLength(); i++) {
                Element objRef = (Element) objectRefs.item(i);

                String uri = objRef.getAttributeNS(ADTCORE_NS, "uri");
                String type = objRef.getAttributeNS(ADTCORE_NS, "type");
                String name = objRef.getAttributeNS(ADTCORE_NS, "name");
                String packageName = objRef.getAttributeNS(ADTCORE_NS, "packageName");
                String description = objRef.getAttributeNS(ADTCORE_NS, "description");

                // Skip if missing essential fields
                if (name == null || name.isEmpty()) {
                    continue;
                }

                // Extract object type from ADT type (e.g., "FUGR/I" -> "FUGR")
                String objectType = extractObjectType(type);
                String pgmid = extractPgmid(type);

                DiscoveredObject obj = new DiscoveredObject(
                        pgmid,
                        objectType,
                        name,
                        packageName,
                        username, // author
                        description,
                        uri.isEmpty() ? null : uri,
                        username != null ? username : "(current)" // source
                );
                objects.add(obj);
            }

            log.debug("Parsed {} objects from ADT XML", objects.size());

        } catch (Exception e) {
            log.error("Error parsing ADT search results XML: {}", e.getMessage(), e);
        }

        return objects;
    }

    /**
     * Discover objects from package hierarchy (recursive).
     *
     * <p>
     * Implementation:
     * <ol>
     * <li>Get package hierarchy via PackageHierarchyService</li>
     * <li>For each package (including subpackages), get objects via
     * NavigationService</li>
     * <li>Aggregate results</li>
     * </ol>
     *
     * @param packageNames comma-separated package names
     * @param recursive    whether to include subpackages (default: true)
     * @return ExtractionDiscovery with discovered objects
     */
    public ExtractionDiscovery discoverPackageObjects(String packageNames, boolean recursive) {
        log.info("Discovering objects in packages: {} (recursive: {})", packageNames, recursive);

        List<String> warnings = new ArrayList<>();
        List<DiscoveredObject> allObjects = new ArrayList<>();
        List<DiscoverySource> sources = new ArrayList<>();

        // Parse comma-separated package names
        String[] packages = packageNames.split(",");

        for (String packageName : packages) {
            String pkg = packageName.trim().toUpperCase();
            if (pkg.isEmpty())
                continue;

            try {
                // Get all packages to process (including subpackages if recursive)
                List<String> packagesToProcess = new ArrayList<>();
                packagesToProcess.add(pkg);

                if (recursive) {
                    List<String> subPackages = getSubPackages(pkg);
                    packagesToProcess.addAll(subPackages);
                    log.debug("Package {} has {} subpackages", pkg, subPackages.size());
                }

                // Get objects from each package
                for (String currentPkg : packagesToProcess) {
                    List<DiscoveredObject> pkgObjects = getObjectsFromPackage(currentPkg);

                    if (pkgObjects.isEmpty()) {
                        warnings.add("Package " + currentPkg + " is empty or not found");
                    } else {
                        allObjects.addAll(pkgObjects);
                        sources.add(new DiscoverySource("package", currentPkg, pkgObjects.size()));
                        log.info("Found {} objects in package {}", pkgObjects.size(), currentPkg);
                    }
                }

            } catch (Exception e) {
                String error = "Error processing package " + pkg + ": " + e.getMessage();
                warnings.add(error);
                log.error(error, e);
            }
        }

        return buildDiscoveryResult(
                ExtractionScope.PACKAGE,
                packageNames,
                sources,
                allObjects,
                warnings);
    }

    /**
     * Discover objects from transport request(s).
     *
     * <p>
     * Implementation: Uses TransportService.getTransportObjects() for each
     * transport.
     *
     * @param transportNumbers comma-separated transport numbers
     * @return ExtractionDiscovery with discovered objects
     */
    public ExtractionDiscovery discoverTransportObjects(String transportNumbers) {
        log.info("Discovering objects in transports: {}", transportNumbers);

        List<String> warnings = new ArrayList<>();
        List<DiscoveredObject> allObjects = new ArrayList<>();
        List<DiscoverySource> sources = new ArrayList<>();

        // Parse comma-separated transport numbers
        String[] transports = transportNumbers.split(",");

        for (String transportNumber : transports) {
            String trkorr = transportNumber.trim().toUpperCase();
            if (trkorr.isEmpty())
                continue;

            try {
                TransportObjectsResult result = transportService.getTransportObjects(trkorr, null);

                if (!result.success()) {
                    String errorMsg = result.metadata() != null
                            ? String.valueOf(result.metadata().get("error"))
                            : "Unknown error";
                    warnings.add("Transport " + trkorr + ": " + errorMsg);
                    continue;
                }

                List<DiscoveredObject> trObjects = convertTransportObjects(result, trkorr);
                allObjects.addAll(trObjects);
                sources.add(new DiscoverySource("transport", trkorr, trObjects.size()));

                log.info("Found {} objects in transport {}", trObjects.size(), trkorr);

            } catch (Exception e) {
                String error = "Error processing transport " + trkorr + ": " + e.getMessage();
                warnings.add(error);
                log.error(error, e);
            }
        }

        return buildDiscoveryResult(
                ExtractionScope.TRANSPORT,
                transportNumbers,
                sources,
                allObjects,
                warnings);
    }

    /**
     * Discover specific objects by name.
     *
     * <p>
     * Implementation: Uses SearchService.searchObjects() to resolve object types
     * and URIs for each provided name.
     *
     * @param objectNames comma-separated object names
     * @return ExtractionDiscovery with discovered objects
     */
    public ExtractionDiscovery discoverSpecificObjects(String objectNames) {
        log.info("Discovering specific objects: {}", objectNames);

        List<String> warnings = new ArrayList<>();
        List<DiscoveredObject> allObjects = new ArrayList<>();
        List<DiscoverySource> sources = new ArrayList<>();

        // Parse comma-separated object names
        String[] names = objectNames.split(",");
        int foundCount = 0;

        for (String objectName : names) {
            String name = objectName.trim().toUpperCase();
            if (name.isEmpty())
                continue;

            try {
                // Search for exact match
                SearchResult result = searchService.searchObjects(name, 10);

                // Find exact match (case-insensitive)
                Optional<SearchResult.ObjectReference> exactMatch = result.results().stream()
                        .filter(ref -> ref.name().equalsIgnoreCase(name))
                        .findFirst();

                if (exactMatch.isPresent()) {
                    SearchResult.ObjectReference ref = exactMatch.get();
                    DiscoveredObject obj = new DiscoveredObject(
                            extractPgmid(ref.type()),
                            extractObjectType(ref.type()),
                            ref.name(),
                            ref.packageName(),
                            null, // author not available from search
                            ref.description(),
                            ref.uri(),
                            "list");
                    allObjects.add(obj);
                    foundCount++;
                    log.debug("Found object: {} ({})", name, ref.type());
                } else {
                    warnings.add("Object not found: " + name);
                    log.warn("Object not found: {}", name);
                }

            } catch (Exception e) {
                String error = "Error searching for " + name + ": " + e.getMessage();
                warnings.add(error);
                log.error(error, e);
            }
        }

        sources.add(new DiscoverySource("list", "explicit", foundCount));

        return buildDiscoveryResult(
                ExtractionScope.LIST,
                objectNames,
                sources,
                allObjects,
                warnings);
    }

    /**
     * Main discovery method - routes to specific discovery based on scope.
     *
     * @param scope scope type
     * @param input scope-specific input (username, packages, transports, or names)
     * @return ExtractionDiscovery with discovered objects
     */
    public ExtractionDiscovery discover(ExtractionScope scope, String input) {
        return switch (scope) {
            case USER -> discoverUserObjects(input);
            case PACKAGE -> discoverPackageObjects(input, true);
            case TRANSPORT -> discoverTransportObjects(input);
            case LIST -> discoverSpecificObjects(input);
        };
    }

    // ==================== Private Helper Methods ====================

    /**
     * Get subpackages of a package using PackageHierarchyService.
     */
    private List<String> getSubPackages(String packageName) {
        List<String> subPackages = new ArrayList<>();

        try {
            PackageHierarchyResult result = packageHierarchyService.getPackageHierarchy(
                    packageName, "C", true // C=children, recursive=true
            );

            if (result.success() && result.hierarchy() != null) {
                // Extract package names from hierarchy JSON
                extractPackageNamesFromHierarchy(result.hierarchy(), subPackages);
            }

        } catch (Exception e) {
            log.warn("Error getting subpackages for {}: {}", packageName, e.getMessage());
        }

        return subPackages;
    }

    /**
     * Extract package names from hierarchy JSON node.
     */
    private void extractPackageNamesFromHierarchy(JsonNode node, List<String> packages) {
        if (node == null)
            return;

        if (node.isArray()) {
            for (JsonNode item : node) {
                extractPackageNamesFromHierarchy(item, packages);
            }
        } else if (node.isObject()) {
            // Extract packageName field (was DEVCLASS)
            if (node.has("packageName")) {
                String pkgName = node.get("packageName").asText();
                if (pkgName != null && !pkgName.isEmpty()) {
                    packages.add(pkgName);
                }
            } else if (node.has("DEVCLASS")) { // Fallback for backward compatibility
                String devclass = node.get("DEVCLASS").asText();
                if (devclass != null && !devclass.isEmpty()) {
                    packages.add(devclass);
                }
            }

            // Recurse into children array if present (lowercase per log)
            if (node.has("children")) {
                extractPackageNamesFromHierarchy(node.get("children"), packages);
            } else if (node.has("hierarchy")) { // Alternative name seen in log
                extractPackageNamesFromHierarchy(node.get("hierarchy"), packages);
            } else if (node.has("CHILDREN")) { // Fallback
                extractPackageNamesFromHierarchy(node.get("CHILDREN"), packages);
            }
        }
    }

    /**
     * Get objects from a single package using NavigationService.
     */
    private List<DiscoveredObject> getObjectsFromPackage(String packageName) {
        List<DiscoveredObject> objects = new ArrayList<>();

        PackageObjectsResult result = navigationService.getPackageObjects(
                packageName,
                1000, // maxRows - get all objects
                0, // offset
                null, // no type filter
                null, // no author filter
                null, // no date from
                null // no date to
        );

        // Convert PackageObjectsResult to DiscoveredObjects
        for (PackageObjectsResult.ObjectTypeGroup group : result.objectTypes().values()) {
            for (PackageObjectsResult.ObjectInfo info : group.objects()) {
                DiscoveredObject obj = new DiscoveredObject(
                        info.pgmid(),
                        info.objectType(),
                        info.objName(),
                        info.devClass(),
                        info.author(),
                        null, // description not available
                        buildUri(info.pgmid(), info.objectType(), info.objName()),
                        packageName);
                objects.add(obj);
            }
        }

        return objects;
    }

    /**
     * Convert TransportObjectsResult to list of DiscoveredObjects.
     */
    private List<DiscoveredObject> convertTransportObjects(
            TransportObjectsResult result,
            String transportNumber) {
        List<DiscoveredObject> objects = new ArrayList<>();

        for (TransportObjectsResult.TransportObject obj : result.objects()) {
            DiscoveredObject discovered = new DiscoveredObject(
                    obj.pgmid(),
                    obj.objectType(),
                    obj.objectName(),
                    null, // devclass not available from transport
                    null, // author not available
                    null, // description not available
                    buildUri(obj.pgmid(), obj.objectType(), obj.objectName()),
                    transportNumber);
            objects.add(discovered);
        }

        return objects;
    }

    /**
     * Build ADT URI for an object.
     */
    private String buildUri(String pgmid, String objectType, String objectName) {
        if (objectType == null || objectName == null) {
            return null;
        }

        String name = objectName.toLowerCase();

        return switch (objectType.toUpperCase()) {
            case "CLAS" -> "/sap/bc/adt/oo/classes/" + name;
            case "INTF" -> "/sap/bc/adt/oo/interfaces/" + name;
            case "PROG" -> "/sap/bc/adt/programs/programs/" + name;
            case "FUGR" -> "/sap/bc/adt/functions/groups/" + name;
            case "FUNC" -> "/sap/bc/adt/functions/groups/" + name.substring(0,
                    Math.min(name.length(), 26)) + "/fmodules/" + name;
            case "TABL", "DTEL", "DOMA", "TTYP", "SHLP" ->
                "/sap/bc/adt/ddic/tables/" + name;
            case "DDLS" -> "/sap/bc/adt/ddic/ddl/sources/" + name;
            case "ENHO" -> "/sap/bc/adt/enhancements/implementations/" + name;
            default -> "/sap/bc/adt/repository/objects/" + objectType.toLowerCase() + "/" + name;
        };
    }

    /**
     * Extract PGMID from ADT type string (e.g., "CLAS/OC" -> "R3TR").
     */
    private String extractPgmid(String adtType) {
        // Most repository objects are R3TR
        return "R3TR";
    }

    /**
     * Extract object type from ADT type string (e.g., "CLAS/OC" -> "CLAS").
     */
    private String extractObjectType(String adtType) {
        if (adtType == null)
            return null;
        int slashIndex = adtType.indexOf('/');
        return slashIndex > 0 ? adtType.substring(0, slashIndex) : adtType;
    }

    /**
     * Build ExtractionDiscovery result with grouped objects and estimates.
     */
    private ExtractionDiscovery buildDiscoveryResult(
            ExtractionScope scope,
            String scopeInput,
            List<DiscoverySource> sources,
            List<DiscoveredObject> objects,
            List<String> warnings) {
        // Group objects by type
        Map<String, ObjectTypeInfo> objectsByType = objects.stream()
                .collect(Collectors.groupingBy(
                        DiscoveredObject::objectType,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> new ObjectTypeInfo(
                                        list.get(0).objectType(),
                                        ExtractionDiscovery.getTypeText(list.get(0).objectType()),
                                        list.size(),
                                        list.stream()
                                                .map(DiscoveredObject::objectName)
                                                .limit(10) // Only show first 10 names
                                                .collect(Collectors.toList())))));

        // Estimate size in MB
        double estimatedSizeMb = estimateSize(objects) / 1024.0;

        return new ExtractionDiscovery(
                scope,
                scopeInput,
                sources,
                objectsByType,
                objects.size(),
                objects,
                estimatedSizeMb,
                warnings);
    }

    /**
     * Estimate extraction size in KB based on object types.
     */
    private double estimateSize(List<DiscoveredObject> objects) {
        double totalKb = 0.0;

        for (DiscoveredObject obj : objects) {
            double kbPerObject = KB_PER_OBJECT_TYPE.getOrDefault(
                    obj.objectType(), DEFAULT_KB_PER_OBJECT);
            totalKb += kbPerObject;
        }

        return totalKb;
    }
}
