package com.intuit.isl.dynamic

import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.runtime.Transformer

suspend fun IOperationContext.run(command: IIslCommand, localContext: Any? = null): Any?{
    val context = ExecutionContext(this, localContext);
    val result = command.executeAsync(context);
    return result.value;
}