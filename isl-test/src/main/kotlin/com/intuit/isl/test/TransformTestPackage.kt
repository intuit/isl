package com.intuit.isl.test

import com.intuit.isl.common.IOperationContext
import com.intuit.isl.test.annotations.SetupAnnotation
import com.intuit.isl.test.annotations.TestAnnotation
import com.intuit.isl.test.annotations.TestResultContext
import com.intuit.isl.commands.IFunctionDeclarationCommand
import com.intuit.isl.runtime.TransformModule
import com.intuit.isl.runtime.TransformPackage
import java.nio.file.Path

class TransformTestPackage(
    private val transformPackage: TransformPackage,
    private val basePath: Path? = null,
    private val contextCustomizers: List<(IOperationContext) -> Unit> = emptyList()
) {
    private val testFiles = mutableMapOf<String, TransformTestFile>()

    init {
        transformPackage.modules.forEach { file ->
            val module = transformPackage.getModule(file)?.module
            module?.functions?.forEach { function ->
                val testFile = verifyIfModuleIsTestFile(function, module, file)
                if (testFile != null) {
                    val existing = testFiles[file]
                    testFiles[file] = if (existing != null) {
                        TransformTestFile(
                            file,
                            existing.testFunctions + testFile.testFunctions,
                            existing.setupFile ?: testFile.setupFile
                        )
                    } else {
                        testFile
                    }
                }
            }
        }
    }

    fun runAllTests(testResultContext: TestResultContext? = null) : TestResultContext {
        val context = testResultContext ?: TestResultContext()
        testFiles.forEach { (_, file) ->
            file.testFunctions.forEach { function ->
                runTest(file.fileName, function, context)
            }
        }

        return context
    }

    fun runTest(testFile: String, testFunc: String, testResultContext: TestResultContext? = null) : TestResultContext {
        var context = testResultContext ?: TestResultContext()
        runTest(testFile, testFunc, context, testFiles[testFile]?.setupFile)
        return context
    }

    private fun runTest(testFile: String, testFunc: String, testResultContext: TestResultContext, setupFunc: String? = null) {
        val fullFunctionName = TransformPackage.toFullFunctionName(testFile, testFunc)
        val context = TestOperationContext.create(testResultContext, testFile, basePath, contextCustomizers)
        // Run setup function if it exists
        if (setupFunc != null) {
            val fullSetupFunctionName = TransformPackage.toFullFunctionName(testFile, setupFunc)
            transformPackage.runTransform(fullSetupFunctionName, context)
        }
        transformPackage.runTransform(fullFunctionName, context)
    }

    private fun verifyIfModuleIsTestFile(
        function: IFunctionDeclarationCommand,
        module: TransformModule,
        file: String
    ): TransformTestFile? {
        val testFunctions = mutableSetOf<String>()
        var setUpFunction: String? = null
        function.token.annotations.forEach { a ->
            when (a.annotationName) {
                TestAnnotation.annotationName -> {
                    testFunctions.add(function.token.functionName)
                }

                SetupAnnotation.annotationName -> {
                    if (setUpFunction != null) {
                        throw Exception("Multiple setUp functions found. File: ${module.name}, Function: ${function.token.functionName}")
                    }
                    setUpFunction = function.token.functionName
                }
            }
        }

        // Mark file as test file if it has any test functions
        if (testFunctions.isNotEmpty()) {
            return TransformTestFile(file, testFunctions, setUpFunction)
        }
        return null
    }
}

