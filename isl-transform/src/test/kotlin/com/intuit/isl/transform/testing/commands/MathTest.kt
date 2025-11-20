package com.intuit.isl.transform.testing.commands

import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.utils.ConvertUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream
import kotlin.math.sqrt

@Suppress("unused")
class MathTest : BaseTransformTest() {
    companion object {
        @JvmStatic
        fun simpleMath(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("r: {{ 1 + 2 }}", """{ "r": 3 }""", null),
                Arguments.of("r: {{ 5 - 8 }}", """{ "r": -3 }""", null),
                Arguments.of("r: {{ 2 * 3 }}", """{ "r": 6 }""", null),
                Arguments.of("r: {{ 8 / 2 }}", """{ "r": 4.0 }""", null),

                // div by zero
                Arguments.of("r: {{ 8 / 0 }}", """{ "r": 0 }""", null),

                // order of operations
                Arguments.of("r: {{ 1 + 2 * 3 }}", """{ "r": 7 }""", null),
                Arguments.of("r: {{ 10/5 + 2 * 3 }}", """{ "r": 8.0 }""", null),

                // ()
                Arguments.of("r: {{ (1 + 2) * 3 }}", """{ "r": 9 }""", null),
                Arguments.of("r: {{ 10 / (5 + 2) * 3 }}", """{ "r": 4.2858 }""", null),
            )
        }

        @JvmStatic
        fun funMath(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("" +
                        "fun run(){\n" +
                        "    \$t = 1;\n" +
                        "    \$i = 1;\n" +
                        "    \$r = [];\n" +
                        "\n" +
                        "    while( @.This.factorCount( \$t ) < 1001, { maxLoops: 100000 } )\n" +
                        "        \$i = {{ \$i + 1 }};\n" +
                        "        \$t = {{ \$t + \$i }}\n" +
                        // "       @.Log.Info(`t=\$t`);\n" +
                        "    endwhile\n" +
                        "\n" +
                        "    @.Log.Info(`Triangle \$t`);\n" +
                        "    return { result: \$t };\n" +
                        "}\n" +
                        "\n" +
                        "fun factorCount(\$n){\n" +
                        "    \$sq =  @.Math.Sqrt( \$n );\n" +
                        "    \$isq = \$sq | to.number;\n" +
                        "    \$count = if ( \$isq == \$sq ) -1 else 0;\n" +
                        "    \n" +
                        //"    @.Log.Info(`sq=\$sq is=\$isq count=\$count`);\n" +
                        "\n" +
                        "    \$candidate = 1;\n" +
                        "    while( \$candidate <= \$isq, { maxLoops: 100000 } )\n" +
                        "        if( @.Math.Mod( \$n, \$candidate ) == 0 )\n" +
                        "            \$count = {{ \$count + 2 }};\n" +
                        "        endif\n" +
                        "\n" +
                        "        \$candidate = {{ \$candidate + 1 }}\n" +
                        "    endwhile\n" +
                        "\n" +
                        //"    @.Log.Info(`Factor=\$n Count=\$count`);\n" +
                        "    return \$count;\n" +
                        "}\n" +
                        "" +
                        "", """{"result": 2162160}""", null),
            )
        };
    }

    @ParameterizedTest
    @MethodSource(
        "simpleMath",
        //"funMath",
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map);
    }

    fun factorCount(n: Int): Int{
        val square = sqrt(n.toDouble());
        val isSquare = square.toInt();
        var count = if(isSquare.toDouble() == square) -1 else 0;
        for ( i in 1..isSquare+1){
            if( n.mod(i) == 0)
                count += 2;
        }
        return count;
    }

    @Test
    fun testCount(){
        val startTime = Instant.now();

        var t = 1;
        var index = 1;
        while(factorCount(t) < 500)
        {
            index += 1;
            t += index;
        }

        val time = Duration.between(startTime, Instant.now());
        println("Time to run ${time.toMillis()}ms val $t")
    }
}