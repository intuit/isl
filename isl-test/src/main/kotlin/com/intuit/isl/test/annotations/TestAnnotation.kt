package com.intuit.isl.test.annotations;

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.test.assertions.AssertException
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.runtime.IslException
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.ConvertUtils

object TestAnnotation {
    const val annotationName = "test"

    fun registerAnnotation(operationContext: IOperationContext, testResultContext: TestResultContext) {
        operationContext.registerAnnotation(annotationName)  { context ->
            var contextCommandOutput : Any? = null
            val (testName, testGroup) = parseTestAnnotationParams(context)
            val result = TestResult(
                testFile = context.command.token.position.file,
                functionName = context.functionName,
                testName = testName,
                testGroup = testGroup,
                success = true
            )
            try {
                contextCommandOutput = context.runNextCommand()
            }
            catch (e: Exception) {
                result.success = false
                result.message = e.message
                result.exception = e
                if (e is TransformException && e.cause != null) {
                    e.cause.let {
                        if (it is AssertException) {
                            // Surface the assertion exception message
                            result.message = it.message
                            result.errorPosition = it.position
                            result.exception = it
                        }
                    }
                }
                if (e is IslException) {
                    result.errorPosition = e.position
                }

            }
            finally {
                testResultContext.testResults.add(result)
            }

            contextCommandOutput
        }
    }

    /**
     * Parse @test annotation parameters.
     * Supports: @test(), @test(name), @test(name, group), @test({ name: "x", group: "y" })
     */
    private fun parseTestAnnotationParams(context: com.intuit.isl.common.AnnotationExecuteContext): Pair<String, String?> {
        val functionName = context.functionName
        val testFile = context.command.token.position.file
        val defaultGroup = testFile.substringAfterLast('/').substringAfterLast('\\')
        val params = context.parameters
        return when {
            params.isEmpty() -> Pair(functionName, defaultGroup)
            params.size == 1 -> {
                val first = params[0]
                when {
                    first is JsonNode && first.isObject -> {
                        val name = first.path("name").takeIf { !it.isMissingNode }?.asText() ?: functionName
                        val group = first.path("group").takeIf { !it.isMissingNode }?.asText()
                        Pair(name, group)
                    }
                    else -> Pair(ConvertUtils.tryToString(first) ?: functionName, defaultGroup)
                }
            }
            params.size >= 2 -> {
                val name = ConvertUtils.tryToString(params[0]) ?: functionName
                val group = ConvertUtils.tryToString(params[1])
                Pair(name, group)
            }
            else -> Pair(functionName, defaultGroup)
        }
    }
}
