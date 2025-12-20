package com.crystal.mcp.sapserver.model;

import java.util.List;
import java.util.Map;

/**
 * Result model for Enhancement Implementation source retrieval.
 *
 * Contains header metadata, implementation elements (hooks/BAdIs),
 * and source code for each element.
 */
public record EnhancementSourceResult(
                String enhancementName,
                EnhancementHeader header,
                List<EnhancementElement> elements,
                Map<String, Object> metadata) {

        /**
         * Enhancement header information.
         */
        public record EnhancementHeader(
                        String enhancementName,
                        String description,
                        String toolType,
                        String toolTypeText,
                        String devclass,
                        String author,
                        String createdOn,
                        String changedBy,
                        String changedOn) {
        }

        /**
         * Enhancement element (hook or BAdI implementation).
         */
        public record EnhancementElement(
                        String elementType,
                        String spotName,
                        String programName,
                        String fullName,
                        String badiName,
                        String badiImpl,
                        String implClass,
                        String interfaceName,
                        Boolean active,
                        String sourceCode) {
        }
}
