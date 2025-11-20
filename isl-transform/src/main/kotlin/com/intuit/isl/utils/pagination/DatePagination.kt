package com.intuit.isl.utils.pagination

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.StatementExecution
import com.intuit.isl.parser.tokens.FunctionCallToken
import com.intuit.isl.parser.tokens.VariableSelectorValueToken
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.DateExtensions
import com.intuit.isl.utils.InstantNode
import com.intuit.isl.utils.JsonConvert
import java.time.Duration
import java.time.Instant

/**
 * Simple Page Pagination
 * ./docs/dsl/pagination.md
 */
object DatePagination {
    suspend fun executeAsync(
        context: FunctionExecuteContext,
        statementsExtensionMethod: StatementExecution
    ): Any? {
        // execute a simple page pagination - we keep looping until the result is null, or we get an explicit $Page.continue
        // Self Introspection :)    // @.Pagination.Date( $variable, { parameters } )
        val functionCallToken = context.command.token as? FunctionCallToken;
        val variableName =
            (functionCallToken?.arguments?.first() as? VariableSelectorValueToken)?.variableName
                ?: throw TransformException(
                    "Unknown format for variable for ${context.functionName}",
                    context.command.token.position
                );

        val parameters = context.secondParameter as? ObjectNode;
        val startDate = DateExtensions.getDate(parameters?.get("startDate"))!!;
        val endDate = DateExtensions.getDate(parameters?.get("endDate")) ?: DateExtensions.now(null) as? Instant;
        val durationText = ConvertUtils.tryToString(parameters?.get("duration")) ?: "P1D";

        val duration: Duration =
            try {
                Duration.parse(durationText);
            } catch (e: Exception) {
                throw TransformException(
                    "Could not parse duration $durationText as a valid Duration. Expected format is 'PnnDTnnHnnMnnS'.",
                    context.command.token.position
                );
            }

        var page = 0;

        var thisStartDate = startDate;
        do {
            var nextEndDate = thisStartDate.plus(duration);
            // execute once when the period between start date and end date is less than the duration
            if (endDate != null) {
                if ( (nextEndDate > endDate && endDate > thisStartDate && !duration.isNegative()) || // Positive duration
                     (nextEndDate < endDate && endDate < thisStartDate && duration.isNegative())     // Negative duration
                    ) {
                    nextEndDate = endDate;
                }
            }
            if ((nextEndDate > endDate && !duration.isNegative()) ||    // Positive duration
                (nextEndDate < endDate && duration.isNegative())        // Negative duration
            ) {
                break;
            }

            val pageVariable = JsonNodeFactory.instance.objectNode();
            pageVariable.set<JsonNode>("startDate", JsonConvert.convert(thisStartDate));
            pageVariable.set<JsonNode>("endDate" , JsonConvert.convert(nextEndDate));
            pageVariable.set<JsonNode>("page", JsonConvert.convert(page));

            context.executionContext.operationContext.setVariable(variableName, pageVariable);

            statementsExtensionMethod(context.executionContext).value;

            thisStartDate = nextEndDate;

            // yey -> next page
            page += 1;
        } while (true);

        // nothing to capture from a Pagination
        return null;
    }
}