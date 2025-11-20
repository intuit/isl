package com.intuit.isl.transform.testing.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intuit.isl.common.AnnotationExecuteContext;
import com.intuit.isl.common.FunctionExecuteContext;
import com.intuit.isl.common.IOperationContext;
import com.intuit.isl.common.OperationContext;
import com.intuit.isl.runtime.ITransformer;
import com.intuit.isl.runtime.TransformCompiler;
import com.intuit.isl.utils.ConvertUtils;
import com.intuit.isl.utils.JsonConvert;
import kotlin.coroutines.Continuation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ISLJavaHost {

	ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<String, Object>();;
	public ITransformer compile(String script) {
		// Compile the script. t now contains the execution graph of your script
		// If you want to support `import` statements better to use the
		// TransformPackageBuilder to build a complete "package"
		// by adding multiple files together
		ITransformer t = new TransformCompiler().compileIsl("myscript.isl", script, null);
		return t;
	}

	public JsonNode runScript(ITransformer t, String functionName) {
		// System.out.println("Running Script\n" + script);

		// Now you can cache the t if you want and reuse it as many times as you want
		// without re-compiling it

		// note that current error handling during compilation is quite poor :)

		// prepare an operation context
		OperationContext context = new OperationContext();
		context.registerJavaExtension("Log.Info", this::logInfo);

		// add any custom variables you want accessible in the script
		ObjectNode myVariable = JsonNodeFactory.instance.objectNode();
		myVariable.put("field", "text value");

		context.setVariable("$myVar", JsonConvert.INSTANCE.convert(myVariable), false);

		// Register custom callback extension methods.
		// This will be available from the script as @.MyService.DoStuff( ... )
		context.registerJavaExtension("MyService.DoStuff", this::DoStuff);
		context.registerJavaExtension("Modifier.testModify", this::DoStuff);

		context.registerJavaAnnotation("Cache", this::cache);

		// run
		JsonNode result = t.runTransformSync(functionName, context);
		return result;
	}

	private Object cache(AnnotationExecuteContext context) {

		Object[] param = context.getParameters().length > 0 ? context.getParameters() : context.getFunctionParameters();
		String cacheKey = "fn:" + context.getCommand().getToken().getPosition().getFile()
				+ ":@" + context.getAnnotationName()
				+ "->" + context.getFunctionName()
				+ "param:" + Arrays.toString(param);

		Object cachedResult = cache.get(cacheKey);
		if (cachedResult == null) {
			// Invoke the next command
			Object result = context.runNextCommandSync();
			cache.put(cacheKey, result);
			return result;
		}
		return cachedResult;
	}

	public Object DoStuff(FunctionExecuteContext context) {
		List<Object> l = Arrays.asList(context.getParameters());
		// check your parameters and do some processing. Most parameters are some
		// flavour of JsonNode
		// you can easily convert those using

		String value = ConvertUtils.Companion.tryToString(context.getFirstParameter());

		return "Hi " + context.getFirstParameter();
	}

	public Object logInfo(FunctionExecuteContext context) {
		StringBuilder message = new StringBuilder("#: ");
		for (Object p : context.getParameters()) {
			message.append(p);
		}
		System.out.println(context.component2().getToken().getPosition() + ":" + message);
		return null;
	}
}
