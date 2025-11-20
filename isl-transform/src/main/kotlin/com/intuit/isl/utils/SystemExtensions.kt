package com.intuit.isl.utils

import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import kotlinx.coroutines.delay
import java.util.*

object SystemExtensions {
    fun registerExtensions(context: IOperationContext) {
        context.registerExtensionMethod("Run.Sleep", SystemExtensions::sleep);
        context.registerExtensionMethod("UUID.New", SystemExtensions::uuidNew);
    }

    private suspend fun sleep(context: FunctionExecuteContext): Any? {
        val time = ConvertUtils.tryParseInt(context.firstParameter) ?: 0;
        delay(time.toLong());
        return null;
    }
    @Suppress("UNUSED_PARAMETER")
    private fun uuidNew(context: FunctionExecuteContext): Any? {
        return UUID.randomUUID().toString();
    }
}