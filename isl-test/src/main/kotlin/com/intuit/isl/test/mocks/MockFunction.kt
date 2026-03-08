package com.intuit.isl.test.mocks

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.intuit.isl.runtime.FileInfo
import com.intuit.isl.runtime.TransformCompilationException
import com.intuit.isl.runtime.TransformPackageBuilder
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

        val relativePath = basePath.relativize(resolvedPath).toString().replace("\\", "/")
        applyMocksFromNode(context, root as ObjectNode, relativePath)
        return null
    }

    /**
     * Applies mocks from a parsed object (e.g. from YAML/JSON) to the test context.
     * Root must have "func" and/or "annotation" arrays in the same format as @.Mock.Load file.
     * Mocks are always added; params differentiate multiple mocks for the same function.
     * Clearing only happens when the next test starts (new TestOperationContext).
     *
     * @param islSourceFile Optional file path (e.g. mock file or test file) used as the module name when compiling inline ISL snippets, so compilation errors point to the correct file.
     */
    fun applyMocksFromNode(context: TestOperationContext, root: ObjectNode, islSourceFile: String? = null) {
        val funcMocks = root.get("func")
        val annotationMocks = root.get("annotation")

        if (funcMocks != null && funcMocks.isArray) {
            funcMocks.forEach { entry ->
                if (entry.isObject) {
                    registerMockFromNode(context, entry as ObjectNode, ::createFuncMock, funcRegex, islSourceFile)
                }
            }
        }
        if (annotationMocks != null && annotationMocks.isArray) {
            annotationMocks.forEach { entry ->
                if (entry.isObject) {
                    registerMockFromNode(context, entry as ObjectNode, ::createAnnotationMock, annotationRegex, islSourceFile)
                }
            }
        }
    }

    private fun resolvePath(basePath: Path, currentFile: String, fileName: String): Path {
        val currentDir = basePath.resolve(currentFile).parent ?: basePath
        return currentDir.resolve(fileName).normalize()
    }

    private fun registerMockFromNode(
        context: TestOperationContext,
        node: ObjectNode,
        registerMock: (TestOperationContext, String, Any?, Map<Int, JsonNode>) -> Int?,
        nameRegex: String,
        islSourceFile: String? = null
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

        val key = name.lowercase()

        val islNode = node.get("isl")
        val islContent = if (islNode != null && !islNode.isNull) ConvertUtils.tryToString(islNode)?.trim() else null

        val sourceFile = islSourceFile ?: context.mockFileName ?: context.testFileName ?: context.currentFile
        val returnNode = if (islContent != null) null else (node.get("result") ?: node.get("return"))
        val returnValue: Any? = when {
            islContent != null -> compileIslSnippetToExecutor(islContent, name, sourceFile)
            returnNode == null || returnNode.isNull -> null
            else -> returnNode
        }

        val params = mutableMapOf<Int, JsonNode>()
        val paramsNode = node.get("params")
        if (paramsNode != null) {
            if (paramsNode.isArray) {
                (paramsNode as ArrayNode).forEachIndexed { i, param ->
                    params[i] = param
                }
            } else {
                // Single value (e.g. params: "start_date") -> treat as single parameter at index 0
                params[0] = paramsNode
            }
        }

        registerMock(context, key, returnValue, params)
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
        val key = funcName.lowercase()
        return context.mockExtensions.mockStatementExtensions.getOrPut(key) {
            MockContext { mockObj ->
                { mockContext, statementExecution ->
                    val name = mockContext.functionName
                    val paramsShort = shortParams(mockContext.parameters)
                    println("[ISL Mock] Calling mocked statement function $name($paramsShort)")
                    // Capture the argument inputs
                    tryFindMatch(mockObj, mockContext)
                    // Run the statement
                    statementExecution(mockContext.executionContext)
                    // Return null
                    println("[ISL Mock] Returned mocked statement function $name=${shortValue(null)}")
                    null
                }
            }
        }
    }

    private fun createAnnotationContext(
        context: TestOperationContext,
        funcName: String
    ): MockContext<AsyncExtensionAnnotation> {
        val key = funcName.lowercase()
        return context.mockExtensions.mockAnnotations.getOrPut(key) {
            MockContext { mockObj ->
                { mockContext ->
                    val name = mockContext.annotationName
                    val paramsShort = shortParams(mockContext.parameters)
                    println("[ISL Mock] Calling mocked modifier $name($paramsShort)")
                    val r = tryFindMatch(mockObj, mockContext)
                    val result = when {
                        r != null && r !is IslMockExecutor -> r
                        else -> mockContext.runNextCommand()
                    }
                    println("[ISL Mock] Returned mocked modifier $name=${shortValue(result)}")
                    result
                }
            }
        }
    }

    private fun createFunctionContext(
        context: TestOperationContext,
        funcName: String
    ): MockContext<AsyncContextAwareExtensionMethod> {
        val key = funcName.lowercase()
        return context.mockExtensions.mockExtensions.getOrPut(key) {
            MockContext { mockObj ->
                { mockContext ->
                    val name = mockContext.functionName
                    val paramsShort = shortParams(mockContext.parameters)
                    val r = tryFindMatch(mockObj, mockContext)
                    //println("[ISL Mock] Calling mocked function $name($paramsShort) Match=$r ")
                    val result = if (r is IslMockExecutor) r.run(mockContext) else r
                    //println("[ISL Mock] Returned mocked function $name=${shortValue(result)}")
                    result
                }
            }
        }
    }

    /**
     * Compiles an ISL snippet (e.g. a single function) and returns an executor that runs it in context.
     * The snippet must define a function called "run": "fun run( ...)"
     * When the mock is invoked, that function is run with the call's parameters bound to its arguments.
     *
     * @param islContent ISL source (e.g. "fun mask(\$value) { return `xxxxxx\$value`; }")
     * @param mockName Mock name (for error messages)
     * @param sourceFile Optional file path (e.g. mock YAML or test file) used as the module name so compilation errors show the correct file instead of __mock_isl__
     * @throws TransformCompilationException if compilation fails
     */
    private fun compileIslSnippetToExecutor(islContent: String, mockName: String, sourceFile: String? = null): IslMockExecutor {
        if (islContent.isBlank()) {
            throw IllegalArgumentException("Mock '$mockName': 'isl' content must be non-empty")
        }
        val moduleName = sourceFile?.takeIf { it.isNotBlank() } ?: "__mock_isl__"
        val pkg = try {
            TransformPackageBuilder().build(mutableListOf(FileInfo(moduleName, islContent)), null)
        } catch (e: Exception) {
            val msg = (e as? TransformCompilationException)?.message ?: e.toString()
            throw TransformCompilationException("Mock '$mockName': ISL compilation failed. $msg", (e as? TransformCompilationException)?.position)
        }
        val transformer = pkg.getModule(moduleName)
            ?: throw TransformCompilationException("Mock '$mockName': compiled module not found", null)
        val module = transformer.module
        val firstFunc = module.getFunction("run")
            ?: throw TransformCompilationException("Mock '$mockName': ISL snippet must define a function 'fun run(...){ ... }')", null)
        val runner = module.getFunctionRunner(firstFunc.name)
            ?: throw TransformCompilationException("Mock '$mockName': could not get runner for function ${firstFunc.name}", null)
        return IslMockExecutor(runner)
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
        val (name, position) = when (executeContext) {
            is FunctionExecuteContext -> executeContext.functionName to executeContext.command.token.position
            is AnnotationExecuteContext -> executeContext.annotationName to executeContext.command.token.position
            else -> null to null
        }
        val testContext = when (executeContext) {
            is FunctionExecuteContext -> executeContext.executionContext.operationContext as? TestOperationContext
            is AnnotationExecuteContext -> executeContext.executionContext.operationContext as? TestOperationContext
            else -> null
        }
        val mockFileName = testContext?.mockFileName
        val testFileName = testContext?.testFileName
        return mockObject.tryFindMatch(inputParams, true, name, position, mockFileName, testFileName)
    }

    private const val SHORT_MAX_LEN = 80

    private fun shortValue(value: Any?, maxLen: Int = SHORT_MAX_LEN): String {
        val s = when (value) {
            null -> "null"
            else -> try {
                JsonConvert.mapper.writeValueAsString(JsonConvert.convert(value))
            } catch (_: Exception) {
                value.toString()
            }
        }
        return if (s.length <= maxLen) s else s.take(maxLen - 3) + "..."
    }

    private fun shortParams(parameters: Array<*>): String {
        val s = parameters.mapIndexed { i, p -> shortValue(p, 40) }.joinToString(", ")
        return if (s.length <= SHORT_MAX_LEN) s else s.take(SHORT_MAX_LEN - 3) + "..."
    }
}


