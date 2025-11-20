package com.intuit.isl.utils.pagination

import com.intuit.isl.commands.ConditionEvaluator
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.StatementExecution
import com.intuit.isl.parser.tokens.FunctionCallToken
import com.intuit.isl.parser.tokens.VariableSelectorValueToken
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.JsonConvert

/**
 * Simple Page Pagination
 * ./docs/dsl/pagination.md
 */
object CursorPagination{
    suspend fun executeAsync(
        context: FunctionExecuteContext,
        statementsExtensionMethod: StatementExecution
    ): Any? {
        // execute a simple page pagination - we keep looping until the result is null, or we get an explicit $Page.continue
        val functionCallToken = context.command.token as? FunctionCallToken;
        val variableName =
            (functionCallToken?.arguments?.first() as? VariableSelectorValueToken)?.variableName
                ?: throw TransformException("Unknown format for variable for ${context.functionName}", context.command.token.position);

        var current: Any? = null;
        do {
            val pageVariable = JsonConvert.convert( mapOf(
                // important - we store the page details based on the variable name so we know how to restore them
                "current" to current,
                "next" to null
            ));
            context.executionContext.operationContext.setVariable(variableName, pageVariable);

            statementsExtensionMethod(context.executionContext).value;

            val pageVal = context.executionContext.operationContext.getVariable(variableName);
            val newNext =pageVal?.get("next");

            if(ConditionEvaluator.isValid(newNext)){
                if(ConditionEvaluator.equalish(newNext, current))
                    break;  // you are going in circles
                current = newNext;
            } else{
                break;
            }
        } while(true);

        // nothing to capture from a Pagination
        return null;
    }
}