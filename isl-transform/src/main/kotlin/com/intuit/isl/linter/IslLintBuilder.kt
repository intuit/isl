//package com.intuit.isl.linter
//
//import com.fasterxml.jackson.databind.JsonNode
//import com.fasterxml.jackson.databind.node.ObjectNode
//import com.intuit.isl.commands.AnnotationCommand
//import com.intuit.isl.commands.ObjectBuildCommand
//import com.intuit.isl.commands.builder.IJsonVisitor
//import com.intuit.isl.commands.builder.IslJsBuilder
//import com.intuit.isl.common.AnnotationExecuteContext
//import com.intuit.isl.common.ExecutionContext
//import com.intuit.isl.common.FunctionExecuteContext
//import com.intuit.isl.common.OperationContext
//import com.intuit.isl.parser.tokens.IIslToken
//import com.intuit.isl.parser.tokens.ModuleImplementationToken
//import com.intuit.isl.runtime.ITransformer
//import com.intuit.isl.runtime.TransformCompilationException
//import com.intuit.isl.runtime.TransformCompiler
//import com.intuit.isl.utils.ConvertUtils
//import com.intuit.isl.utils.JsonConvert
//
///**
// * This is a bit crazy, but we want to be able to LINT ISL and provide feedback,
// * either fail ISL or give a recommendation for better code.
// *
// * However, to make this really crazy we want to use ISL to build the validation rules.
// * https://intuit-teams.slack.com/archives/D7ZPJESJZ/p1697403292459869
// * That means that the process is like this:
// *
// * 1. We load a set of ISL validation rules and compile those.
// *    This creates a IIslLinter instance.
// *
// * 2. When we load a specific ISL, we tokenize it, then we export that to JSON
// *
// * 3. We apply the IIslLinter to the exported JSON to give us linting instructions
// */
//class IslLintBuilder {
//    private lateinit var linter: ITransformer;
//
//    private data class LintDeclaration(val instruction: String, val filter: ObjectNode?, val function: String);
//    private val tests = mutableListOf<LintDeclaration>()
//
//    suspend fun buildLinter(lintScript: String): IslLintBuilder {
//        linter = TransformCompiler().compileIsl("linter", lintScript);
//
//        // analyze the registered `@Lint( instruction: '', filter: { ... } )
//        linter.module.functions.forEach {
//            var annotation = it.statements;
//            while (annotation is AnnotationCommand) {
//                // { instruction: 'if' }
//                val arg = annotation.arguments.first() as ObjectBuildCommand;
//                // execute it so we get a nice ObjectNode out of it
//                val params = arg.executeAsync(ExecutionContext(OperationContext(), null)).value as ObjectNode;
//
//                // { instruction: 'if' }
//                tests.add(
//                    LintDeclaration(
//                        params["instruction"]?.textValue() ?: "",
//                        params["filter"] as? ObjectNode?,
//                        it.name
//                    )
//                )
//
//                annotation = annotation.nextCommand;
//            }
//        }
//
//        println("Detected the following Linter directives:")
//        tests.forEach {
//            println("    $it")
//        }
//
//        return this;
//    }
//
//    fun lintCode(script: String, module: ModuleImplementationToken): List<LintIssue> {
//        val visitor = JsonVisitor(script, this);
//        try {
//            IslJsBuilder("test", module, visitor, null).build();
//        } catch (e: TransformCompilationException) {
//            println(e.message);
//            println(e.stackTraceToString());
//            return listOf();
//        }
//
//
//        // due to how the hierarchy works we might report multiple identical errors with bigger depth
//        // e.g. value | concat | concat will return two errors, one for the first "value | concat" and one for "value | concat | concat"
//        // clear duplicates
//        val result = mutableListOf<LintIssue>();
//        for (i in 0 until visitor.issues.size) {
//            val issue = visitor.issues[i];
//            if (i > 0) {
//                val prevIssue = result.last();
//                if(prevIssue.id != null && prevIssue.id == issue.id &&
//                    prevIssue.loc.substringBefore("/") == issue.loc.substringBefore("/"))
//                {
//                    // new issue to replace the old issue
//                    result[ result.size - 1 ] = issue;
//                    continue;
//                }
//            }
//
//            result.add(issue)
//        }
//
//        for (issue in result){
//            renderWarning(script, issue)
//        }
//
//        return result;
//    }
//
//    class JsonVisitor(val script: String, val builder: IslLintBuilder) : IJsonVisitor {
//        private val context = OperationContext();
//
//        val issues = mutableListOf<LintIssue>()
//
//        init {
//            context.registerExtensionMethod("Log.Info", builder::logInfo);
//            context.registerAnnotation("lint", builder::linterAnnotation)
//        }
//
//        override fun visited(token: IIslToken, json: JsonNode) {
//            //
//            builder.tests
//                .filter { it.instruction == token.type }
//                .forEach { lint ->
////                    println("visited ${token.type} - ${json}");
//                    // check if we have filters to apply
//                    if (lint.filter != null) {
//                        // check the filter
//                        val inner = json[token.type] as ObjectNode;
//                        var filterValid = true;
//                        lint.filter.fields().forEach { f ->
//                            val found = inner[f.key]?.textValue()
//                            val expecting = f.value?.textValue();
//                            if (found != null && found != expecting) {
//                                //println("    Filter ${f.key} ${found}!=${expecting}")
//                                filterValid = false;
//                                return@forEach; // no match
//                            }
//                        }
//                        if (!filterValid)
//                            return@forEach;
//                    }
//                    context.setVariable("\$s", json);
//                    val result = builder.linter.runTransformSync(lint.function, context);
//                    if (result != null && !result.isEmpty) {
//                        println(result.toPrettyString())
//                        val issue = JsonConvert.mapper.treeToValue<LintIssue>(result, LintIssue::class.java);
//                        issues.add(issue)
//                    }
//                }
//        }
//    }
//
//    private fun renderWarning(script: String, warning: LintIssue) {
//        val loc = warning.loc;
//        if (loc.isNullOrEmpty())
//            return;
//        // "L1:C12/L1:C13"
//        val parts = loc.split("/");
//        val l = parts[0].substringBefore(":").toInt() - 1;
//        val c = parts[0].substringAfter(":").toInt();
//        val el = parts[1].substringBefore(":").toInt() - 1;
//        val ec = parts[1].substringAfter(":").toInt();
//
//        // first start line
//        val lines = script.split("\n");
//        if (l < lines.size) {
//            if (l == el) {
//                println("Warning: ${warning.message} at: [${l + 1}:$c]")
//                println(lines[l])
//                val chars = "~".repeat(ec - c);
//                val start = " ".repeat(c);
//                println(start + chars);
//                println("Replace with: ${warning.recommendation}");
//            } else {
//                // single line error
//                println("Warning: ${warning.message} at: [${l + 1}:$c]-[${el + 1}:$ec]")
//                for (lineIndex in l..el) {
//                    val lt = lines[lineIndex];
//                    println(lt)
//                    if (lineIndex == l) {
//                        val start = " ".repeat(c);
//                        val chars = "~".repeat(lt.length - c - 1);
//                        println("$start^$chars");
//                    } else
//                        if (lineIndex == el) {
//                            val chars = "~".repeat(ec - 1);
//                            println("$chars^");
//                        } else {
//                            val chars = "~".repeat(lt.length);
//                            println(chars);
//                        }
//                }
//                println("Replace with: ${warning.recommendation}");
//            }
//        } else {
//            println("Warning: ${warning.message} at: [${l + 1}:$c]")
//            println("Replace with: ${warning.recommendation}");
//        }
//
//    }
//
//    // Nothing to do - we've already parsed the linter entry points
//    private suspend fun linterAnnotation(annotationExecuteContext: AnnotationExecuteContext): Any? {
//        return annotationExecuteContext.runNextCommand();
//    }
//
//
//    private fun logInfo(context: FunctionExecuteContext): Any? {
//        val first = context.firstParameter
//        val stringValue = ConvertUtils.tryToString(first) ?: "";
//
//        val args = context.parameters.drop(1).map { ConvertUtils.tryToString(it) };
//        println(stringValue.replace("\\n", "\n") + args?.joinToString { "," })
//
//        return null
//    }
//}
//
//data class LintIssue(
//    val type: String,
//    val token: String,
//    val loc: String,
//    val message: String,
//    val help: String? = null,
//    val recommendation: String? = null,
//    val id: String? = null,
//)