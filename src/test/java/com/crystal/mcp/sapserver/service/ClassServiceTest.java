package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.ClassSourceResult;
import com.sap.conn.jco.JCoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ClassService.
 *
 * These tests use Mockito to mock the RfcAdapter, allowing us to test
 * the business logic without requiring an actual SAP connection.
 */
@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

    @Mock
    private RfcAdapter rfcAdapter;

    @InjectMocks
    private ClassService classService;

    @BeforeEach
    void setUp() {
        // Setup runs before each test
    }

    @Test
    void testGetClassSource_Success() throws JCoException {
        // Given: Mock successful RFC response
        String className = "CL_ABAP_CHAR_UTILITIES";
        String expectedSource = "CLASS cl_abap_char_utilities DEFINITION PUBLIC.\n" +
                "  PUBLIC SECTION.\n" +
                "    CLASS-METHODS char RETURNING VALUE(result) TYPE c.\n" +
                "ENDCLASS.\n" +
                "CLASS cl_abap_char_utilities IMPLEMENTATION.\n" +
                "  METHOD char.\n" +
                "    result = 'A'.\n" +
                "  ENDMETHOD.\n" +
                "ENDCLASS.";

        RfcAdapter.RfcResponse mockResponse = new RfcAdapter.RfcResponse(
                200,
                expectedSource,
                new HashMap<>()
        );

        when(rfcAdapter.request(
                anyString(),
                eq("GET"),
                isNull(),
                any(Map.class),
                eq(""),
                eq("text/plain")
        )).thenReturn(mockResponse);

        // When: Call getClassSource
        ClassSourceResult result = classService.getClassSource(
                className,
                "active",
                "main"
        );

        // Then: Verify result
        assertThat(result).isNotNull();
        assertThat(result.source()).isEqualTo(expectedSource);
        assertThat(result.className()).isEqualTo(className);
        assertThat(result.version()).isEqualTo("active");
        assertThat(result.includeType()).isEqualTo("main");
        assertThat(result.metadata()).isNotNull();
        assertThat(result.metadata()).containsKey("uri");
    }

    @Test
    void testGetClassSource_InactiveVersion() throws JCoException {
        // Given: Mock response for inactive version
        String className = "ZTEST_CLASS";
        String expectedSource = "CLASS ztest_class DEFINITION PUBLIC.\nENDCLASS.";

        RfcAdapter.RfcResponse mockResponse = new RfcAdapter.RfcResponse(
                200,
                expectedSource,
                new HashMap<>()
        );

        when(rfcAdapter.request(
                anyString(),
                eq("GET"),
                isNull(),
                any(Map.class),
                eq(""),
                eq("text/plain")
        )).thenReturn(mockResponse);

        // When: Call with inactive version
        ClassSourceResult result = classService.getClassSource(
                className,
                "inactive",
                "main"
        );

        // Then: Verify inactive version is passed
        assertThat(result.version()).isEqualTo("inactive");
    }

    @Test
    void testGetClassSource_ImplementationInclude() throws JCoException {
        // Given: Mock response for implementation include
        String className = "CL_TEST";
        String expectedSource = "CLASS cl_test IMPLEMENTATION.\n" +
                "  METHOD constructor.\n" +
                "  ENDMETHOD.\n" +
                "ENDCLASS.";

        RfcAdapter.RfcResponse mockResponse = new RfcAdapter.RfcResponse(
                200,
                expectedSource,
                new HashMap<>()
        );

        when(rfcAdapter.request(
                anyString(),
                eq("GET"),
                isNull(),
                any(Map.class),
                eq(""),
                eq("text/plain")
        )).thenReturn(mockResponse);

        // When: Call with implementation include type
        ClassSourceResult result = classService.getClassSource(
                className,
                "active",
                "implementation"
        );

        // Then: Verify include type
        assertThat(result.includeType()).isEqualTo("implementation");
        assertThat(result.source()).contains("IMPLEMENTATION");
    }

    @Test
    void testGetClassSource_ClassNotFound() throws JCoException {
        // Given: Mock 404 response (class not found)
        String className = "NON_EXISTENT_CLASS";

        RfcAdapter.RfcResponse mockResponse = new RfcAdapter.RfcResponse(
                404,
                "Class not found",
                new HashMap<>()
        );

        when(rfcAdapter.request(
                anyString(),
                eq("GET"),
                isNull(),
                any(Map.class),
                eq(""),
                eq("text/plain")
        )).thenReturn(mockResponse);

        // When & Then: Expect exception
        assertThatThrownBy(() ->
                classService.getClassSource(className, "active", "main")
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get class source")
                .hasMessageContaining("404");
    }

    @Test
    void testGetClassSource_ServerError() throws JCoException {
        // Given: Mock 500 response (server error)
        String className = "CL_TEST";

        RfcAdapter.RfcResponse mockResponse = new RfcAdapter.RfcResponse(
                500,
                "Internal server error",
                new HashMap<>()
        );

        when(rfcAdapter.request(
                anyString(),
                eq("GET"),
                isNull(),
                any(Map.class),
                eq(""),
                eq("text/plain")
        )).thenReturn(mockResponse);

        // When & Then: Expect exception
        assertThatThrownBy(() ->
                classService.getClassSource(className, "active", "main")
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("500");
    }

    @Test
    void testGetClassSource_RfcException() throws JCoException {
        // Given: Mock JCoException
        String className = "CL_TEST";

        when(rfcAdapter.request(
                anyString(),
                eq("GET"),
                isNull(),
                any(Map.class),
                eq(""),
                eq("text/plain")
        )).thenThrow(new JCoException(0, "Connection failed"));

        // When & Then: Expect runtime exception wrapping JCoException
        assertThatThrownBy(() ->
                classService.getClassSource(className, "active", "main")
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to retrieve class source")
                .hasCauseInstanceOf(JCoException.class);
    }

    @Test
    void testGetClassSource_VerifyURIFormat() throws JCoException {
        // Given: Mock response
        String className = "ZTEST_CLASS";
        RfcAdapter.RfcResponse mockResponse = new RfcAdapter.RfcResponse(
                200,
                "source code",
                new HashMap<>()
        );

        when(rfcAdapter.request(
                anyString(),
                eq("GET"),
                isNull(),
                any(Map.class),
                eq(""),
                eq("text/plain")
        )).thenReturn(mockResponse);

        // When: Call getClassSource
        ClassSourceResult result = classService.getClassSource(
                className,
                "active",
                "main"
        );

        // Then: Verify URI format in metadata
        assertThat(result.metadata().get("uri"))
                .isEqualTo("/sap/bc/adt/oo/classes/ZTEST_CLASS/source/main");
    }
}
