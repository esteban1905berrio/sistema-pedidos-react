package com.crystal.mcp.sapserver.service;

import com.crystal.mcp.sapserver.model.TableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Manual test to preview DDL generation before creating table in SAP.
 *
 * <p>Use this to validate DDL syntax before executing the integration test.
 *
 * @author Crystal Development Team
 */
@SpringBootTest
class DdlGeneratorManualTest {

    @Autowired
    private DdlGenerator ddlGenerator;

    @Test
    void previewDdlForZTPSR013_2() {
        // Given
        String tableName = "ZTPSR013_2";
        String description = "Test Table for Integration Testing";

        List<TableField> fields = List.of(
                new TableField("mat", "matnr", true, "Material Number"),
                new TableField("gjahr", "gjahr", true, "Fiscal Year"),
                new TableField("description", "abap.char(255)", false, "Description Text")
        );

        // When
        String ddl = ddlGenerator.generateTableDdl(tableName, description, fields);

        // Then
        System.out.println("=== Generated DDL for ZTPSR013_2 ===");
        System.out.println(ddl);
        System.out.println("====================================");
    }
}
