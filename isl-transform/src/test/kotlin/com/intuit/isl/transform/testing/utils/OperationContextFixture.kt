package com.intuit.isl.transform.testing.utils

import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.LocalOperationContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.parser.TransformParser
import com.intuit.isl.runtime.TransformCompiler
import com.intuit.isl.transform.testing.commands.BaseTransformTest
import com.intuit.isl.utils.ConvertUtils
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class OperationContextFixture : BaseTransformTest() {

    @Test
    fun testLocaOperationContext(){
        val parentContext = OperationContext()
            .registerExtensionMethod("Test.Extension", ::testExtension);

        val script = "value: @.Test.Extension( 'ABC' )"

        val transform = TransformCompiler().compileLocalIsl("test", script, parentContext);

        val localContext = LocalCarryContext(20);

        runBlocking {
            val result = transform.runTransformAsync("run", LocalOperationContext(parentContext), localContext).result!!;
            println(result.toPrettyString())

            compareJsonResults("\n" +
                    "{\n" +
                    "  \"value\" : \"ABC 20\"\n" +
                    "}", result)
        }
    }

    data class LocalCarryContext(val number: Int = 10);

    fun testExtension(context: FunctionExecuteContext): Any? {
        val local = context.executionContext.localContext as LocalCarryContext;

        val first = ConvertUtils.tryToString(context.firstParameter);
        val stringValue = first + " " + local.number;
        return stringValue;
    }
}