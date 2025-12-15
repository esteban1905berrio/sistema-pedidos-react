package com.crystal.mcp.sapserver.model;

import java.util.List;
import java.util.Map;

/**
 * Result model for BAdI Implementation (SXCI) retrieval.
 *
 * Contains complete information about a classic BAdI implementation including:
 * - Header: metadata (name, active status, author, dates)
 * - BAdI Definitions: which BAdIs this implementation covers
 * - Implementing Classes: classes that implement the BAdI interfaces
 *
 * Data Sources (SAP Tables):
 * - SXC_ATTR: Implementation attributes (name, active, author, dates)
 * - SXC_EXIT: Implementation ↔ BAdI definition relationship + filter values
 * - SXC_CLASS: Implementing class for each interface
 * - SXS_INTER: BAdI definition interfaces
 * - SXS_ATTRT: BAdI definition texts/descriptions
 *
 * @param implementationName BAdI implementation name (IMP_NAME)
 * @param header implementation metadata
 * @param badiDefinitions list of BAdI definitions this implementation covers
 * @param implementingClasses list of classes implementing the interfaces
 * @param metadata additional metadata (FM used, version, etc.)
 */
public record BadiImplementationResult(
        String implementationName,
        BadiImplementationHeader header,
        List<BadiDefinitionInfo> badiDefinitions,
        List<BadiImplementingClass> implementingClasses,
        Map<String, Object> metadata
) {

    /**
     * Header metadata for a BAdI implementation.
     *
     * @param implementationName BAdI implementation name
     * @param description implementation description
     * @param active whether the implementation is active
     * @param devclass development package
     * @param author creator username
     * @param createdOn creation date (YYYYMMDD)
     * @param changedBy last modifier username
     * @param changedOn last modification date (YYYYMMDD)
     * @param migrationEnhancement enhancement name if migrated to new framework
     */
    public record BadiImplementationHeader(
            String implementationName,
            String description,
            boolean active,
            String devclass,
            String author,
            String createdOn,
            String changedBy,
            String changedOn,
            String migrationEnhancement
    ) {}

    /**
     * Information about a BAdI definition that this implementation covers.
     *
     * @param badiName BAdI definition name (EXIT_NAME)
     * @param description BAdI description from SXS_ATTRT
     * @param filterValue filter value for this implementation (FLT_VAL)
     * @param interfaces list of interfaces this BAdI defines
     * @param isMultipleUse whether multiple implementations are allowed
     * @param isFilterDependent whether BAdI uses filter values
     */
    public record BadiDefinitionInfo(
            String badiName,
            String description,
            String filterValue,
            List<String> interfaces,
            boolean isMultipleUse,
            boolean isFilterDependent
    ) {}

    /**
     * Class that implements a BAdI interface.
     *
     * @param interfaceName interface name (e.g., IF_EX_BADI_NAME)
     * @param className implementing class name (e.g., ZCL_IMPL_BADI)
     */
    public record BadiImplementingClass(
            String interfaceName,
            String className
    ) {}
}
