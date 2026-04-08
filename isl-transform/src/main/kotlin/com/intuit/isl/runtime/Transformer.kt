package com.intuit.isl.runtime

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.IFunctionDeclarationCommand
import com.intuit.isl.common.*
import com.intuit.isl.commands.CoverageStatementIdAssigner
import com.intuit.isl.debug.IExecutionHook
import com.intuit.isl.parser.tokens.IIslToken
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.jar.Manifest
import kotlin.coroutines.coroutineContext

class Transformer(override val module: TransformModule) : ITransformer {
    val token: IIslToken
        get() = module.token;

    val version: String
        get() = islInfo["version"].textValue();

    companion object {
        // Virtual thread dispatcher for the internal ISL engine
        private val vtDispatcher = Executors.newVirtualThreadPerTaskExecutor()
            .asCoroutineDispatcher()
        
        private val islInfo: ObjectNode = initIslInfo();

        private fun initIslInfo(): ObjectNode {
            val info = JsonNodeFactory.instance.objectNode();
            info.put("version", "Unknown");
            info.put("jacksonVersion", "Unknown");
            info.put("maxParallelWorkers", 1);
            try {
                var version = detectIslVersion();
                var jacksonVersion = detectJacksonVersion();
                if (jacksonVersion == version) {
                    jacksonVersion = "Unknown";
                }
                info.put("version", version);
                info.put("jacksonVersion", jacksonVersion);
                if (jacksonVersion == "Unknown") {
                    println("Loaded ISL Version $version");
                } else {
                    println("Loaded ISL Version $version Jackson $jacksonVersion");
                }
            } catch (e: Exception) {
                println("Could not detect ISL Version ${e.message}");
            }
            return info;
        }

        private fun detectIslVersion(): String {
            Transformer::class.java.`package`?.let { pkg ->
                (pkg.implementationVersion ?: pkg.specificationVersion)?.let { return it }
            }
            readManifestMainAttribute("Implementation-Version")?.let { return it }
            readManifestMainAttribute("Specification-Version")?.let { return it }
            return "Unknown"
        }

        private fun detectJacksonVersion(): String {
            JsonNode::class.java.`package`?.let { pkg ->
                (pkg.specificationVersion ?: pkg.implementationVersion)?.let { return it }
            }
            readJacksonVersionFromPomProperties()?.let { return it }
            return "Unknown"
        }

        private fun readManifestMainAttribute(name: String): String? {
            return try {
                Transformer::class.java.getResourceAsStream("/META-INF/MANIFEST.MF")?.use { stream ->
                    Manifest(stream).mainAttributes.getValue(name)?.takeIf { it.isNotBlank() }
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun readJacksonVersionFromPomProperties(): String? {
            return try {
                JsonNode::class.java.getResourceAsStream(
                    "/META-INF/maven/com.fasterxml.jackson.core/jackson-databind/pom.properties"
                )?.use { stream ->
                    java.util.Properties().apply { load(stream) }.getProperty("version")?.takeIf { it.isNotBlank() }
                }
            } catch (e: Exception) {
                null
            }
        }

        val version: String
            get() = islInfo["version"].textValue();

        fun getIslInfo(): JsonNode {
            return islInfo.deepCopy();
        }

        internal fun init(context: ExecutionContext) {
            if (context.operationContext.getVariable("\$isl") == null) {
                val version = TransformVariable(getIslInfo(), true, true);
                context.operationContext.setVariable("\$isl", version);
            }
        }

        var maxParallelWorkers: Int
            get() = islInfo["maxParallelWorkers"].intValue();
            set(value) {
                val realValue = Math.clamp(value.toLong(), 1, 50);
                islInfo.put("maxParallelWorkers", realValue)
            };
    }

    /**
     * Run a specific function in a pre-compiled transformation.
     * You can pass through the @param operationContext
     * and a unique @param carryContext that will be carried across the executions
     * Using a carryContext allows you to pre-register all methods in the operationContext and use
     * the carryContext
     */
    override suspend fun runTransformAsync(
        functionName: String,
        operationContext: IOperationContext,
        executionHook: IExecutionHook?
    ): ITransformResult {
        // Capture the original coroutine context to preserve cancellation, logging context, etc.
        val originalContext = coroutineContext
        
        // Bridge: coroutine world → virtual thread world
        return withContext(vtDispatcher) {
            executeInternal(functionName, operationContext, executionHook, originalContext)
        }
    }

    /**
     * Entry functions run via [IFunctionDeclarationCommand.executeAsync] (not [IFunctionDeclarationCommand.getRunner]),
     * so formal parameters are not populated by the call path. The body still resolves `$param` through the
     * operation context. Hosts usually pre-seed JSON keys as variables; this copies legacy keys stored without
     * `$` (e.g. `status` → `$status`) so they match the names used in the function body.
     */
    private fun bindEntryPointParametersFromContext(
        function: IFunctionDeclarationCommand,
        operationContext: IOperationContext
    ) {
        for (p in function.token.arguments) {
            val dollarName = p.name.let { n -> if (n.startsWith("$")) n else "$" + n }
            if (operationContext.getVariable(dollarName) != null) continue
            val bare = dollarName.removePrefix("$").lowercase()
            if (bare.isEmpty()) continue
            val legacy = operationContext.getVariable(bare) ?: continue
            operationContext.setVariable(
                dollarName,
                TransformVariable(legacy, readOnly = false, global = true)
            )
        }
    }

    /**
     * Internal execution method - runs on virtual threads, no suspend.
     * This is the real engine that executes ISL transformations.
     */
    private fun executeInternal(
        functionName: String,
        operationContext: IOperationContext,
        executionHook: IExecutionHook?,
        capturedContext: kotlin.coroutines.CoroutineContext
    ): ITransformResult {
        val function = module.getFunction(functionName)
            ?: throw TransformException("Unknown Function @.${module.name}.$functionName", module.token.position)

        if (executionHook?.preparesStatementIds == true) {
            CoverageStatementIdAssigner.assign(module)
        }
        
        // Store the captured context in ExecutionContext for use in bridges
        val context = ExecutionContext(operationContext, null, executionHook, capturedContext)

        // Add Default Variables
        init(context)

        // Optimize this - we should be able to plug the whole internals
        (operationContext as BaseOperationContext).useModuleFunctions(this.module.functionExtensions)

        bindEntryPointParametersFromContext(function, operationContext)

        context.executionHook?.onFunctionEnter(function, context)
        try {
            val result = function.execute(context)
            return TransformResult(JsonConvert.convert(result.value))
        } finally {
            context.executionHook?.onFunctionExit(function, context)
        }
    }

    override fun runTransformSync(
        functionName: String,
        operationContext: IOperationContext
    ): JsonNode? {
        // Already blocking — run directly on a virtual thread
        // Note: If caller is already on a VT, this just runs inline
        val result = executeInternal(functionName, operationContext, null, kotlin.coroutines.EmptyCoroutineContext)
        return result.result
    }

    /**
     * Return a proxy to an internal function that can be called from an external module
     * All internal functions are sync
     */
    fun crossModuleExecuteFunction(
        functionName: String
    ): ContextAwareExtensionMethod? {
        return module.getFunctionRunner(functionName);
    }
}