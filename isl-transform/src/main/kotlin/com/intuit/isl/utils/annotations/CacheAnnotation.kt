package com.intuit.isl.utils.annotations

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.utils.JsonConvert
import java.util.concurrent.ConcurrentHashMap

interface IIslCache {
    fun get(key: String): Any?;
    fun set(key: String, value: Any?);
}

object CacheAnnotation {
    private val MissingObject = Any();

    fun registerCacheAnnotation(operationContext: IOperationContext, cacheBacking: IIslCache) {
        operationContext.registerAnnotation("defaultCache") { context ->
            val param = if (context.parameters.isNotEmpty()) context.parameters
            else context.functionParameters;

            val cacheKey = JsonNodeFactory.instance.objectNode()
                .put(
                    "fn",
                    context.command.token.position.file + ":@" + context.annotationName + "->" + context.functionName
                )
                .set<JsonNode>("param", JsonConvert.mapper.valueToTree(param))

            val key = cacheKey.toString();

            val cachedResult = cacheBacking.get(key);

            if (cachedResult == MissingObject) {
                // We need to invoke the next command
                val result = context.runNextCommand();
                cacheBacking.set(key, result);
                return@registerAnnotation result
            }
            return@registerAnnotation cachedResult;
        };
    }

    fun registerInMemoryCacheAnnotation(context: IOperationContext) {
        class ConcurrentCache : IIslCache{
            var cache: ConcurrentHashMap<JsonNode, Any?> = ConcurrentHashMap<JsonNode, Any?>();

            override fun get(key: String): Any? {
                return cache.getOrDefault(JsonConvert.convert(key), MissingObject);
            }

            override fun set(key: String, value: Any?) {
                cache[JsonConvert.convert(key)] = value
            }
        }
        registerCacheAnnotation(context, ConcurrentCache())
    }
}