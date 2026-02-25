package com.intuit.isl.test.mocks

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.intuit.isl.test.TestOperationContext
import com.intuit.isl.common.*
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import java.nio.file.Path

object MockFunction {
    private const val funcRegex = "[A-Za-z_]+\\.[A-Za-z0-9_]+(#[0-9]+)?"
    private const val annotationRegex = "[A-Za-z0-9_]+(#[0-9]+)?"

    fun registerExtensions(context: TestOperationContext) {
        context.registerExtensionMethod("Mock.Func") {
            mockFunction(it, funcRegex) { funcName ->
                createFunctionContext(context, funcName)
            }
        }

        context.registerExtensionMethod("Mock.Annotation") {
            mockFunction(it, annotationRegex) { funcName ->
                createAnnotationContext(context, funcName)
            }
        }

        context.registerExtensionMethod("Mock.StatementFunc") {
            mockFunction(it, funcRegex) { funcName ->
                createStatementFuncContext(context, funcName)
            }
        }

        context.registerExtensionMethod("Mock.GetFuncCaptures") {
            getCaptures(it, funcRegex) { funcName ->
                context.mockExtensions.mockExtensions[funcName]
            }
        }

        context.registerExtensionMethod("Mock.GetAnnotationCaptures") {
            getCaptures(it, annotationRegex) { funcName ->
                context.mockExtensions.mockAnnotations[funcName]
            }
        }

        context.registerExtensionMethod("Mock.GetStatementFuncCaptures") {
            getCaptures(it, funcRegex) { funcName ->
                context.mockExtensions.mockStatementExtensions[funcName]
            }
        }

        context.registerExtensionMethod("Mock.Load") {
            loadMocksFromFile(it)
        }
    }

    private fun loadMocksFromFile(executeContext: FunctionExecuteContext): Any? {
        val context = executeContext.executionContext.operationContext as? TestOperationContext
            ?: throw IllegalStateException("@.Mock.Load is only available in test context")
        val fileName = ConvertUtils.tryToString(executeContext.firstParameter)
            ?: throw IllegalArgumentException("@.Mock.Load requires a file name (string)")

        val basePath = context.basePath
            ?: throw IllegalStateException("@.Mock.Load requires basePath; run tests via isl test command or pass basePath to TransformTestPackageBuilder")

        val currentFile = context.currentFile
            ?: throw IllegalStateException("@.Mock.Load requires currentFile; run tests via isl test command")

        val resolvedPath = resolvePath(basePath, currentFile, fileName)
        val file = resolvedPath.toFile()

        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $resolvedPath (resolved from $fileName relative to $currentFile)")
        }
        if (!file.isFile) {
            throw IllegalArgumentException("Not a file: $resolvedPath")
        }

        val ext = file.extension.lowercase()
        val root = when (ext) {
            "json" -> JsonConvert.mapper.readTree(file)
            "yaml", "yml" -> com.fasterxml.jackson.databind.ObjectMapper(YAMLFactory()).readTree(file)
            else -> throw IllegalArgumentException("@.Mock.Load supports .json, .yaml, .yml; got: $fileName")
        }

        if (!root.isObject) {
            throw IllegalArgumentException("Mock file must have a root object with 'func' and/or 'annotation' keys")
        }

        val obj = root as ObjectNode
        val funcMocks = obj.get("func")
        val annotationMocks = obj.get("annotation")

        if (funcMocks != null && funcMocks.isArray) {
            funcMocks.forEach { entry ->
                if (entry.isObject) {
                    registerMockFromNode(context, entry as ObjectNode, ::createFuncMock, funcRegex)
                }
            }
        }
        if (annotationMocks != null && annotationMocks.isArray) {
            annotationMocks.forEach { entry ->
                if (entry.isObject) {
                    registerMockFromNode(context, entry as ObjectNode, ::createAnnotationMock, annotationRegex)
                }
            }
        }

        return null
    }

    private fun resolvePath(basePath: Path, currentFile: String, fileName: String): Path {
        val currentDir = basePath.resolve(currentFile).parent ?: basePath
        return currentDir.resolve(fileName).normalize()
    }

    private fun registerMockFromNode(
        context: TestOperationContext,
        node: ObjectNode,
        registerMock: (TestOperationContext, String, Any?, Map<Int, JsonNode>) -> Int?,
        nameRegex: String
    ) {
        val nameNode = node.get("name") ?: throw IllegalArgumentException("Mock entry must have 'name' field")
        val name = ConvertUtils.tryToString(nameNode)?.trim()
            ?: throw IllegalArgumentException("Mock 'name' must be a non-empty string")
        if (name.isBlank()) {
            throw IllegalArgumentException("Mock 'name' must be a non-empty string")
        }
        if (!name.matches(Regex(nameRegex))) {
            throw IllegalArgumentException("Invalid mock name: $name")
        }

        val returnNode = node.get("return")
        val returnValue: Any? = when {
            returnNode == null || returnNode.isNull -> null
            else -> returnNode
        }

        val params = mutableMapOf<Int, JsonNode>()
        val paramsNode = node.get("params")
        if (paramsNode != null && paramsNode.isArray) {
            (paramsNode as ArrayNode).forEachIndexed { i, param ->
                params[i] = param
            }
        }

        registerMock(context, name.lowercase(), returnValue, params)
    }

    /***
     * Create a function mock.
     * @param context The test operation context.
     * @param functionNameStr The function name string.
     * @param returnValue The return value.
     * @param parameters The parameters.
     * @return The function id.
     */
    fun createFuncMock(
        context: TestOperationContext, functionNameStr: String, returnValue: Any?, parameters: Map<Int, JsonNode>
    ): Int? {
        return mockFunction(functionNameStr, returnValue, parameters) {
            createFunctionContext(context, it)
        }
    }

    /***
     * Create an annotation mock.
     * @param context The test operation context.
     * @param functionNameStr The function name string.
     * @param returnValue The return value.
     * @param parameters The parameters.
     * @return The function id.
     */
    fun createAnnotationMock(
        context: TestOperationContext, functionNameStr: String, returnValue: Any?, parameters: Map<Int, JsonNode>
    ): Int? {
        return mockFunction(functionNameStr, returnValue, parameters) {
            createAnnotationContext(context, it)
        }
    }

    /***
     * Create a statement function mock.
     * @param context The test operation context.
     * @param functionNameStr The function name string.
     * @param returnValue The return value.
     * @param parameters The parameters.
     * @return The function id.
     */
    fun createStatementFuncMock(
        context: TestOperationContext, functionNameStr: String, returnValue: Any?, parameters: Map<Int, JsonNode>
    ): Int? {
        return mockFunction(functionNameStr, returnValue, parameters) {
            createStatementFuncContext(context, it)
        }
    }

    private fun createStatementFuncContext(
        context: TestOperationContext,
        funcName: String
    ): MockContext<AsyncStatementsExtensionMethod> {
        return context.mockExtensions.mockStatementExtensions.getOrPut(funcName) {
            MockContext { mockObj ->
                { mockContext, statementExecution ->
                    // Capture the argument inputs
                    tryFindMatch(mockObj, mockContext)
                    // Run the statement
                    statementExecution(mockContext.executionContext)
                    // Return null
                    null
                }
            }
        }
    }

    private fun createAnnotationContext(
        context: TestOperationContext,
        funcName: String
    ): MockContext<AsyncExtensionAnnotation> {
        return context.mockExtensions.mockAnnotations.getOrPut(funcName) {
            MockContext { mockObj ->
                { mockContext ->
                    // Capture the argument inputs
                    tryFindMatch(mockObj, mockContext)
                    // Run and return the underlying function value
                    mockContext.runNextCommand()
                }
            }
        }
    }

    private fun createFunctionContext(
        context: TestOperationContext,
        funcName: String
    ): MockContext<AsyncContextAwareExtensionMethod> {
        return context.mockExtensions.mockExtensions.getOrPut(funcName) {
            MockContext { mockObj ->
                { mockContext ->
                    tryFindMatch(mockObj, mockContext)
                }
            }
        }
    }

    private fun <T> mockFunction(
        context: FunctionExecuteContext,
        funcValidationRegex: String,
        getMockContext: (funcNameStr: String) -> MockContext<T>
    ): Any? {
        val (functionNameStr, returnValue, parameters) = parseMockFunctionExecuteContext(context, funcValidationRegex)

        return mockFunction(functionNameStr, returnValue, parameters, getMockContext)
    }

    private fun <T> mockFunction(
        functionNameStr: String,
        returnValue: Any?,
        parameters: Map<Int, JsonNode>,
        getMockContext: (funcNameStr: String) -> MockContext<T>
    ): Int? {
        val (baseName, index) = parseFunctionNameWithIndex(functionNameStr)
        val obtainedContext = getMockContext(baseName)

        return obtainedContext.mockObject.addMock(returnValue, parameters, index)
    }

    /**
     * Parses function name for optional #index suffix.
     * e.g. "data.getdata#1" -> ("data.getdata", 1), "data.getdata" -> ("data.getdata", null)
     */
    private fun parseFunctionNameWithIndex(functionNameStr: String): Pair<String, Int?> {
        val hashIndex = functionNameStr.lastIndexOf('#')
        if (hashIndex >= 0) {
            val baseName = functionNameStr.substring(0, hashIndex)
            val indexStr = functionNameStr.substring(hashIndex + 1)
            val index = indexStr.toIntOrNull()
            if (index != null && index >= 1) {
                return baseName to index
            }
        }
        return functionNameStr to null
    }

    private fun parseMockFunctionExecuteContext(
        context: FunctionExecuteContext, funcValidationRegex: String
    ): Triple<String, Any?, MutableMap<Int, JsonNode>> {
        val functionNameStr = getAndValidateFunctionName(context, funcValidationRegex)
        val returnValue = context.secondParameter
        val parameters = mutableMapOf<Int, JsonNode>()
        if (context.parameters.size > 2) {
            parameters.putAll(context.parameters.slice(2 until context.parameters.size).mapIndexed { i, it ->
                i to JsonConvert.convert(it)
            }.toMap())
        }
        return Triple(functionNameStr, returnValue, parameters)
    }

    private fun <T> getCaptures(
        context: FunctionExecuteContext, regex: String, getMockContext: (funcNameStr: String) -> MockContext<T>?
    ): Any? {
        val functionNameStr = getAndValidateFunctionName(context, regex)
        val (baseName, _) = parseFunctionNameWithIndex(functionNameStr)
        val instanceId = context.secondParameter

        val obtainedContext =
            getMockContext(baseName) ?: throw Exception("Mock function $baseName is not registered.")

        return obtainedContext.mockObject.getCaptures(ConvertUtils.tryParseInt(instanceId))
    }

    private fun getAndValidateFunctionName(context: FunctionExecuteContext, regex: String): String {
        val functionName = context.firstParameter
        // Get the function name in lower case
        val functionNameStr = ConvertUtils.tryToString(functionName)?.lowercase()
        if (functionNameStr.isNullOrBlank()) {
            throw Exception("Function name to mock is not provided.")
        }
        if (!validateFunctionName(functionNameStr, regex)) {
            throw Exception("Valid function name to mock must provided. Invalid function name: $functionNameStr")
        }
        return functionNameStr
    }

    private fun validateFunctionName(functionName: String, regex: String): Boolean {
        return functionName.matches(Regex(regex))
    }


    private fun tryFindMatch(mockObject: MockObject, executeContext: IExecuteContext): Any? {
        val inputParams = executeContext.parameters.mapIndexed { i, it ->
            i to JsonConvert.convert(it)
        }.toMap()
        return mockObject.tryFindMatch(inputParams)
    }
}


