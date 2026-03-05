package com.intuit.isl.test

import com.intuit.isl.test.annotations.SetupAnnotation
import com.intuit.isl.test.annotations.TestAnnotation
import com.intuit.isl.test.annotations.TestResultContext
import com.intuit.isl.test.assertions.AssertFunction
import com.intuit.isl.common.*
import com.intuit.isl.test.mocks.MockFunction
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.JsonConvert
import java.nio.file.Path

/** Sentinel used to register a fallback handler for any function call that is not mocked. */
private const val FALLBACK_METHOD_NAME = "*"

class TestOperationContext : BaseOperationContext {
    /** Current ISL file path (module name, e.g. "tests/sample.isl") for resolving relative paths in @.Load.From() */
    var currentFile: String? = null
        internal set

    /** Base path for resolving relative file paths (e.g. project root) */
    var basePath: Path? = null
        internal set

    /** Mock file name (e.g. from setup.mockSource) for error messages when an unmocked function is called. */
    var mockFileName: String? = null
        internal set

    companion object {
        fun create(
            testResultContext: TestResultContext,
            currentFile: String? = null,
            basePath: Path? = null,
            mockFileName: String? = null,
            contextCustomizers: List<(IOperationContext) -> Unit> = emptyList()
        ): TestOperationContext {
            val context = TestOperationContext()

            context.registerAnnotation(SetupAnnotation.annotationName, SetupAnnotation::runAnnotationFunction)
            TestAnnotation.registerAnnotation(context, testResultContext)

            AssertFunction.registerExtensions(context)
            LoadFunction.registerExtensions(context)
            MockFunction.registerExtensions(context)

            context.registerExtensionMethod(FALLBACK_METHOD_NAME) { functionContext ->
                throw buildUnmockedCallException(functionContext)
            }

            contextCustomizers.forEach { it(context) }

            context.currentFile = currentFile
            context.basePath = basePath
            context.mockFileName = mockFileName

            return context
        }

        private fun buildUnmockedCallException(context: FunctionExecuteContext): TransformException {
            val functionName = context.functionName
            val position = context.command.token.position
            val place = "file=${position.file}, line=${position.line}, column=${position.column}" +
                (position.endLine?.let { ", endLine=$it" } ?: "") +
                (position.endColumn?.let { ", endColumn=$it" } ?: "")

            val paramsJson = context.parameters
                .map { JsonConvert.convert(it) }
                .let { nodes -> JsonConvert.mapper.writeValueAsString(nodes) }

            val testContext = context.executionContext.operationContext as? TestOperationContext
            val mockFile = testContext?.mockFileName ?: "your-mocks.yaml"

            val yamlSnippet = buildString {
                appendLine("- name: \"$functionName\"")
                if (context.parameters.isNotEmpty()) {
                    appendLine("  params: $paramsJson")
                }
                appendLine("  result: <replace with expected return value>")
            }

            val message = buildString {
                appendLine("Unmocked function was called. The test must only call functions that are mocked.")
                appendLine("Function: @.$functionName")
                appendLine("Called from: $place")
                appendLine("Parameters: $paramsJson")
                appendLine("")
                appendLine("To mock this function add this to your [$mockFile] then rerun the tests:")
                appendLine("func:")
                append(yamlSnippet)
            }

            return TransformException(message.trimEnd(), position)
        }

        private fun buildUnmockedModifierException(modifierKey: String, context: FunctionExecuteContext): TransformException {
            val displayName = if (modifierKey.lowercase().startsWith("modifier.")) modifierKey.drop("modifier.".length) else modifierKey
            val position = context.command.token.position
            val place = "file=${position.file}, line=${position.line}, column=${position.column}" +
                (position.endLine?.let { ", endLine=$it" } ?: "") +
                (position.endColumn?.let { ", endColumn=$it" } ?: "")

            val testContext = context.executionContext.operationContext as? TestOperationContext
            val mockFile = testContext?.mockFileName ?: "your-mocks.yaml"

            val yamlName = "Modifier.$displayName"
            val yamlSnippet = buildString {
                appendLine("- name: \"$yamlName\"")
                appendLine("  result: <replace with expected return value>")
            }

            val message = buildString {
                appendLine("Unmocked modifier was called. The test must only call modifiers that are mocked.")
                appendLine("Modifier: | $displayName")
                appendLine("Called from: $place")
                appendLine("")
                appendLine("To mock this modifier add this to your [$mockFile] then rerun the tests:")
                appendLine("func:")
                append(yamlSnippet)
            }

            return TransformException(message.trimEnd(), position)
        }
    }

    constructor() : super() {
        this.mockExtensions = TestOperationMockExtensions()
    }

    private constructor(
        extensions: HashMap<String, AsyncContextAwareExtensionMethod>,
        annotations: HashMap<String, AsyncExtensionAnnotation>,
        statementExtensions: HashMap<String, AsyncStatementsExtensionMethod>,
        internalExtensions: HashMap<String, AsyncContextAwareExtensionMethod>,
        mockExtensions: TestOperationMockExtensions,
        mockFileName: String? = null
    ) : super(
        extensions, annotations, statementExtensions, internalExtensions, HashMap<String, ConditionalExtension>()
    ) {
        this.mockExtensions = mockExtensions
        this.mockFileName = mockFileName
    }

    val mockExtensions : TestOperationMockExtensions

    override fun getExtension(name: String): AsyncContextAwareExtensionMethod? {
        val function = mockExtensions.mockExtensions[name.lowercase()]?.func
        if (function != null) {
            return function
        }
        val fromSuper = super.getExtension(name)
        if (fromSuper != null) {
            return fromSuper
        }
        if (name.lowercase().startsWith("modifier.")) {
            return { context ->
                throw buildUnmockedModifierException(name, context)
            }
        }
        return null
    }

    override fun getAnnotation(annotationName: String): AsyncExtensionAnnotation? {
        val function = mockExtensions.mockAnnotations[annotationName.lowercase()]?.func
        if (function != null) {
            return function
        }
        return super.getAnnotation(annotationName)
    }

    override fun getStatementExtension(name: String): AsyncStatementsExtensionMethod? {
        val function = mockExtensions.mockStatementExtensions[name.lowercase()]?.func
        if (function != null) {
            return function
        }
        return super.getStatementExtension(name)
    }

    override fun clone(newInternals: HashMap<String, AsyncContextAwareExtensionMethod>): IOperationContext {
        return TestOperationContext(
            this.extensions, this.annotations, this.statementExtensions, newInternals, this.mockExtensions, this.mockFileName
        )
    }
}

