package com.crystal.mcp.sapserver.prompt;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP Prompt Provider for SAP ABAP operations.
 *
 * Exposes reusable prompt templates that guide LLMs in performing
 * SAP-specific tasks with consistent quality and structure.
 *
 * MCP Prompts provide:
 * - Pre-built templates for common SAP operations
 * - Consistent output structure and quality
 * - Domain expertise encoded in prompts
 * - Reduced token usage vs ad-hoc prompting
 *
 * Available Prompts:
 * 1. review_abap_code - Code review following Crystal standards
 * 2. analyze_transport - Transport analysis with risks/dependencies
 * 3. explain_class - Class explanation (purpose, architecture)
 * 4. debug_dump - ABAP dump analysis with root cause suggestions
 * 5. migration_checklist - Pre-release/migration checklist
 * 6. generate_unit_test - Generate ABAP Unit test class
 * 7. document_function_module - Document FM with examples
 * 8. compare_versions - Compare active vs inactive versions
 *
 * Usage via MCP:
 * Client calls: prompts/get { name: "review_abap_code", arguments: { code: "..." } }
 */
@Slf4j
@Component
public class SapPromptProvider {

    // ========================================================================
    // 1. REVIEW_ABAP_CODE - Code Review Prompt
    // ========================================================================

    /**
     * Reviews ABAP code following Crystal development standards.
     *
     * Provides structured feedback on:
     * - Code quality and readability
     * - Performance considerations
     * - Security vulnerabilities
     * - Naming conventions
     * - Error handling patterns
     * - ABAP best practices
     *
     * @param code ABAP source code to review
     * @param context Optional context (purpose, requirements)
     * @return Structured code review prompt
     */
    @McpPrompt(
            name = "review_abap_code",
            title = "ABAP Code Review",
            description = "Reviews ABAP code following Crystal development standards. Analyzes quality, performance, security, and best practices."
    )
    public GetPromptResult reviewAbapCode(
            @McpArg(name = "code", description = "ABAP source code to review", required = true) String code,
            @McpArg(name = "context", description = "Optional context about the code purpose", required = false) String context
    ) {
        log.info("Prompt request: review_abap_code");

        String contextSection = (context != null && !context.isBlank())
                ? "\n\n## Context\n" + context
                : "";

        String promptText = """
            # ABAP Code Review Request

            You are an expert SAP ABAP developer performing a code review following Crystal development standards.

            ## Code to Review
            ```abap
            %s
            ```
            %s

            ## Review Guidelines

            Please analyze the code and provide feedback on:

            ### 1. Code Quality
            - Readability and maintainability
            - Proper indentation and formatting
            - Comments and documentation
            - Method/function length (max 50 lines recommended)

            ### 2. Naming Conventions
            - Variables: lv_ (local), gv_ (global), mv_ (member), cv_ (changing), iv_ (importing), ev_ (exporting)
            - Tables: lt_ (local), gt_ (global), mt_ (member)
            - Structures: ls_ (local), gs_ (global), ms_ (member)
            - Classes: ZCL_ or YCL_ prefix
            - Interfaces: ZIF_ or YIF_ prefix

            ### 3. Performance
            - SELECT efficiency (avoid SELECT *)
            - Loop optimization
            - Internal table operations
            - Database access patterns

            ### 4. Security
            - SQL injection prevention
            - Authorization checks
            - Input validation
            - Sensitive data handling

            ### 5. Error Handling
            - Exception handling
            - Return codes
            - Message handling
            - Logging practices

            ### 6. ABAP Best Practices
            - Use of modern ABAP syntax (7.40+)
            - Inline declarations where appropriate
            - String templates vs concatenation
            - Functional methods vs procedural code

            ## Output Format

            Provide your review in the following structure:

            **Summary**: Brief overall assessment (1-2 sentences)

            **Severity Levels**:
            - CRITICAL: Must fix before release
            - WARNING: Should fix, potential issues
            - INFO: Suggestions for improvement

            **Findings**: List each issue with severity, location, description, and fix suggestion

            **Positive Aspects**: What the code does well

            **Recommendations**: Top 3 priorities for improvement
            """.formatted(code, contextSection);

        return new GetPromptResult(
                "ABAP Code Review",
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }

    // ========================================================================
    // 2. ANALYZE_TRANSPORT - Transport Analysis Prompt
    // ========================================================================

    /**
     * Analyzes a transport request for risks and dependencies.
     *
     * Evaluates:
     * - Object dependencies
     * - Cross-transport conflicts
     * - Release sequence recommendations
     * - Potential risks
     *
     * @param transportNumber Transport request number
     * @param objects JSON list of objects in transport
     * @param targetSystem Target system for import
     * @return Structured transport analysis prompt
     */
    @McpPrompt(
            name = "analyze_transport",
            title = "Transport Analysis",
            description = "Analyzes SAP transport request for risks, dependencies, and release recommendations."
    )
    public GetPromptResult analyzeTransport(
            @McpArg(name = "transportNumber", description = "Transport request number (e.g., DEVK900123)", required = true) String transportNumber,
            @McpArg(name = "objects", description = "JSON array of objects in transport", required = true) String objects,
            @McpArg(name = "targetSystem", description = "Target system (DEV, QAS, PRD)", required = false) String targetSystem
    ) {
        log.info("Prompt request: analyze_transport for {}", transportNumber);

        String targetInfo = (targetSystem != null && !targetSystem.isBlank())
                ? "Target System: " + targetSystem
                : "Target System: Not specified";

        String promptText = """
            # SAP Transport Analysis Request

            You are an expert SAP Basis/Developer analyzing a transport request before release.

            ## Transport Information
            - **Transport Number**: %s
            - **%s**

            ## Objects in Transport
            ```json
            %s
            ```

            ## Analysis Requirements

            Please analyze this transport and provide:

            ### 1. Dependency Analysis
            - Identify dependencies between objects
            - Check for missing dependent objects
            - Verify table/structure dependencies
            - Identify cross-package dependencies

            ### 2. Risk Assessment
            - HIGH: Objects that could cause system issues
            - MEDIUM: Objects requiring careful testing
            - LOW: Standard changes with minimal risk

            ### 3. Release Recommendations
            - Suggested release sequence
            - Required predecessor transports
            - Post-import activities needed

            ### 4. Conflict Detection
            - Objects that might be in other open transports
            - Potential merge conflicts
            - Naming conflicts

            ### 5. Testing Checklist
            - Key test scenarios for these objects
            - Integration points to verify
            - Regression areas to check

            ## Output Format

            **Summary**: Brief transport assessment

            **Risk Level**: HIGH/MEDIUM/LOW with justification

            **Dependencies Found**: List with explanations

            **Warnings**: Issues that need attention

            **Release Checklist**: Step-by-step release process

            **Post-Import Actions**: Required follow-up tasks
            """.formatted(transportNumber, targetInfo, objects);

        return new GetPromptResult(
                "Transport Analysis: " + transportNumber,
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }

    // ========================================================================
    // 3. EXPLAIN_CLASS - Class Explanation Prompt
    // ========================================================================

    /**
     * Explains an ABAP class structure and purpose.
     *
     * Provides:
     * - Purpose and responsibility
     * - Architecture pattern used
     * - Key methods and their roles
     * - Usage examples
     *
     * @param className Class name
     * @param definition Class definition source
     * @param implementation Optional implementation source
     * @return Structured class explanation prompt
     */
    @McpPrompt(
            name = "explain_class",
            title = "Class Explanation",
            description = "Explains ABAP class purpose, architecture, methods, and usage patterns."
    )
    public GetPromptResult explainClass(
            @McpArg(name = "className", description = "ABAP class name (e.g., ZCL_INVOICE)", required = true) String className,
            @McpArg(name = "definition", description = "Class definition source code", required = true) String definition,
            @McpArg(name = "implementation", description = "Class implementation source code", required = false) String implementation
    ) {
        log.info("Prompt request: explain_class for {}", className);

        String implSection = (implementation != null && !implementation.isBlank())
                ? "\n\n## Implementation\n```abap\n" + implementation + "\n```"
                : "";

        String promptText = """
            # ABAP Class Explanation Request

            You are an expert SAP ABAP developer explaining a class to a colleague.

            ## Class: %s

            ## Definition
            ```abap
            %s
            ```
            %s

            ## Explanation Requirements

            Please provide a comprehensive explanation covering:

            ### 1. Purpose & Responsibility
            - What problem does this class solve?
            - Single Responsibility Principle adherence
            - Business domain it belongs to

            ### 2. Architecture Pattern
            - Design pattern used (Factory, Singleton, Strategy, etc.)
            - Inheritance hierarchy
            - Interface implementations
            - Dependency relationships

            ### 3. Public Interface
            - Key public methods and their purposes
            - Input/output parameters
            - Exception handling

            ### 4. Internal Structure
            - Private/protected methods
            - Internal state management
            - Helper methods

            ### 5. Usage Patterns
            - How to instantiate
            - Typical usage scenarios
            - Example code snippets

            ### 6. Dependencies
            - Required classes/interfaces
            - Database tables accessed
            - External services called

            ## Output Format

            **Summary**: One paragraph overview

            **Responsibility**: Clear statement of class purpose

            **Key Methods**:
            | Method | Purpose | Parameters |
            |--------|---------|------------|

            **Usage Example**: Code snippet showing typical usage

            **Architecture Notes**: Design decisions and patterns

            **Related Objects**: Dependencies and related classes
            """.formatted(className, definition, implSection);

        return new GetPromptResult(
                "Class Explanation: " + className,
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }

    // ========================================================================
    // 4. DEBUG_DUMP - Dump Analysis Prompt
    // ========================================================================

    /**
     * Analyzes an ABAP dump and suggests fixes.
     *
     * Provides:
     * - Root cause analysis
     * - Fix suggestions
     * - Prevention recommendations
     *
     * @param errorType Dump error type (e.g., SYNTAX_ERROR, MESSAGE_TYPE_X)
     * @param errorAnalysis Error analysis text from ST22
     * @param sourceCode Source code at error point
     * @param callStack Optional call stack
     * @return Structured dump analysis prompt
     */
    @McpPrompt(
            name = "debug_dump",
            title = "ABAP Dump Analysis",
            description = "Analyzes ABAP short dumps (ST22) and provides root cause analysis with fix suggestions."
    )
    public GetPromptResult debugDump(
            @McpArg(name = "errorType", description = "Dump error type (e.g., MESSAGE_TYPE_X, SYNTAX_ERROR)", required = true) String errorType,
            @McpArg(name = "errorAnalysis", description = "Error analysis text from ST22", required = true) String errorAnalysis,
            @McpArg(name = "sourceCode", description = "Source code at error point", required = false) String sourceCode,
            @McpArg(name = "callStack", description = "Call stack from dump", required = false) String callStack
    ) {
        log.info("Prompt request: debug_dump for error type {}", errorType);

        String sourceSection = (sourceCode != null && !sourceCode.isBlank())
                ? "\n\n## Source Code at Error Point\n```abap\n" + sourceCode + "\n```"
                : "";

        String stackSection = (callStack != null && !callStack.isBlank())
                ? "\n\n## Call Stack\n```\n" + callStack + "\n```"
                : "";

        String promptText = """
            # ABAP Dump Analysis Request

            You are an expert SAP ABAP developer debugging a short dump (runtime error).

            ## Error Information
            - **Error Type**: %s

            ## Error Analysis (from ST22)
            ```
            %s
            ```
            %s
            %s

            ## Analysis Requirements

            Please analyze this dump and provide:

            ### 1. Root Cause Analysis
            - Primary cause of the error
            - Contributing factors
            - Data conditions that triggered it

            ### 2. Error Type Explanation
            - What this error type means
            - Common scenarios that cause it
            - SAP documentation reference

            ### 3. Fix Suggestions
            - Immediate fix (code change)
            - Alternative approaches
            - Defensive coding to prevent recurrence

            ### 4. Testing Recommendations
            - Test cases to verify fix
            - Edge cases to consider
            - Regression testing needed

            ### 5. Prevention
            - How to prevent similar errors
            - Best practices violated
            - Code review checklist items

            ## Output Format

            **Summary**: One sentence description of the issue

            **Root Cause**: Clear explanation of why error occurred

            **Severity**: CRITICAL/HIGH/MEDIUM/LOW

            **Fix**:
            ```abap
            " Suggested code fix
            ```

            **Explanation**: Why this fix works

            **Prevention Checklist**: Steps to prevent recurrence
            """.formatted(errorType, errorAnalysis, sourceSection, stackSection);

        return new GetPromptResult(
                "Dump Analysis: " + errorType,
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }

    // ========================================================================
    // 5. MIGRATION_CHECKLIST - Pre-Release Checklist Prompt
    // ========================================================================

    /**
     * Generates a pre-release/migration checklist.
     *
     * Provides:
     * - Pre-release verification steps
     * - Go-live checklist
     * - Rollback plan
     *
     * @param projectName Project or feature name
     * @param objects List of objects being released
     * @param targetEnvironment Target environment (QAS, PRD)
     * @return Structured migration checklist prompt
     */
    @McpPrompt(
            name = "migration_checklist",
            title = "Migration Checklist",
            description = "Generates comprehensive pre-release/migration checklist for SAP deployments."
    )
    public GetPromptResult migrationChecklist(
            @McpArg(name = "projectName", description = "Project or feature name", required = true) String projectName,
            @McpArg(name = "objects", description = "JSON array of objects being released", required = true) String objects,
            @McpArg(name = "targetEnvironment", description = "Target environment (QAS, PRD)", required = false) String targetEnvironment
    ) {
        log.info("Prompt request: migration_checklist for {}", projectName);

        String envInfo = (targetEnvironment != null && !targetEnvironment.isBlank())
                ? targetEnvironment
                : "Production";

        String promptText = """
            # SAP Migration Checklist Request

            You are an SAP Release Manager preparing a migration checklist.

            ## Release Information
            - **Project**: %s
            - **Target Environment**: %s

            ## Objects to Release
            ```json
            %s
            ```

            ## Checklist Requirements

            Generate a comprehensive checklist covering:

            ### 1. Pre-Migration Checks
            - [ ] All unit tests passed
            - [ ] Code review completed
            - [ ] Syntax check passed
            - [ ] Extended check (SLIN) passed
            - [ ] ATC findings resolved
            - [ ] Transport dependencies documented

            ### 2. Technical Validation
            - [ ] Object activation successful
            - [ ] No orphaned objects
            - [ ] Table changes verified (append structures, includes)
            - [ ] Authorization objects reviewed
            - [ ] Background jobs identified

            ### 3. Functional Validation
            - [ ] Business process testing completed
            - [ ] Integration testing completed
            - [ ] Performance testing completed
            - [ ] User acceptance testing completed

            ### 4. Go-Live Preparation
            - [ ] Release notes prepared
            - [ ] User communication sent
            - [ ] Support team briefed
            - [ ] Monitoring alerts configured

            ### 5. Rollback Plan
            - [ ] Rollback transports prepared
            - [ ] Rollback procedure documented
            - [ ] Rollback testing completed
            - [ ] Rollback decision criteria defined

            ### 6. Post-Migration Steps
            - [ ] Smoke testing checklist
            - [ ] User verification steps
            - [ ] Monitoring period defined

            ## Output Format

            **Release Summary**: Brief description

            **Risk Assessment**: HIGH/MEDIUM/LOW

            **Checklist**: Interactive checklist with all items

            **Critical Path**: Must-complete items before go-live

            **Rollback Procedure**: Step-by-step rollback

            **Contacts**: Key personnel for escalation
            """.formatted(projectName, envInfo, objects);

        return new GetPromptResult(
                "Migration Checklist: " + projectName,
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }

    // ========================================================================
    // 6. GENERATE_UNIT_TEST - Unit Test Generation Prompt
    // ========================================================================

    /**
     * Generates ABAP Unit test class for given code.
     *
     * Creates:
     * - Test class structure
     * - Test methods for public interface
     * - Mock setup
     * - Assertions
     *
     * @param className Class name to test
     * @param definition Class definition source
     * @param methodsToTest Optional comma-separated list of methods to focus on
     * @return Structured unit test generation prompt
     */
    @McpPrompt(
            name = "generate_unit_test",
            title = "Generate Unit Test",
            description = "Generates ABAP Unit test class with test methods, mocks, and assertions."
    )
    public GetPromptResult generateUnitTest(
            @McpArg(name = "className", description = "Class name to generate tests for", required = true) String className,
            @McpArg(name = "definition", description = "Class definition source code", required = true) String definition,
            @McpArg(name = "methodsToTest", description = "Comma-separated list of methods to test (optional)", required = false) String methodsToTest
    ) {
        log.info("Prompt request: generate_unit_test for {}", className);

        String methodsFocus = (methodsToTest != null && !methodsToTest.isBlank())
                ? "\n\n## Focus Methods\nGenerate tests specifically for: " + methodsToTest
                : "";

        String promptText = """
            # ABAP Unit Test Generation Request

            You are an expert SAP ABAP developer creating unit tests using ABAP Unit framework.

            ## Class Under Test: %s

            ## Definition
            ```abap
            %s
            ```
            %s

            ## Test Generation Requirements

            Generate a complete ABAP Unit test class following these guidelines:

            ### 1. Test Class Structure
            - Use `FOR TESTING` class definition
            - Include `RISK LEVEL HARMLESS` or appropriate level
            - Use `DURATION SHORT` for unit tests
            - Implement `setup()` and `teardown()` methods

            ### 2. Test Methods
            - One test method per scenario
            - Method names: `test_<method>_<scenario>`
            - Use Given-When-Then pattern in comments
            - Include positive and negative test cases

            ### 3. Assertions
            - Use `cl_abap_unit_assert` methods
            - `assert_equals()` for value comparisons
            - `assert_initial()` for initial checks
            - `assert_not_initial()` for non-initial checks
            - `fail()` for expected exceptions

            ### 4. Test Data
            - Create test fixtures in `setup()`
            - Use meaningful test data
            - Clean up in `teardown()`

            ### 5. Mocking (if dependencies exist)
            - Create test doubles for dependencies
            - Use method redefinition for mocking
            - Isolate unit under test

            ## Output Format

            Provide complete test class code:

            ```abap
            CLASS ltc_%s DEFINITION
              FOR TESTING
              RISK LEVEL HARMLESS
              DURATION SHORT.

              " Include all test methods
            ENDCLASS.

            CLASS ltc_%s IMPLEMENTATION.
              " Include all implementations
            ENDCLASS.
            ```

            **Test Coverage**: List of scenarios covered

            **Not Covered**: Scenarios that need integration tests
            """.formatted(className, definition, methodsFocus,
                className.toLowerCase(), className.toLowerCase());

        return new GetPromptResult(
                "Unit Test Generation: " + className,
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }

    // ========================================================================
    // 7. DOCUMENT_FUNCTION_MODULE - FM Documentation Prompt
    // ========================================================================

    /**
     * Documents a function module with examples.
     *
     * Creates:
     * - Purpose description
     * - Parameter documentation
     * - Usage examples
     * - Error handling
     *
     * @param fmName Function module name
     * @param signature Function module signature
     * @param source Function module source code
     * @return Structured FM documentation prompt
     */
    @McpPrompt(
            name = "document_function_module",
            title = "Document Function Module",
            description = "Generates comprehensive documentation for ABAP function module with usage examples."
    )
    public GetPromptResult documentFunctionModule(
            @McpArg(name = "fmName", description = "Function module name", required = true) String fmName,
            @McpArg(name = "signature", description = "Function module signature (parameters)", required = true) String signature,
            @McpArg(name = "source", description = "Function module source code", required = false) String source
    ) {
        log.info("Prompt request: document_function_module for {}", fmName);

        String sourceSection = (source != null && !source.isBlank())
                ? "\n\n## Source Code\n```abap\n" + source + "\n```"
                : "";

        String promptText = """
            # Function Module Documentation Request

            You are an SAP technical writer creating documentation for a function module.

            ## Function Module: %s

            ## Signature
            ```abap
            %s
            ```
            %s

            ## Documentation Requirements

            Create comprehensive documentation covering:

            ### 1. Overview
            - Purpose and functionality
            - When to use this FM
            - Prerequisites

            ### 2. Parameters

            #### IMPORTING
            | Parameter | Type | Required | Description |
            |-----------|------|----------|-------------|

            #### EXPORTING
            | Parameter | Type | Description |
            |-----------|------|-------------|

            #### CHANGING
            | Parameter | Type | Description |
            |-----------|------|-------------|

            #### TABLES
            | Parameter | Structure | Description |
            |-----------|-----------|-------------|

            ### 3. Exceptions
            | Exception | When Raised | Handling |
            |-----------|-------------|----------|

            ### 4. Usage Examples

            **Basic Usage**:
            ```abap
            " Example code
            ```

            **With Error Handling**:
            ```abap
            " Example with TRY-CATCH or EXCEPTIONS
            ```

            ### 5. Return Values
            - Success criteria
            - Error conditions
            - Possible return codes

            ### 6. Related Function Modules
            - Similar FMs
            - Helper FMs
            - Alternative approaches

            ### 7. Notes
            - Performance considerations
            - Authorization requirements
            - Known limitations

            ## Output Format

            Provide documentation in markdown format suitable for:
            - Technical documentation wiki
            - Developer onboarding
            - Code review reference
            """.formatted(fmName, signature, sourceSection);

        return new GetPromptResult(
                "FM Documentation: " + fmName,
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }

    // ========================================================================
    // 8. COMPARE_VERSIONS - Version Comparison Prompt
    // ========================================================================

    /**
     * Compares active vs inactive versions of an object.
     *
     * Analyzes:
     * - Differences between versions
     * - Impact of changes
     * - Recommendations
     *
     * @param objectName Object name
     * @param objectType Object type (CLAS, PROG, FUNC)
     * @param activeSource Active version source
     * @param inactiveSource Inactive version source
     * @return Structured version comparison prompt
     */
    @McpPrompt(
            name = "compare_versions",
            title = "Compare Versions",
            description = "Compares active vs inactive versions of ABAP objects and analyzes changes."
    )
    public GetPromptResult compareVersions(
            @McpArg(name = "objectName", description = "Object name (e.g., ZCL_TEST)", required = true) String objectName,
            @McpArg(name = "objectType", description = "Object type (CLAS, PROG, FUNC)", required = true) String objectType,
            @McpArg(name = "activeSource", description = "Active version source code", required = true) String activeSource,
            @McpArg(name = "inactiveSource", description = "Inactive version source code", required = true) String inactiveSource
    ) {
        log.info("Prompt request: compare_versions for {} ({})", objectName, objectType);

        String promptText = """
            # ABAP Version Comparison Request

            You are an SAP ABAP developer reviewing changes before activation.

            ## Object Information
            - **Name**: %s
            - **Type**: %s

            ## Active Version (Current Production)
            ```abap
            %s
            ```

            ## Inactive Version (Pending Changes)
            ```abap
            %s
            ```

            ## Comparison Requirements

            Analyze the differences and provide:

            ### 1. Change Summary
            - What was added?
            - What was modified?
            - What was removed?

            ### 2. Detailed Diff
            ```diff
            " Show key differences in diff format
            ```

            ### 3. Impact Analysis
            - **Functional Impact**: How behavior changes
            - **Performance Impact**: Any performance implications
            - **Interface Changes**: Changes to public methods/parameters
            - **Compatibility**: Backward compatibility concerns

            ### 4. Risk Assessment
            - **HIGH**: Breaking changes, data impact
            - **MEDIUM**: Functional changes requiring testing
            - **LOW**: Minor changes, cosmetic

            ### 5. Testing Recommendations
            - Specific test scenarios for changes
            - Regression testing scope
            - Integration points to verify

            ### 6. Activation Recommendation
            - APPROVE: Safe to activate
            - REVIEW: Needs additional review
            - REJECT: Issues found, do not activate

            ## Output Format

            **Summary**: One paragraph describing changes

            **Changes**:
            | Type | Location | Description |
            |------|----------|-------------|

            **Risk Level**: HIGH/MEDIUM/LOW with justification

            **Recommendation**: APPROVE/REVIEW/REJECT with reasoning

            **Action Items**: Required steps before activation
            """.formatted(objectName, objectType, activeSource, inactiveSource);

        return new GetPromptResult(
                "Version Comparison: " + objectName,
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }

    // ========================================================================
    // 9. ANALYZE_PACKAGE_FOR_EXTRACTION - Package Extraction Analysis
    // ========================================================================

    /**
     * Analyzes a package and recommends objects for extraction.
     *
     * Used by ABAP Ripper Tool to:
     * - Identify valuable objects for training data
     * - Filter out test/demo objects
     * - Recommend extraction priorities
     *
     * @param packageName Package name to analyze
     * @param objectList JSON list of objects in package
     * @param purpose Extraction purpose (training, reference, audit)
     * @return Structured extraction recommendation prompt
     */
    @McpPrompt(
            name = "analyze_package_for_extraction",
            title = "Analyze Package for Extraction",
            description = "Analyzes SAP package contents and recommends which objects to extract for training data, reference, or audit purposes."
    )
    public GetPromptResult analyzePackageForExtraction(
            @McpArg(name = "packageName", description = "Package/devclass name (e.g., ZFI, ZMMI)", required = true) String packageName,
            @McpArg(name = "objectList", description = "JSON array of objects in package", required = true) String objectList,
            @McpArg(name = "purpose", description = "Extraction purpose: training, reference, audit", required = false) String purpose
    ) {
        log.info("Prompt request: analyze_package_for_extraction for {}", packageName);

        String purposeInfo = (purpose != null && !purpose.isBlank()) ? purpose : "training";

        String promptText = """
            # Package Extraction Analysis Request

            You are an SAP ABAP architect analyzing a package for code extraction.

            ## Package Information
            - **Package**: %s
            - **Purpose**: %s

            ## Objects in Package
            ```json
            %s
            ```

            ## Analysis Requirements

            Analyze the package and provide extraction recommendations:

            ### 1. Object Classification

            Classify each object into:
            - **INCLUDE**: High-value objects for extraction
            - **EXCLUDE**: Objects to skip (test, demo, generated)
            - **REVIEW**: Objects needing manual review

            ### 2. Inclusion Criteria (for %s)

            **High Priority** (always include):
            - Production classes with business logic
            - Interfaces defining contracts
            - Core function modules
            - CDS Views with annotations
            - Enhancement implementations

            **Medium Priority** (include if well-documented):
            - Helper/utility classes
            - Data dictionary objects
            - Forms and includes

            **Exclude**:
            - Objects starting with ZTEST*, ZDEMO*
            - Objects in $TMP
            - Generated artifacts (*_GENERATED)
            - Empty or inactive objects

            ### 3. Extraction Statistics

            Provide counts:
            - Total objects: X
            - Recommended for extraction: Y
            - Excluded: Z
            - Need review: W

            ### 4. Quality Indicators

            Flag objects with:
            - Missing documentation
            - Non-standard naming
            - Inactive status
            - Potential duplicates

            ## Output Format

            **Summary**: Package overview and recommendation

            **Extraction List**:
            | Object | Type | Priority | Reason |
            |--------|------|----------|--------|

            **Exclusion List**:
            | Object | Type | Reason for Exclusion |
            |--------|------|----------------------|

            **Quality Warnings**:
            | Object | Warning | Suggestion |
            |--------|---------|------------|

            **Recommendation**: Final extraction strategy
            """.formatted(packageName, purposeInfo, objectList, purposeInfo);

        return new GetPromptResult(
                "Package Analysis: " + packageName,
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }

    // ========================================================================
    // 10. GENERATE_EXTRACTION_REPORT - Extraction Report Generation
    // ========================================================================

    /**
     * Generates a comprehensive extraction report.
     *
     * Creates:
     * - Summary of extracted objects
     * - Quality metrics
     * - Recommendations for use
     *
     * @param extractionId Extraction identifier
     * @param statistics JSON with extraction statistics
     * @param errors JSON with extraction errors
     * @return Structured extraction report prompt
     */
    @McpPrompt(
            name = "generate_extraction_report",
            title = "Generate Extraction Report",
            description = "Generates comprehensive report for ABAP code extraction with statistics, quality metrics, and recommendations."
    )
    public GetPromptResult generateExtractionReport(
            @McpArg(name = "extractionId", description = "Extraction identifier or package name", required = true) String extractionId,
            @McpArg(name = "statistics", description = "JSON with extraction statistics", required = true) String statistics,
            @McpArg(name = "errors", description = "JSON array of extraction errors", required = false) String errors
    ) {
        log.info("Prompt request: generate_extraction_report for {}", extractionId);

        String errorSection = (errors != null && !errors.isBlank())
                ? "\n\n## Extraction Errors\n```json\n" + errors + "\n```"
                : "";

        String promptText = """
            # Extraction Report Generation Request

            You are an SAP technical analyst generating an extraction report.

            ## Extraction Information
            - **Extraction ID**: %s

            ## Statistics
            ```json
            %s
            ```
            %s

            ## Report Requirements

            Generate a comprehensive extraction report:

            ### 1. Executive Summary
            - Overall extraction status
            - Key metrics at a glance
            - Quality assessment

            ### 2. Object Statistics

            | Object Type | Count | Size | Success Rate |
            |-------------|-------|------|--------------|
            | Classes | X | Y MB | Z%% |
            | Programs | X | Y MB | Z%% |
            | ...

            ### 3. Quality Metrics

            - **Code Coverage**: Objects with documentation
            - **Naming Compliance**: Objects following conventions
            - **Activation Status**: Active vs inactive ratio
            - **Complexity**: Average lines per object

            ### 4. Error Analysis

            If errors occurred:
            - Error categorization
            - Most common failure reasons
            - Retry recommendations

            ### 5. Content Analysis

            - Business domains covered
            - Technology patterns found
            - Framework usage (RAP, CDS, OData)

            ### 6. Recommendations

            - Objects that need attention
            - Suggested follow-up extractions
            - Quality improvements needed

            ## Output Format

            **Report Title**: ABAP Extraction Report - %s

            **Date**: [Current date]

            **Status**: SUCCESS/PARTIAL/FAILED

            **Summary**: 2-3 sentence overview

            **Detailed Statistics**: Tables and metrics

            **Quality Score**: X/100 with breakdown

            **Next Steps**: Recommended actions
            """.formatted(extractionId, statistics, errorSection, extractionId);

        return new GetPromptResult(
                "Extraction Report: " + extractionId,
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }

    // ========================================================================
    // 11. EXPLAIN_CDS_VIEW - CDS View Explanation
    // ========================================================================

    /**
     * Explains a CDS View structure and associations.
     *
     * Provides:
     * - View purpose and usage
     * - Field descriptions
     * - Association explanations
     * - Annotation meanings
     *
     * @param cdsName CDS View name
     * @param source CDS View source code
     * @param context Optional business context
     * @return Structured CDS explanation prompt
     */
    @McpPrompt(
            name = "explain_cds_view",
            title = "Explain CDS View",
            description = "Explains CDS View structure, associations, annotations, and usage patterns in S/4HANA."
    )
    public GetPromptResult explainCdsView(
            @McpArg(name = "cdsName", description = "CDS View name (e.g., I_BUSINESSPARTNER, ZCDS_INVOICE)", required = true) String cdsName,
            @McpArg(name = "source", description = "CDS View DDL source code", required = true) String source,
            @McpArg(name = "context", description = "Optional business context or usage scenario", required = false) String context
    ) {
        log.info("Prompt request: explain_cds_view for {}", cdsName);

        String contextSection = (context != null && !context.isBlank())
                ? "\n\n## Business Context\n" + context
                : "";

        String promptText = """
            # CDS View Explanation Request

            You are an SAP S/4HANA expert explaining a CDS View to a developer.

            ## CDS View: %s

            ## Source Code
            ```sql
            %s
            ```
            %s

            ## Explanation Requirements

            Provide comprehensive explanation covering:

            ### 1. Purpose & Overview
            - What business entity/process does this view represent?
            - Is it a basic view, composite view, or consumption view?
            - What is the view category (analytical, transactional)?

            ### 2. Annotations Analysis

            Explain key annotations:
            - **@AbapCatalog**: Technical settings
            - **@ObjectModel**: Semantic information
            - **@UI**: Fiori UI generation
            - **@Analytics**: Analytical queries
            - **@Consumption**: Filter/parameter settings

            ### 3. Data Model

            | Field | Type | Description | Source |
            |-------|------|-------------|--------|

            ### 4. Associations

            | Association | Target | Cardinality | Purpose |
            |-------------|--------|-------------|---------|

            ### 5. Parameters (if any)

            | Parameter | Type | Purpose |
            |-----------|------|---------|

            ### 6. Usage Patterns

            **As Data Source**:
            ```abap
            " Example ABAP consumption
            ```

            **In Fiori App**:
            - How UI annotations generate UI
            - Search help behavior
            - Value help configuration

            ### 7. Performance Considerations
            - Aggregations and calculations
            - Association cardinality impact
            - Filter pushdown capabilities

            ### 8. Related Views
            - Parent/child views
            - Extension points
            - Similar standard views

            ## Output Format

            **Summary**: One paragraph overview

            **View Type**: Basic/Composite/Consumption

            **Key Annotations**: Table of important annotations

            **Field Reference**: Complete field documentation

            **Usage Example**: Code snippet for common usage

            **Best Practices**: Tips for using this view
            """.formatted(cdsName, source, contextSection);

        return new GetPromptResult(
                "CDS View Explanation: " + cdsName,
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }

    // ========================================================================
    // 12. EXPLAIN_ENHANCEMENT - Enhancement/BAdI Explanation
    // ========================================================================

    /**
     * Explains an enhancement or BAdI implementation.
     *
     * Provides:
     * - Enhancement spot/BAdI purpose
     * - Implementation details
     * - Business logic explanation
     * - Testing recommendations
     *
     * @param enhancementName Enhancement or BAdI implementation name
     * @param enhancementType Type: BADI, ENHO, CMOD, BTE
     * @param source Implementation source code
     * @param definition Optional enhancement definition
     * @return Structured enhancement explanation prompt
     */
    @McpPrompt(
            name = "explain_enhancement",
            title = "Explain Enhancement/BAdI",
            description = "Explains SAP enhancement implementations (BAdI, Enhancement Spot, CMOD, BTE) with business logic and testing guidance."
    )
    public GetPromptResult explainEnhancement(
            @McpArg(name = "enhancementName", description = "Enhancement or BAdI implementation name", required = true) String enhancementName,
            @McpArg(name = "enhancementType", description = "Type: BADI, ENHO, CMOD, BTE", required = true) String enhancementType,
            @McpArg(name = "source", description = "Implementation source code", required = true) String source,
            @McpArg(name = "definition", description = "Enhancement spot or BAdI definition", required = false) String definition
    ) {
        log.info("Prompt request: explain_enhancement for {} ({})", enhancementName, enhancementType);

        String defSection = (definition != null && !definition.isBlank())
                ? "\n\n## Enhancement Definition\n```abap\n" + definition + "\n```"
                : "";

        String promptText = """
            # Enhancement Explanation Request

            You are an SAP ABAP expert explaining an enhancement implementation.

            ## Enhancement Information
            - **Name**: %s
            - **Type**: %s

            ## Implementation
            ```abap
            %s
            ```
            %s

            ## Explanation Requirements

            Provide comprehensive explanation covering:

            ### 1. Enhancement Overview
            - What standard SAP process is being enhanced?
            - What is the business requirement?
            - When is this enhancement triggered?

            ### 2. Enhancement Technology

            Explain based on type (%s):
            - **BAdI**: Business Add-In with filter values
            - **ENHO**: Enhancement Implementation with hook
            - **CMOD**: Customer modification project
            - **BTE**: Business Transaction Event

            ### 3. Trigger Point
            - Standard program/transaction being enhanced
            - Exact point where enhancement executes
            - Execution sequence (before/after standard)

            ### 4. Implementation Logic

            Break down the code logic:
            - Input parameters used
            - Business rules applied
            - Output/modifications made
            - Exception handling

            ### 5. Data Flow

            | Input | Processing | Output |
            |-------|------------|--------|

            ### 6. Filter Values (for BAdI)

            If applicable:
            - Filter criteria
            - Multiple implementations handling
            - Fallback behavior

            ### 7. Testing Strategy

            - Unit test approach
            - Integration test scenarios
            - Test data requirements
            - Transactions for testing

            ### 8. Dependencies
            - Custom tables used
            - External calls
            - Authorization checks
            - Configuration dependencies

            ### 9. Risks & Considerations
            - Upgrade impact
            - Performance implications
            - Maintenance considerations

            ## Output Format

            **Summary**: What this enhancement does

            **Business Purpose**: Why it was implemented

            **Technical Details**:
            | Aspect | Value |
            |--------|-------|
            | Enhancement Spot | X |
            | Trigger Point | Y |
            | Execution Time | Before/After |

            **Logic Breakdown**: Step-by-step explanation

            **Testing Checklist**: Key scenarios to test

            **Maintenance Notes**: Important considerations
            """.formatted(enhancementName, enhancementType, source, defSection, enhancementType);

        return new GetPromptResult(
                "Enhancement Explanation: " + enhancementName,
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }

    // ========================================================================
    // 13. EVALUATE_CODE_QUALITY_BATCH - Batch Code Quality Evaluation
    // ========================================================================

    /**
     * Evaluates code quality for multiple objects.
     *
     * Used for:
     * - Pre-extraction quality assessment
     * - Package-wide quality audit
     * - Training data filtering
     *
     * @param objectList JSON array of objects with source code
     * @param qualityCriteria Optional custom quality criteria
     * @return Structured batch quality evaluation prompt
     */
    @McpPrompt(
            name = "evaluate_code_quality_batch",
            title = "Batch Code Quality Evaluation",
            description = "Evaluates code quality for multiple ABAP objects, providing quality scores and recommendations for extraction filtering."
    )
    public GetPromptResult evaluateCodeQualityBatch(
            @McpArg(name = "objectList", description = "JSON array of objects with name, type, and source", required = true) String objectList,
            @McpArg(name = "qualityCriteria", description = "Optional custom quality criteria JSON", required = false) String qualityCriteria
    ) {
        log.info("Prompt request: evaluate_code_quality_batch");

        String criteriaSection = (qualityCriteria != null && !qualityCriteria.isBlank())
                ? "\n\n## Custom Quality Criteria\n```json\n" + qualityCriteria + "\n```"
                : "";

        String promptText = """
            # Batch Code Quality Evaluation Request

            You are an SAP code quality analyst evaluating multiple ABAP objects.

            ## Objects to Evaluate
            ```json
            %s
            ```
            %s

            ## Evaluation Requirements

            Evaluate each object using these quality dimensions:

            ### 1. Quality Dimensions (Score 1-10)

            | Dimension | Weight | Criteria |
            |-----------|--------|----------|
            | Readability | 20%% | Clear naming, formatting, comments |
            | Maintainability | 20%% | Method size, complexity, modularity |
            | Performance | 15%% | Efficient DB access, loop optimization |
            | Security | 15%% | Input validation, auth checks |
            | Standards | 15%% | Naming conventions, ABAP guidelines |
            | Documentation | 15%% | Comments, method docs, README |

            ### 2. Evaluation Criteria

            **Excellent (9-10)**: Production-ready, exemplary code
            **Good (7-8)**: Minor improvements possible
            **Acceptable (5-6)**: Functional but needs improvement
            **Poor (3-4)**: Significant issues, needs refactoring
            **Unacceptable (1-2)**: Should not be used/extracted

            ### 3. Quality Flags

            Flag objects with:
            - 🔴 CRITICAL: Security vulnerabilities, data corruption risk
            - 🟡 WARNING: Performance issues, missing error handling
            - 🟢 INFO: Style improvements, documentation gaps

            ### 4. Training Data Suitability

            For each object, assess suitability for LLM training:
            - **INCLUDE**: High-quality, good patterns to learn
            - **EXCLUDE**: Bad practices, would teach wrong patterns
            - **CONDITIONAL**: Include with warnings/annotations

            ## Output Format

            **Evaluation Summary**:
            - Objects evaluated: X
            - Average quality score: Y/10
            - Recommended for extraction: Z

            **Detailed Results**:
            | Object | Type | Score | Flags | Training? |
            |--------|------|-------|-------|-----------|

            **Quality Distribution**:
            - Excellent (9-10): X objects
            - Good (7-8): Y objects
            - Acceptable (5-6): Z objects
            - Poor/Unacceptable (<5): W objects

            **Top Issues Found**:
            1. [Most common issue]
            2. [Second issue]
            3. [Third issue]

            **Recommendations**:
            - Objects to prioritize for extraction
            - Objects needing improvement before extraction
            - Objects to exclude from extraction

            **Batch Quality Score**: X/100
            """.formatted(objectList, criteriaSection);

        return new GetPromptResult(
                "Batch Quality Evaluation",
                List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
        );
    }
}
