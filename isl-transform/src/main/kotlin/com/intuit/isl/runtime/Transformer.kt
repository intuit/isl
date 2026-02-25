package com.intuit.isl.runtime

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.common.*
import com.intuit.isl.parser.tokens.IIslToken
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.runBlocking
import java.util.jar.Manifest

class Transformer(override val module: TransformModule) : ITransformer {
    val token: IIslToken
        get() = module.token;

    val version: String
        get() = islInfo["version"].textValue();

    companion object {
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
        operationContext: IOperationContext
    ): ITransformResult {
        val function = module.getFunction(functionName);

        if (function != null) {
            val context = ExecutionContext(operationContext, null);

            // Add Default Variables
            init(context);

            // Optimize this - we should be able to plug the whole internals
            (operationContext as BaseOperationContext).useModuleFunctions(this.module.functionExtensions);

            val result = function.executeAsync(context);
            return TransformResult(JsonConvert.convert(result.value));
        }

        throw TransformException("Unknown Function @.${module.name}.$functionName", module.token.position);
    }

    override fun runTransformSync(
        functionName: String,
        operationContext: IOperationContext
    ): JsonNode? {
        val result = runBlocking {
            runTransformAsync(functionName, operationContext);
        }
        return result.result;
    }

    /**
     * Return a proxy to an internal function that can be called from an external module
     */
    fun crossModuleExecuteFunction(
        functionName: String
    ): AsyncContextAwareExtensionMethod? {
        val function = module.getFunctionRunner(functionName);
        return function;
    }
}