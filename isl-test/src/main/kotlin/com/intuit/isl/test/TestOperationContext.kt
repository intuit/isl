package com.intuit.isl.test

import com.intuit.isl.test.annotations.SetupAnnotation
import com.intuit.isl.test.annotations.TestAnnotation
import com.intuit.isl.test.annotations.TestResultContext
import com.intuit.isl.test.assertions.AssertFunction
import com.intuit.isl.common.*
import com.intuit.isl.test.mocks.MockFunction
import java.nio.file.Path

class TestOperationContext : BaseOperationContext {
    /** Current ISL file path (module name, e.g. "tests/sample.isl") for resolving relative paths in @.Load.From() */
    var currentFile: String? = null
        internal set

    /** Base path for resolving relative file paths (e.g. project root) */
    var basePath: Path? = null
        internal set

    companion object {
        fun create(
            testResultContext: TestResultContext,
            currentFile: String? = null,
            basePath: Path? = null,
            contextCustomizers: List<(IOperationContext) -> Unit> = emptyList()
        ): TestOperationContext {
            val context = TestOperationContext()

            context.registerAnnotation(SetupAnnotation.annotationName, SetupAnnotation::runAnnotationFunction)
            TestAnnotation.registerAnnotation(context, testResultContext)

            AssertFunction.registerExtensions(context)
            LoadFunction.registerExtensions(context)
            MockFunction.registerExtensions(context)

            contextCustomizers.forEach { it(context) }

            context.currentFile = currentFile
            context.basePath = basePath

            return context
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
        mockExtensions: TestOperationMockExtensions
    ) : super(
        extensions, annotations, statementExtensions, internalExtensions, HashMap<String, ConditionalExtension>()
    ) {
        this.mockExtensions = mockExtensions
    }

    val mockExtensions : TestOperationMockExtensions

    override fun getExtension(name: String): AsyncContextAwareExtensionMethod? {
        val function = mockExtensions.mockExtensions[name.lowercase()]?.func
        if (function != null) {
            return function
        }
        return super.getExtension(name)
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
            this.extensions, this.annotations, this.statementExtensions, newInternals, this.mockExtensions
        )
    }
}

