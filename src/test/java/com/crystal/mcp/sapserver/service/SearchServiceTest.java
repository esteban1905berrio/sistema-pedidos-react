package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SearchService.
 *
 * Tests search_objects MCP tool functionality.
 *
 * Prerequisites:
 * - SAP connection configured via environment variables
 * - ADT installed on SAP system
 * - Test objects exist in the system
 */
@Slf4j
@SpringBootTest
class SearchServiceTest {

    @Autowired
    private SearchService searchService;

    /**
     * Test searching for a specific program that we know exists.
     */
    @Test
    void testSearchObjects_SpecificProgram() {
        // Given
        String query = "ZFIAAC002*";
        int maxResults = 50;

        log.info("===========================================");
        log.info("Testing search for query: {}", query);
        log.info("===========================================");

        // When
        SearchResult result = searchService.searchObjects(query, maxResults);

        // Then
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.results(), "Results list should not be null");

        log.info("Search completed:");
        log.info("  Query: {}", result.query());
        log.info("  Total found: {}", result.totalFound());
        log.info("  Results returned: {}", result.results().size());
        log.info("===========================================");

        // Always print all results (even if empty) for debugging
        if (result.results().isEmpty()) {
            log.warn("⚠️  NO RESULTS FOUND for query: {}", query);
            log.warn("This indicates an issue with:");
            log.warn("  1. Query format (wildcards, case sensitivity)");
            log.warn("  2. ADT API endpoint or parameters");
            log.warn("  3. Object doesn't exist in the system");
            log.warn("  4. XML parsing issue");
        }

        // Validate results
        assertTrue(result.totalFound() > 0, "Should find at least one object");

        // Print all results for debugging
        result.results().forEach(obj -> {
            log.info("  Found object:");
            log.info("    Name: {}", obj.name());
            log.info("    Type: {}", obj.type());
            log.info("    URI: {}", obj.uri());
            log.info("    Package: {}", obj.packageName());
            log.info("    Description: {}", obj.description());
        });

        // Verify structure of first result
        if (!result.results().isEmpty()) {
            SearchResult.ObjectReference first = result.results().get(0);
            assertNotNull(first.name(), "Name should not be null");
            assertNotNull(first.type(), "Type should not be null");
            assertNotNull(first.uri(), "URI should not be null");
            assertFalse(first.name().isEmpty(), "Name should not be empty");
            assertFalse(first.type().isEmpty(), "Type should not be empty");
            assertFalse(first.uri().isEmpty(), "URI should not be empty");
        }
    }

    /**
     * Test searching for standard SAP classes.
     */
    @Test
    void testSearchObjects_StandardClass() {
        // Given
        String query = "CL_ABAP_CHAR_UTILITIES";
        int maxResults = 10;

        log.info("Testing search for standard class: {}", query);

        // When
        SearchResult result = searchService.searchObjects(query, maxResults);

        // Then
        assertNotNull(result);
        log.info("Found {} objects for query '{}'", result.totalFound(), query);

        // Should find the class
        assertTrue(result.totalFound() > 0, "Should find CL_ABAP_CHAR_UTILITIES");

        // Check if we found the exact class
        boolean foundExactMatch = result.results().stream()
                .anyMatch(obj -> obj.name().equalsIgnoreCase("CL_ABAP_CHAR_UTILITIES"));

        assertTrue(foundExactMatch, "Should find exact match for CL_ABAP_CHAR_UTILITIES");
    }

    /**
     * Test searching with wildcard pattern.
     */
    @Test
    void testSearchObjects_WildcardPattern() {
        // Given
        String query = "ZFI*";
        int maxResults = 20;

        log.info("Testing wildcard search: {}", query);

        // When
        SearchResult result = searchService.searchObjects(query, maxResults);

        // Then
        assertNotNull(result);
        log.info("Found {} objects matching '{}'", result.totalFound(), query);

        // Print first 5 results
        result.results().stream()
                .limit(5)
                .forEach(obj -> log.info("  - {} ({}) in {}",
                        obj.name(), obj.type(), obj.packageName()));
    }

    /**
     * Test maxResults limit is respected.
     */
    @Test
    void testSearchObjects_MaxResultsLimit() {
        // Given
        String query = "CL_*";
        int maxResults = 5;

        log.info("Testing max results limit: {}", maxResults);

        // When
        SearchResult result = searchService.searchObjects(query, maxResults);

        // Then
        assertNotNull(result);
        assertTrue(result.results().size() <= maxResults,
                "Results should not exceed maxResults limit");

        log.info("Requested max: {}, Received: {}", maxResults, result.results().size());
    }

    /**
     * Test searching for non-existent object.
     */
    @Test
    void testSearchObjects_NoResults() {
        // Given
        String query = "ZZZZ_NONEXISTENT_OBJECT_12345";
        int maxResults = 10;

        log.info("Testing search for non-existent object: {}", query);

        // When
        SearchResult result = searchService.searchObjects(query, maxResults);

        // Then
        assertNotNull(result);
        assertEquals(0, result.totalFound(), "Should find 0 objects");
        assertTrue(result.results().isEmpty(), "Results list should be empty");

        log.info("Correctly returned empty result set");
    }

    /**
     * Test different object types are returned correctly.
     */
    @Test
    void testSearchObjects_DifferentObjectTypes() {
        // Given
        String query = "ZFIAAC002*";
        int maxResults = 50;

        log.info("Testing object type variety: {}", query);

        // When
        SearchResult result = searchService.searchObjects(query, maxResults);

        // Then
        assertNotNull(result);

        // Collect unique object types
        var objectTypes = result.results().stream()
                .map(SearchResult.ObjectReference::type)
                .distinct()
                .toList();

        log.info("Found {} different object types:", objectTypes.size());
        objectTypes.forEach(type -> log.info("  - {}", type));

        // Should find multiple types (programs, includes, function groups, etc.)
        assertTrue(objectTypes.size() > 0, "Should find at least one object type");
    }
}
