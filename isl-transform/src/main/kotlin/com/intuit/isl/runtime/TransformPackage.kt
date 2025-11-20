package com.intuit.isl.runtime

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.utils.Position
import kotlinx.coroutines.runBlocking
import java.util.*


/**
 * A Transform Package is a set of Transform Modules (files) working together.
 * This is basically a complete package of files bundled together. This might not necessarily be all transform files.
 * For example, we can load (WIP) .yml enum mapping files and convert them to convert functions @.Modifier.NameOfConversion()
 * and register them as "| nameOfConversion"
 */
class TransformPackage(modules: TreeMap<String, ITransformer>) {
    companion object {
        fun toFullFunctionName(moduleName: String, functionName: String): String {
            return "$moduleName:$functionName";
        }
    }
    private val _modules = TreeMap<String, Transformer>(String.CASE_INSENSITIVE_ORDER);

    init {
        modules.forEach {
            this._modules[it.key] = it.value as Transformer;
        }
    }

    val modules get() = _modules.keys.toList();

    fun getModule(moduleName: String): Transformer? {
        return _modules[moduleName];
    }

    // DO NOT USE THIS FUNCTION
    // CRITICAL: This is here only for backwards compatiblity with old Connectivity-DSL - this will break any other user of ISL
    // so this build should only be included in ConnectivityDSL, and it will be deprecated soon.
    fun runTransform(fullFunctionName: String, operationContext: IOperationContext): Any? {
        return runBlocking {
            val moduleName = fullFunctionName.substringBeforeLast(":");
            val functionName = fullFunctionName.substringAfterLast(":", "run");

            val module = _modules[moduleName];
            if (module == null) {
                println("Unknown Module: $moduleName. Loaded Modules: $_modules");
                throw TransformException("Unknown Module: $moduleName", Position(moduleName, 0, 0));
            }

			val result = module.runTransformAsync(functionName, operationContext);

			when (result.result) {
				null -> {
					return@runBlocking null
				}
				is NullNode -> {
					return@runBlocking null
				}

				is ObjectNode -> {
                    val om = ObjectMapper();
                    val obj = om.convertValue(result.result, object : TypeReference<Map<String?, Any?>>() {})
                    return@runBlocking obj as Any;
				}
				is ArrayNode -> {
					val array = result.result as ArrayNode;
					val resultArray = array.map {
						ObjectMapper().convertValue(it, object : TypeReference<Map<String?, Any?>?>() {})
					}.toList();
					return@runBlocking resultArray;
				}

				else -> {
					return@runBlocking result.result;
				}
			}
        }
    }

	fun runTransformNew(fullFunctionName: String, operationContext: IOperationContext): JsonNode? {
        return runBlocking {
            val moduleName = fullFunctionName.substringBeforeLast(":");
            val functionName = fullFunctionName.substringAfterLast(":", "run");

            val module = _modules[moduleName];
            if (module == null) {
                println("Unknown Module: $moduleName. Loaded Modules: $_modules");
                throw TransformException("Unknown Module: $moduleName", Position(moduleName, 0, 0));
            }

			val result = module.runTransformAsync(functionName, operationContext);

			when (result.result) {
				null -> {
					return@runBlocking null
				}
				is NullNode -> {
					return@runBlocking null
				}

				else -> {
					return@runBlocking result.result;
				}
			}
        }
    }


    suspend fun runTransformAsync(
        fullFunctionName: String,
        operationContext: IOperationContext
    ): ITransformResult {
        val moduleName = fullFunctionName.substringBeforeLast(":");
        val functionName = fullFunctionName.substringAfterLast(":");

        val module =
            _modules[moduleName] ?: throw TransformException("Unknown Module: $moduleName", Position(moduleName, 0, 0));

        return module.runTransformAsync(functionName, operationContext);
    }
}