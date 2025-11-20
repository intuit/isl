package com.intuit.isl.transform.testing.java;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intuit.isl.common.OperationContext;
import com.intuit.isl.runtime.*;
import com.intuit.isl.utils.JsonConvert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JavaRunTest {

    @Test
    public void testScriptFromJava() throws IOException {
        String script = "fun run( $myVar ){\n" +
                "$t = {\n" +
                "       prop1: $myVar,\n" +
                "       prop2: $myVar.field,\n" +
                "   }\n" +
                "   @.Log.Info(\"returning \", $t);\n" +
                "   return $t;\n" +
                "}\n";

        ISLJavaHost host = new ISLJavaHost();

        ITransformer t = host.compile(script);

        JsonNode result = host.runScript(t, "run");

        System.out.println(result.toString());
        compareJsonResults("{\n" +
                "  \"prop1\" : {\n" +
                "    \"field\" : \"text value\"\n" +
                "  },\n" +
                "  \"prop2\" : \"text value\"\n" +
                "}", result);
    }

    @Test
    public void testScriptFromJava2() throws IOException {
        String script = "fun run( $myVar ){\n" +
                "$t = {\n" +
                "       prop1: 123\n" +
                "   }\n" +
                "   return $t;\n" +
                "}\n";

        TransformPackage p = new TransformPackageBuilder()
                .build(Arrays.asList(new FileInfo("test.isl", script)), null);

        assertNotNull(p.getModule("test.isl"));

        var result = p.runTransform("test.isl:run", new OperationContext());

        System.out.println(result);

        ObjectNode jo = (ObjectNode) JsonConvert.INSTANCE.convert(result);
        compareJsonResults("{ \"prop1\" : 123\n }", jo);
    }

    @Test
    public void testSimpleModifierFromJava() {
        String script = "modifier run( $myVar ){\n" +
                "   return \"value\";\n" +
                "}\n";

        ISLJavaHost host = new ISLJavaHost();

        ITransformer t = host.compile(script);

        JsonNode result = host.runScript(t, "run");

        System.out.println(result.toString());
        assertEquals("value", JsonConvert.INSTANCE.getValue(result));
    }

    @Test
    public void testAnnotationFromJava() {
        String script ="@Cache\n" +
                "fun child( $obj, $value ) {\n" +
                "   $val: {{ $obj.value * 10 + $value}};\n" +
                "   return $val;\n" +
                "}\n" +
                // main entry point is the run function
                "fun run( $v1, $v2 ){\n" +
                "   $v1: { name: \"test1\", value: 1 };\n" +
                "   $v2: 2;\n" +
                "   $result: @.This.child( $v1, $v2 );\n" +
                "   $cachedResult: @.This.child( $v1, $v2 );\n" +
                "   return { result: $result, cachedResult: $cachedResult };\n" +
                "}\n";

        ISLJavaHost host = new ISLJavaHost();

        ITransformer t = host.compile(script);

        JsonNode result = host.runScript(t, "run");

        System.out.println(result.toString());
        assertEquals("{\"result\":12,\"cachedResult\":12}", result.toString());

    }

    @Test
    public void testAnnotationFromJava2() {
        String script ="@Cache({ key: $tax.name })\n" +
                "fun child( $tax, $tax2 ) {\n" +
                "   $val: {{ $tax.value * 10 + $tax2.value}};\n" +
                "   return $val;\n" +
                "}\n" +
                // main entry point is the run function
                "fun run( $v1, $v2 ){\n" +
                "   $v1: { name: \"test1\", value: 1 };\n" +
                "   $v2: { name: \"test2\", value: 2 };\n" +
                "   $result: @.This.child( $v1, $v2 );\n" +
                "   $cachedResult: @.This.child( $v1, $v2 );\n" +
                "   return { result: $result, cachedResult: $cachedResult };\n" +
                "}\n";

        ISLJavaHost host = new ISLJavaHost();

        ITransformer t = host.compile(script);

        JsonNode result = host.runScript(t, "run");

        System.out.println(result.toString());
        assertEquals("{\"result\":12,\"cachedResult\":12}", result.toString());

    }

    //@Test
    public void loopTestScriptFromJava() {
        String script = "fun run(){\n" +
                "$t = {\n" +
                "       prop1: $myVar,\n" +
                "       prop2: $myVar.field,\n" +

                "       prop1: $myVar,\n" +
                "       prop2: $myVar.field,\n" +

                "       prop1: $myVar,\n" +
                "       prop2: $myVar.field,\n" +

                "       prop1: $myVar,\n" +
                "       prop2: $myVar.field,\n" +

                "       prop1: $myVar,\n" +
                "       prop2: $myVar.field,\n" +

                "       prop1: $myVar,\n" +
                "       prop2: $myVar.field,\n" +
                "   }\n" +
                //"   @.Log.Info(\"returning {}\", $t);\n" +
                "   return $t;\n" +
                "}\n";

        ISLJavaHost host = new ISLJavaHost();

        ITransformer t = host.compile(script);

        JsonNode result = null;
        host.runScript(t, "run");

        System.out.println("Starting Test");
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            result = host.runScript(t, "run");
        }
        long end = System.currentTimeMillis();
        System.out.println("Time:" + (end - start) + " result:" + result);
    }

    @Test
    public void testPackageWithName() throws Exception {
        String script = "fun run(){ return { val: true }; }";

        run(script, "test1.isl:run", "{ \"val\": true }");
        run(script, "test1.isl", "{ \"val\": true }");

        run("do: \"stuff\"", "test1.isl", "{ \"do\": \"stuff\" }");
        run("do: \"stuff\"", "test1.isl:run", "{ \"do\": \"stuff\" }");
    }

    @Test()
    public void testMissingFunction() throws Exception {
        Exception exception = assertThrows(TransformException.class, () -> {
            run("", "test1.isl", null);
        });
        assertEquals("Unknown Function @.test1.isl.run at Position(file=test1.isl, line=1, column=0, endLine=1, endColumn=5)", exception.getMessage());

        exception = assertThrows(TransformException.class, () -> {
            run("", "test1.isl:random", null);
        });
        assertEquals("Unknown Function @.test1.isl.random at Position(file=test1.isl, line=1, column=0, endLine=1, endColumn=5)", exception.getMessage());
    }

    private void run(String script, String function, String expectedResult) throws Exception {
        List<FileInfo> files = new ArrayList<>();
        files.add(new FileInfo("test1.isl", script));
        TransformPackage pack = new TransformPackageBuilder().build(files, null);

        OperationContext context = new OperationContext();

        Object result = pack.runTransform(function, context);

        ObjectMapper mapper = new ObjectMapper();
        String toString = mapper.writeValueAsString(result);
        System.out.println("Expected:\n" + expectedResult);
        System.out.println("Transformed:\n" + toString);

        if (expectedResult == null)
            assertNull(result);
        else
            assertEquals(mapper.readTree(expectedResult), mapper.readTree(toString));
    }


    private void compareJsonResults(String expectedJson, JsonNode result) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // we need this so 60 does not get written as 6e1 :(
        mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

        System.out.println("Transformed:\n${mapper.writeValueAsString(result)}");

        assertEquals(mapper.readTree(expectedJson), mapper.readTree(result.toString()));
    }
}
