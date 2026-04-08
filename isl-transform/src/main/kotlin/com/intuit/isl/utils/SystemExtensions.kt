package com.intuit.isl.utils

import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import java.util.*

object SystemExtensions {
    fun registerExtensions(context: IOperationContext) {
        context.registerSyncExtensionMethod("Run.Sleep", SystemExtensions::sleep);
        context.registerSyncExtensionMethod("UUID.New", SystemExtensions::uuidNew);
    }

    private fun sleep(context: FunctionExecuteContext): Any? {
        val time = ConvertUtils.tryParseInt(context.firstParameter) ?: 0
        Thread.sleep(time.coerceAtLeast(0).toLong())
        return null
    }
    @Suppress("UNUSED_PARAMETER")
    private fun uuidNew(context: FunctionExecuteContext): Any? {
        return UUID.randomUUID().toString();
    }
}