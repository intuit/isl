package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.stream.Stream
import kotlin.run

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("unused")
class DateTimeTest : YamlTransformTest("modifiers") {
    private fun dateParse(): Stream<Arguments> {
        return createTests("dateparse")
    }



    companion object {
        @JvmStatic
        fun simpleDates(): Stream<Arguments> {
            val now = Instant.now();
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
            val value = formatter.format(now);
            val longText = DateTimeFormatter.ofPattern("'Year' yyyy 'Month' MM 'Day' dd").withZone(ZoneOffset.UTC);
            val longValue = longText.format(now);

            return Stream.of(
                //Arguments.of("r: @.Date.Now()", """{ "r": 3 }""", null),
                Arguments.of(
                    "\$r: @.Date.Now();\n" +
                            "r: \$r | to.string(\"yyyy-MM-dd\") ",
                    """{ "r": "$value" }""", null
                ),

                Arguments.of("r: @.Date.Now() | to.string(\"yyyy-MM-dd\") ", """{ "r": "$value" }""", null),
                //Arguments.of("r: @.Date.Now() | to.string(\"yyyyMMdd'T'HHmmss'Z'\") ", """{ "r": "$value" }""", null),
                Arguments.of(
                    "r: @.Date.Now() | to.string(\"'Year' yyyy 'Month' MM 'Day' dd\") ",
                    """{ "r": "$longValue" }""",
                    null
                ),
                //Arguments.of("r: @.Date.Now() | to.string(\"yyyyMMdd'T'HHmmssZ\") ", """{ "r": "$value" }""", null),
            );
        }

        @JvmStatic
        fun dateParsing(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "r: \"2021-12-03\" | date.parse(\"yyyy-MM-dd\") | to.string(\"yyMMdd\")",
                    """{ "r": "211203" }""", null
                ),

                Arguments.of(
                    "\$format: \"yyyy-MM-dd\";\n" + //
                    "r: \"2021-12-03\" | date.parse( \$format ) | to.string(\"yyMMdd\")",
                    """{ "r": "211203" }""", null
                ),

                // Our parsing is happy to ignore times the SSS and Z
                Arguments.of(
                    "r: \"2021-12-03T23:00:15\" | date.parse() | to.string(\"yyMMdd-HHmmss\")",
                    """{ "r": "211203-230015" }""", null
                ),
                Arguments.of(
                    "r: \"2021-12-03\" | date.parse() | to.string(\"yyMMdd-HHmmss\")",
                    """{ "r": "211203-000000" }""", null
                ),
                Arguments.of(
                    "r: \"2021-12-03\" | date.parse()",
                    """{ "r": "2021-12-03T00:00:00.000Z" }""", null
                ),

                // short month
                Arguments.of(
                    "r: \"2021-02-03\" | date.parse(\"yyyy-MM-dd\") | to.string(\"yy-MM-dd\")",
                    """{ "r": "21-02-03" }""", null
                ),

                Arguments.of(
                    "r: \"2021-2-03\" | date.parse(\"yyyy-M-dd\") | to.string(\"yy-M-dd\")",
                    """{ "r": "21-2-03" }""", null
                ),

                Arguments.of(
                    "r: \"20211203\" | date.parse(\"yyyyMMdd\") | to.string(\"yyMMdd\")",
                    """{ "r": "211203" }""", null
                ),

                Arguments.of(
                    "r: \"20211203T201201\" | date.parse(\"yyyyMMdd'T'HHmmss\") | to.string(\"yyyy-MM-dd-HH-mm-ss\")",
                    """{ "r": "2021-12-03-20-12-01" }""", null
                ),

                // can't parse > get null
                Arguments.of(
                    "r: null | date.parse(\"yyyyMMdd\") | to.string(\"yyMMdd\")",
                    """{ "r": null }""", null
                ),
                Arguments.of(
                    "r: \"\" | date.parse(\"yyyyMMdd\") | to.string(\"yyMMdd\")",
                    """{ "r": null }""", null
                ),
                Arguments.of(
                    "r: \"202112X3\" | date.parse(\"yyyyMMdd\") | to.string(\"yyMMdd\")",
                    """{ "r": null }""", null
                ),

                // Conversions
                Arguments.of(
                    "r: \"2021-12-03T01:29:27.153Z\" | date.parse() | to.number",
                    """{ "r": 1638494967 }""", null
                ),

                Arguments.of(
                    "r: \"1638494967\" | date.fromEpochSeconds | to.string",
                    """{ "r": "2021-12-03T01:29:27.000Z" }""", null
                ),

                Arguments.of(
                    "r: 1645004735 | date.fromEpochSeconds | to.string",
                    """{ "r": "2022-02-16T09:45:35.000Z" }""", null
                ),

                Arguments.of(
                    "r: \"2021-12-03T01:29:27.153Z\" | date.parse() | to.epochmillis",
                    """{ "r": 1638494967153 }""", null
                ),

                Arguments.of(
                    "r: \"2021-12-03T01:29:27.153Z\" | date.parse('ISO_8601') | to.epochmillis",
                    """{ "r": 1638494967153 }""", null
                ),

                Arguments.of(
                    "r: \"1638494967153\" | date.fromEpochMillis | to.string",
                    """{ "r": "2021-12-03T01:29:27.153Z" }""", null
                ),
                Arguments.of(
                    "\$t: \"1601877381344\"; " +
                            "r: \$t | date.fromEpochMillis | to.string",
                    """{ "r": "2020-10-05T05:56:21.344Z" }""", null
                ),

                // am/pm
                Arguments.of(
                    "r: \"4/10/2021 3:12:40 AM\" | date.parse(\"M/d/yyyy h:mm:ss a\")",
                    """{ "r": "2021-04-10T03:12:40.000Z" }""", null
                ),
                Arguments.of(
                    "r: \"4/10/2021 03:12:40 AM\" | date.parse(\"M/d/yyyy hh:mm:ss a\")",
                    """{ "r": "2021-04-10T03:12:40.000Z" }""", null
                ),
                Arguments.of(
                    "r: \"4/10/2021 3:12:40 PM\" | date.parse(\"M/d/yyyy h:mm:ss a\")",
                    """{ "r": "2021-04-10T15:12:40.000Z" }""", null
                ),
                Arguments.of(
                    "r: \"4/10/2021 03:12:40 PM\" | date.parse(\"M/d/yyyy hh:mm:ss a\")",
                    """{ "r": "2021-04-10T15:12:40.000Z" }""", null
                ),
                Arguments.of(
                    "r: \"10/4/2021 3:12:40 am\" | date.parse(\"d/M/yyyy h:mm:ss a\", { locale: \"en_AU\"})",
                    """{ "r": "2021-04-10T03:12:40.000Z" }""", null
                ),
                Arguments.of(
                    "r: \"10/4/2021 03:12:40 am\" | date.parse(\"d/M/yyyy hh:mm:ss a\", { locale: \"en_AU\"})",
                    """{ "r": "2021-04-10T03:12:40.000Z" }""", null
                ),
            )
        }

        @JvmStatic
        fun dateArrayParsing(): Stream<Arguments> {
            return Stream.of(
                // array conversions
                Arguments.of(
                    "r: \"20211203\" | date.parse( [\"yyyy-MM-dd\",\"dd-MM-YY\",\"yyyyMMdd\"]) | to.string(\"yyMMdd\")",
                    """{ "r": "211203" }""", null
                ),
                Arguments.of(
                    "\$f1: \"yyyy-MM-dd\";\n" +
                    "\$f2: \"yyyyMMdd\";\n" +
                    "r: \"20211203\" | date.parse( [ \$f1, \"dd-MM-YY\", \$f2 ]) | to.string(\"yyMMdd\")",
                    """{ "r": "211203" }""", null
                ),
                Arguments.of(
                    "r: \"20211214\" | date.parse( [\"yyyyddMM\",\"dd-MM-YY\",\"yyyyddMM\"]) | to.string(\"yyMMdd\")",
                    """{ "r": null }""", null
                ),
                Arguments.of(
                    "r: \"20211214\" | date.parse( [\"yyyy-yy-dd\",\"dd-MM-YY\",\"yyyyddMM\",\"yyyyMMdd\"]) | to.string(\"yyMMdd\")",
                    """{ "r": "211214" }""", null
                ),
            )
        }

        @JvmStatic
        fun dateOperations(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "r: \"2021-12-03\" | date.parse(\"yyyy-MM-dd\") | date.add( 1, 'DAYS' ) | to.string(\"yyMMdd\")",
                    """{ "r": "211204" }""", null
                ),


                Arguments.of(
                    "r: \"2021-12-03\" | date.parse(\"yyyy-MM-dd\") | date.add( -3, 'DAYS' ) | to.string(\"yyMMdd\")",
                    """{ "r": "211130" }""", null
                ),

                // CT: For Ranges larger than Month we need to go via the LocalDate so we're loosing millis precision
                Arguments.of(
                    "r: \"2021-12-03\" | date.parse(\"yyyy-MM-dd\") | date.add( -3, 'MONTHS' ) | to.string(\"yyMMdd\")",
                    """{ "r": "210903" }""", null
                ),
                Arguments.of(
                    "r: \"2021-12-03\" | date.parse(\"yyyy-MM-dd\") | date.add( -3, 'YEARS' ) | to.string(\"yyMMdd\")",
                    """{ "r": "181203" }""", null
                )
            );
        }
    }


    @ParameterizedTest
    @MethodSource(
        "simpleDates",
        "dateParsing",
        "dateArrayParsing",
        "dateOperations"
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map);
    }


    @ParameterizedTest
    @MethodSource(
        "dateParse",
    )
    fun runFixtures(testName: String, script: String, expectedResult: JsonNode, map: Map<String, Any?>? = null) {
        run(script, expectedResult.toPrettyString(), map)
    }
}