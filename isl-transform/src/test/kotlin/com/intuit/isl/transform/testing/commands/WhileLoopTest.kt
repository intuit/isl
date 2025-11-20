package com.intuit.isl.transform.testing.commands


import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.run

@Suppress("unused")
class WhileLoopTest : BaseTransformTest() {
	companion object {
		@JvmStatic
		fun whileLoopFixture(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					"\$first: 1;\n" +
							"while ( \$first < 5 )\n" +
							" \$first : {{ \$first + 1 }}\n" +
							"endwhile\n" +
							"result: \$first", """{ "result": 5 }""", null
				),
				Arguments.of(
					"\$first: 1;\n" +
							"while ( \$first < 5 )\n" +
							" \$first : {{ \$first + 1 }}\n" +
							" \$second : {{ \$first + 2 }}\n" +
							"endwhile\n" +
							"result1: \$first\n" +
							"result2: \$second", """{ "result1": 5, "result2": 7 }""", null
				),
				Arguments.of(
					"\$first: 1;\n" +
							"while ( \$first < 5 or \$second < 10 )\n" +
							" \$first : {{ \$first + 1 }}\n" +
							" \$second : {{ \$first + 2 }}\n" +
							"endwhile\n" +
							"result1: \$first\n" +
							"result2: \$second", """{ "result1": 8, "result2": 10 }""", null
				),
				Arguments.of(
					"\$first: 1;\n" +
							"while ( (\$first < 5) and (\$second < 10) )\n" +
							" \$first : {{ \$first + 1 }}\n" +
							" \$second : {{ \$first + 2 }}\n" +
							"endwhile\n" +
							"result1: \$first\n" +
							"result2: \$second", """{ "result1": 5, "result2": 7 }""", null
				),
				Arguments.of(
					"\$first: 1;\n" +
							"while ( \$first < 5 or \$second < 10, { maxLoops: 3 } )\n" +
							" \$first : {{ \$first + 1 }}\n" +
							" \$second : {{ \$first + 2 }}\n" +
							"endwhile\n" +
							"result1: \$first\n" +
							"result2: \$second", """{ "result1": 4, "result2": 6 }""", null
				),
				// make sure default max is 50
				Arguments.of(
					"\$first: 0;\n" +
							"while ( \$first < 500 )\n" +
							" \$first : {{ \$first + 1 }}\n" +
							"endwhile\n" +
							"result: \$first", """{ "result": 50 }""", null
				),
				Arguments.of(
					"\$first: 0;\n" +
							"while ( \$first < 500 , { maxLoops: 100})\n" +
							" \$first : {{ \$first + 1 }}\n" +
							"endwhile\n" +
							"result: \$first", """{ "result": 100 }""", null
				),
				// MAX MAX - 5000
				Arguments.of(
					"\$first: 0;\n" +
							"while ( \$first < 5500, { maxLoops: 6000} )\n" +
							" \$first : {{ \$first + 1 }}\n" +
							"endwhile\n" +
							"result: \$first", """{ "result": 5000 }""", null
				),
				Arguments.of(
					"\$first: 0;\n" +
							"while ( \$first > 500 , { maxLoops: 100} )\n" +
							" \$first : {{ \$first + 1 }}\n" +
							"endwhile\n" +
							"result: \$first", """{ "result": 0}""", null
				),
				Arguments.of(
					"\$i: 1;\n" +
							"loopres: while ( \$i < 4 )\n" +
							"{\n" +
							"	first: \$i,\n" +
							"	\$i = {{ \$i + 1 }}\n" +
							"}\n" +
							"endwhile\n", """{ "loopres": [ { "first": 1 }, { "first": 2 }, { "first": 3 } ] }""", null
				),
				Arguments.of(
					"\$i: 1;\n" +
							"loopres: while ( \$i > 4 )\n" +
							"{\n" +
							"	first: \$i,\n" +
							"	\$i = {{ \$i + 1 }}\n" +
							"}\n" +
							"endwhile\n", """{ "loopres": [] }""", null
				),
				Arguments.of(
					"\$i: 1;\n" +
							"loopres: while ( \$i < 3 )\n" +
							"{\n" +
							"	first: \$i,\n" +
							"	second: {{ \$i + 1 }},\n" +
							"	\$i = {{ \$i + 1 }}\n" +
							"}\n" +
							"endwhile\n", """{ "loopres": [{ "first" : 1, "second": 2}, { "first" :2, "second": 3 }] }""", null
				),
			)
		}
	}

	@ParameterizedTest
	@MethodSource(
		"whileLoopFixture"
	)
	fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
		run(script, expectedResult, map);
	}
}