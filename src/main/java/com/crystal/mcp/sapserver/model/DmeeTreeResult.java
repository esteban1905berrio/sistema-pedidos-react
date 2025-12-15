package com.crystal.mcp.sapserver.model;

import java.util.List;
import java.util.Map;

/**
 * Result model for DMEE (Data Medium Exchange Engine) Tree retrieval.
 *
 * Contains complete information about a DMEE payment format tree including:
 * - Header: metadata (tree type, ID, version, description, author, dates)
 * - Nodes: hierarchical tree structure with mapping properties
 *
 * Data Sources (SAP Tables):
 * - DMEE_TREE: Tree master data (type, ID, release flag)
 * - DMEE_TREE_T: Tree texts/descriptions
 * - DMEE_TREE_HEAD: Version-specific header (charset, param structure)
 * - DMEE_TREE_NODE: Tree nodes with mapping configuration
 * - DMEE_TREE_NODE_T: Node texts and comments
 *
 * Use Cases:
 * - Extract payment format trees for migration (S/4HANA DMEEX ↔ ECC DMEE)
 * - Analyze tree structure and mapping logic
 * - Document payment format configurations
 *
 * @param treeType DMEE tree type (e.g., "PAYM" for payments)
 * @param treeId DMEE tree ID (e.g., "ZFIE1017_CITIBANAMEX")
 * @param header tree metadata including version and configuration
 * @param nodes list of tree nodes with hierarchical structure
 * @param metadata additional metadata (FM used, version, etc.)
 */
public record DmeeTreeResult(
        String treeType,
        String treeId,
        DmeeTreeHeader header,
        List<DmeeTreeNode> nodes,
        Map<String, Object> metadata
) {

    /**
     * Header metadata for a DMEE tree.
     *
     * @param treeType DMEE tree type
     * @param treeId DMEE tree ID
     * @param description tree description from DMEE_TREE_T
     * @param version tree version number
     * @param createdBy creator username
     * @param createdOn creation date (YYYYMMDD)
     * @param changedBy last modifier username
     * @param changedOn last modification date (YYYYMMDD)
     * @param releaseFlag release status flag
     * @param dmeex DMEEX flag (S/4HANA enhanced format)
     * @param paramStructure parameter structure name
     * @param charset character set for output
     * @param versionUser version creator username
     * @param versionDate version creation date
     */
    public record DmeeTreeHeader(
            String treeType,
            String treeId,
            String description,
            String version,
            String createdBy,
            String createdOn,
            String changedBy,
            String changedOn,
            String releaseFlag,
            String dmeex,
            String paramStructure,
            String charset,
            String versionUser,
            String versionDate
    ) {}

    /**
     * A node in the DMEE tree structure.
     *
     * Nodes represent elements in the payment format: root, segments, fields, etc.
     * Each node can have mapping properties defining how data is extracted.
     *
     * @param nodeId unique node identifier within the tree
     * @param techName technical name of the node
     * @param parentId parent node ID (empty for root)
     * @param nodeType type of node (ROOT, SEGM, ELEM, etc.)
     * @param level hierarchy level (0 = root)
     * @param text node description/label
     * @param nodeComment additional node comments
     * @param length output field length
     * @param dataType data type for the field
     * @param mappingConstant constant value for mapping
     * @param mappingSourceTable source table for dynamic mapping
     * @param mappingSourceField source field for dynamic mapping
     * @param mappingExitFunction function module for custom mapping logic
     * @param conversionRule conversion rule for data transformation
     */
    public record DmeeTreeNode(
            String nodeId,
            String techName,
            String parentId,
            String nodeType,
            int level,
            String text,
            String nodeComment,
            int length,
            String dataType,
            String mappingConstant,
            String mappingSourceTable,
            String mappingSourceField,
            String mappingExitFunction,
            String conversionRule
    ) {}
}
