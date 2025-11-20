package com.intuit.isl.common

import com.intuit.isl.utils.Const
import com.intuit.isl.utils.ConvertUtils
import kotlinx.coroutines.runBlocking

/**
 * Context specific to one operation.
 * This is state-full and represents the current state of the operation and transformation.
 */
open class OperationContext : BaseOperationContext {
    constructor() {
    }

    /**
     * Register an extension method in format `Service.MethodName`.
     * Java style :)
     */
    fun registerJavaExtension(
        fullName: String,
        callback: java.util.function.Function<FunctionExecuteContext, Any?>
    ): OperationContext {
        val lower = fullName.lowercase();
        extensions[lower] = { context ->
            try {
                val cleanParams = context.parameters.map {
                    val r = ConvertUtils.extractFromNode(it);
                    return@map r;
                }.toTypedArray();

                val newContext =
                    FunctionExecuteContext(
                        context.functionName,
                        context.command,
                        context.executionContext,
                        cleanParams
                    );

                runBlocking {
                    return@runBlocking callback.apply(newContext);
                }
            } catch (e: Exception) {
//                context.executionContext.operationContext.interceptor?.onIssue(
//                    context.command,
//                    context.executionContext,
//                    "Failed to run {}: {}", arrayOf(fullName, e)
//                )
                throw e;
            }
        };
        return this;
    }

    /**
     * Register an annotation
     * Java style
     * */
    fun registerJavaAnnotation(
        annotationName: String,
        callback: java.util.function.Function<AnnotationExecuteContext, Any?>
    ): OperationContext {
        annotations[annotationName.lowercase()] = { context ->
            runBlocking {
                return@runBlocking callback.apply(context);
            }
        };
        return this;
    }

    fun registerFallbackFunctionHandler(
        callback: AsyncContextAwareExtensionMethod
    ): OperationContext {
        extensions[Const.FallbackMethodName] = { context ->
            callback(context);
        };
        return this;
    }

    fun tryRegisterExtensionMethod(
        fullName: String,
        callback: AsyncContextAwareExtensionMethod
    ): OperationContext {
        if (!extensions.containsKey(fullName.lowercase())) {
            extensions[fullName.lowercase()] = { context ->
                callback(context);
            }
        }

        return this;
    }
}

