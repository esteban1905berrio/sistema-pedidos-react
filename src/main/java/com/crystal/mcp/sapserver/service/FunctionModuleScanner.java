package com.crystal.mcp.sapserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service that scans Java source code to detect custom SAP function modules used by the MCP server.
 *
 * <p>This scanner identifies FM references by analyzing:
 * <ul>
 *   <li>Direct RFC calls: {@code getFunction("Z_CX_*")} or {@code getFunction("ZCX_*")}</li>
 *   <li>String constants: {@code private static final String FM_NAME = "ZCX_*"}</li>
 *   <li>JavaDoc references: mentions of FM names in documentation</li>
 * </ul>
 *
 * <p>Used by {@link ComponentExtractionService} to auto-detect which FMs need extraction,
 * ensuring the manifest stays synchronized with actual code usage.
 *
 * <p>Thread Safety: Stateless service, thread-safe.
 */
@Slf4j
@Service
public class FunctionModuleScanner {

    /**
     * Default source directory for Java code.
     */
    private static final String DEFAULT_SOURCE_DIR = "src/main/java";

    /**
     * Patterns to match custom FM references in Java code.
     * Matches: Z_CX_*, ZCX_*, Z_CRY_*, ZCRY_* (Crystal namespace FMs)
     *
     * <p>Note: Uses DOTALL flag for patterns that may span multiple lines
     * (e.g., method calls with line breaks between parenthesis and string argument).
     */
    private static final List<Pattern> FM_PATTERNS = List.of(
            // getFunction("FM_NAME") or .getFunction("FM_NAME") - supports multiline
            Pattern.compile("getFunction\\s*\\([\\s\\S]*?\"(Z_?C[XRY]_[A-Z0-9_]+)\"", Pattern.CASE_INSENSITIVE),
            // callFunctionModule("FM_NAME", ...) - RfcAdapter direct calls - supports multiline
            Pattern.compile("callFunctionModule\\s*\\([\\s\\S]*?\"(Z_?C[XRY]_[A-Z0-9_]+)\"", Pattern.CASE_INSENSITIVE),
            // callRfc("FM_NAME", ...) - legacy RFC calls - supports multiline
            Pattern.compile("callRfc\\s*\\([\\s\\S]*?\"(Z_?C[XRY]_[A-Z0-9_]+)\"", Pattern.CASE_INSENSITIVE),
            // FUNCTION_MODULE = "FM_NAME" or FM_NAME = "..."
            Pattern.compile("(?:FUNCTION_MODULE|FM_NAME|FM_GET_\\w+)\\s*=\\s*\"(Z_?C[XRY]_[A-Z0-9_]+)\"", Pattern.CASE_INSENSITIVE),
            // private static final String ... = "ZCX_..." or "Z_CX_..."
            Pattern.compile("static\\s+final\\s+String\\s+\\w+\\s*=\\s*\"(Z_?C[XRY]_[A-Z0-9_]+)\"", Pattern.CASE_INSENSITIVE),
            // String literal containing FM name (e.g., in method calls)
            Pattern.compile("\"(Z_?C[XRY]_[A-Z0-9_]+)\"", Pattern.CASE_INSENSITIVE),
            // @code ZCX_... or {@code ZCX_...} in JavaDoc
            Pattern.compile("\\{@code\\s+(Z_?C[XRY]_[A-Z0-9_]+)\\}", Pattern.CASE_INSENSITIVE),
            // FM ZCX_... in comments/JavaDoc
            Pattern.compile("\\bFM\\s+(Z_?C[XRY]_[A-Z0-9_]+)\\b", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Scan Java source code and return set of custom function modules used.
     *
     * @param sourceDirectory path to source directory (default: src/main/java)
     * @return set of unique FM names found in the code, normalized to uppercase
     */
    public Set<String> scanForUsedFunctionModules(String sourceDirectory) {
        Path sourcePath = resolveSourcePath(sourceDirectory);
        Set<String> foundFMs = new TreeSet<>(); // TreeSet for consistent ordering

        log.info("Scanning for custom FMs in: {}", sourcePath);

        try {
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        try {
                            String content = Files.readString(file);
                            Set<String> fmsInFile = extractFMsFromContent(content);
                            if (!fmsInFile.isEmpty()) {
                                log.debug("Found FMs in {}: {}", file.getFileName(), fmsInFile);
                                foundFMs.addAll(fmsInFile);
                            }
                        } catch (IOException e) {
                            log.warn("Could not read file {}: {}", file, e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Error scanning source directory: {}", e.getMessage(), e);
        }

        log.info("Found {} custom FMs: {}", foundFMs.size(), foundFMs);
        return foundFMs;
    }

    /**
     * Scan with default source directory.
     *
     * @return set of unique FM names found
     */
    public Set<String> scanForUsedFunctionModules() {
        return scanForUsedFunctionModules(DEFAULT_SOURCE_DIR);
    }

    /**
     * Extract FM names from file content using regex patterns.
     *
     * @param content Java source file content
     * @return set of FM names found (uppercase)
     */
    private Set<String> extractFMsFromContent(String content) {
        Set<String> fms = new HashSet<>();

        for (Pattern pattern : FM_PATTERNS) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String fmName = matcher.group(1).toUpperCase();
                fms.add(fmName);
            }
        }

        return fms;
    }

    /**
     * Compare detected FMs with manifest FMs and return differences.
     *
     * @param detectedFMs  set of FMs found in Java code
     * @param manifestFMs  set of FMs defined in manifest.json
     * @return ScanDifference with missing and extra FMs
     */
    public ScanDifference compareWithManifest(Set<String> detectedFMs, Set<String> manifestFMs) {
        Set<String> missingInManifest = new TreeSet<>(detectedFMs);
        missingInManifest.removeAll(manifestFMs);

        Set<String> extraInManifest = new TreeSet<>(manifestFMs);
        extraInManifest.removeAll(detectedFMs);

        return new ScanDifference(
                missingInManifest,
                extraInManifest,
                detectedFMs.size(),
                manifestFMs.size()
        );
    }

    /**
     * Resolve source path, using default if not specified.
     */
    private Path resolveSourcePath(String sourceDirectory) {
        if (sourceDirectory == null || sourceDirectory.trim().isEmpty()) {
            return Paths.get(DEFAULT_SOURCE_DIR).toAbsolutePath().normalize();
        }
        return Paths.get(sourceDirectory).toAbsolutePath().normalize();
    }

    /**
     * Result of comparing detected FMs with manifest.
     *
     * @param missingInManifest FMs used in code but not in manifest (should be added)
     * @param extraInManifest   FMs in manifest but not used in code (candidates for removal)
     * @param detectedCount     total FMs detected in code
     * @param manifestCount     total FMs in manifest
     */
    public record ScanDifference(
            Set<String> missingInManifest,
            Set<String> extraInManifest,
            int detectedCount,
            int manifestCount
    ) {
        public boolean hasDifferences() {
            return !missingInManifest.isEmpty() || !extraInManifest.isEmpty();
        }

        public boolean isSynchronized() {
            return missingInManifest.isEmpty() && extraInManifest.isEmpty();
        }
    }
}
