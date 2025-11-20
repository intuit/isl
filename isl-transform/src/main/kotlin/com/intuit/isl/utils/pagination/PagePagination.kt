package com.intuit.isl.utils.pagination

import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.StatementExecution
import com.intuit.isl.parser.tokens.FunctionCallToken
import com.intuit.isl.parser.tokens.VariableSelectorValueToken
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert

/**
 * Simple Page Pagination
 * ./docs/dsl/pagination.md
 */
object PagePagination{
    suspend fun executeAsync(
        context: FunctionExecuteContext,
        statementsExtensionMethod: StatementExecution
    ): Any? {
        // execute a simple page pagination - we keep looping until the result is null, or we get an explicit $Page.continue
        // Self Introspection :)    // @.Pagination.Page( $variable, { parameters } )
        val functionCallToken = context.command.token as? FunctionCallToken;
        val variableName =
            (functionCallToken?.arguments?.first() as? VariableSelectorValueToken)?.variableName
            ?: throw TransformException("Unknown format for variable for ${context.functionName}", context.command.token.position);

        val parameters = context.secondParameter as ObjectNode?;
        val startIndex = ConvertUtils.tryParseLong(parameters?.get("startIndex")) ?: 0;
        var pageSize = ConvertUtils.tryParseLong(parameters?.get("pageSize")) ?: 100;
        if (pageSize <= 0)
            pageSize = 100;

        var page = startIndex;
        do {
            val pageVariable = JsonConvert.convert( mapOf(
                // important - we store the page details based on the variable name so we know how to restore them
                "name" to variableName,
                "startIndex" to startIndex,
                "pageSize" to pageSize,
                "page" to page,
                "fromOffset" to (page * pageSize),
                "toOffset" to ((page+1) * pageSize),
                "hasMorePages" to false
            ));
            context.executionContext.operationContext.setVariable(variableName, pageVariable);

            statementsExtensionMethod(context.executionContext).value;

            val pageVal = context.executionContext.operationContext.getVariable(variableName);
            if( (pageVal?.get("hasMorePages") as? BooleanNode)?.booleanValue() == true){
                // continue
            } else {
                // we are done here - we finished processing all the pages
                //context.operationContext.info("Pagination: $$variableName has no more pages to process.");
                break;
            }

            // yey -> next page
            page += 1;
        } while(true);

        // nothing to capture from a Pagination
        return null;
    }
}