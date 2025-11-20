package com.intuit.isl.transform.testing.commands

import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.transform.testing.utils.runLoop
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.JsonConvert
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class PerfTest : BaseTransformTest() {

    class CustomException(message: String) : Exception(message);

    companion object {
        @JvmStatic
        fun simpleTransform(): Stream<Arguments> {
            return Stream.of(
                // modifier one level down
                Arguments.of(
                    "fun run( \$input ){\n" +
                            "   return {\n" +
//                            "        // JSON Paths selectors\n" +
//                            "        id: 1234,\n" +
//                            "        // modifier: `trim`\n" +
//                            "        name: \"\" | trim,\n" +
//                            "        // string interpolation ` `\n" +
//                            //"        short_description: `\${ \$input.title } by \${ \$input.vendor }`,    \n" +
//                            "        // child object building\n" +
////                            "        primary_image: {\n" +
////                            "            id: 1234,\n" +
////                            "            url: \"https://\",\n" +
////                            "        },\n" +
//                            "        // conditional properties\n" +
//                            //"        is_active: if ( \$input.status == 'active' ) true else false,\n" +
//                            "        option_name: \"\",\n" +
//                            "        // array to CSV\n" +
//                            "        option_values: [1,23] | join.string(),\n" +
//                            "        // date processing\n" +
//                            //"        updated: \$input.updated_at | date.fromEpochSeconds | to.string('YYYY-MM-DD HH:mm'),\n" +
//                            "        total: 1234 | calc,\n" +//                            "        id: \$input.id,\n" +


                            "        // JSON Paths selectors\n" +
                            "        id: \$input.id,\n" +
                            "        // modifier: `trim`\n" +
                            "        name: \$input.title | trim,\n" +
                            "        // string interpolation ` `\n" +
                            "        short_description: `\${ \$input.title } by \${ \$input.vendor }`,    \n" +
                            "        // child object building\n" +
                            "        primary_image: {\n" +
                            "            id: \$input.images[0].id,\n" +
                            "            url: \$input.images[0].src,\n" +
                            "        },\n" +
                            "        // conditional properties\n" +
                            "        is_active: if ( \$input.status == 'active' ) true else false,\n" +
                            "        option_name: \$input.options.name,\n" +
                            "        // array to CSV\n" +
                            "        option_values: \$input.options.values | join.string(),\n" +
                            "        // date processing\n" +
                            "        updated: \$input.updated_at | date.fromEpochSeconds | to.string('YYYY-MM-DD HH:mm'),\n" +
                            "        total: \$input.amount | calc,\n" +
                            "    };\n" +
                            "}\n" +
                            "modifier calc( \$amount ) {" +
                            "   return {{ \$amount * 3 / \$amount }};" +
                            "}" +
                            "",
                    """{"id":632910392,"name":"IPod Nano - 8GB","short_description":"IPod Nano - 8GB by Apple","primary_image":{"id":850703190,"url":"http://example.com/burton.jpg"},"is_active":true,"option_name":"Color","option_values":"Pink,Red,Green,Black","updated":"2022-02-47 09:45","total":3.0000}""",
                    mapOf(
                        "input" to JsonConvert.mapper.readTree(
                            "{\n" +
                                    "    \"title\": \"IPod Nano - 8GB\",\n" +
                                    "    \"body_html\": \"It's the small iPod with a big idea: Video.\",\n" +
                                    "    \"id\": 632910392,\n" +
                                    "    \"amount\": 1235.678,\n" +
                                    "    \"images\": [\n" +
                                    "        {\n" +
                                    "            \"id\": 850703190,\n" +
                                    "            \"src\": \"http://example.com/burton.jpg\"\n" +
                                    "        }\n" +
                                    "    ],\n" +
                                    "    \"options\": {\n" +
                                    "        \"name\": \"Color\",\n" +
                                    "        \"values\": [\n" +
                                    "            \"Pink\",\n" +
                                    "            \"Red\",\n" +
                                    "            \"Green\",\n" +
                                    "            \"Black\"\n" +
                                    "        ]\n" +
                                    "    },\n" +
                                    "    \"status\": \"active\",\n" +
                                    "    \"tags\": \"Emotive, Flash Memory, MP3, Music\",\n" +
                                    "    \"updated_at\": 1645004735,\n" +
                                    "    \"vendor\": \"Apple\"\n" +
                                    "}"
                        )
                    )
                ),
            )
        }


        @JvmStatic
        fun fileTransforms(): Stream<Arguments> {
            return Stream.of(
                // modifier one level down
//                Arguments.of(
//                    "perf/shopify.transform.isl",
//                    "perf/shopify.order.json",
//                    "perf/shopify.order.output.json",
//                    null
//                ),
            )
        }

        fun errorRaise(context: FunctionExecuteContext): Any? {
            throw CustomException(ConvertUtils.tryToString(context.firstParameter)!!);
        }
    }

//    @ParameterizedTest
    @MethodSource(
        "simpleTransform"
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map, runCount = 10000);
    }

//    @ParameterizedTest
    @MethodSource(
        "fileTransforms"
    )
    fun runFixturesWithInput(script: String, input: String, expectedResult: String) {
        val isl = readResource(script);
        val inputText = readResource(input);
        val expectedText = readResource(expectedResult);


        val params = mapOf<String, Any?>(
            "input" to JsonConvert.mapper.readTree(inputText),
        )
        run(isl, expectedText, params, runCount = 100000);
    }


    //    @ParameterizedTest
    @MethodSource(
        "fileTransforms"
    )
    fun runFileTransform(
        scriptName: String,
        payloadName: String,
        expectedResultName: String,
        map: Map<String, Any?>? = null
    ) {
        val runCount = 10000;

        runBlocking {
            val script = readResource(scriptName);
            val payload = runLoop(1, "parsePayload") {
                JsonConvert.mapper.readTree(readResource(payloadName));
            };

            val expected = readResource(expectedResultName);

            val variables = map?.toMutableMap() ?: mutableMapOf();
            variables["input"] = payload;

            val result = run(script, expected, variables, runCount = runCount);

            runLoop(1, "toStringOutput") {
                result.second.result?.toString()
            };
        }
    }
}