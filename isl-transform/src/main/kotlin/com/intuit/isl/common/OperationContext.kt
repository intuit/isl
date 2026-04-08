package com.intuit.isl.common

import com.intuit.isl.utils.Const
import com.intuit.isl.utils.ConvertUtils

/**
 * Context specific to one operation.
 * This is state-full and represents the current state of the operation and transformation.
 */
open class OperationContext : BaseOperationContext {
    constructor() {
    }

    /**
     * Register an extension method in format `Service.MethodName`.
     * Java style - directly stores as sync callback (no wrapping needed)
     */
    fun registerJavaExtension(
        fullName: String,
        callback: java.util.function.Function<FunctionExecuteContext, Any?>
    ): OperationContext {
        val lower = fullName.lowercase();
        // Store directly as sync - no suspend wrapping needed
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

                // Direct call - no bridge needed
                callback.apply(newContext)
            } catch (e: Exception) {
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
            // Bridge is no longer needed - will be handled by AnnotationCommand
            callback.apply(context)
        };
        return this;
    }

    fun registerFallbackFunctionHandler(
        callback: AsyncContextAwareExtensionMethod
    ): OperationContext {
        // Wrap async fallback handler to sync
        extensions[Const.FallbackMethodName] = { context ->
            SuspendBridge.callSuspend(context.executionContext.coroutineContext) {
                callback(context)
            }
        };
        return this;
    }

    fun tryRegisterExtensionMethod(
        fullName: String,
        callback: AsyncContextAwareExtensionMethod
    ): OperationContext {
        if (!extensions.containsKey(fullName.lowercase())) {
            // Wrap async extension to sync
            extensions[fullName.lowercase()] = { context ->
                SuspendBridge.callSuspend(context.executionContext.coroutineContext) {
                    callback(context)
                }
            }
        }

        return this;
    }
}

