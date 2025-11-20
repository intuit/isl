package com.intuit.isl.commands.builder

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.IEvaluableConditionCommand
import com.intuit.isl.commands.NoopToken
import com.intuit.isl.common.AsyncContextAwareExtensionMethod
import com.intuit.isl.dsl.node
import com.intuit.isl.parser.tokens.*
import com.intuit.isl.runtime.ITransformer
import com.intuit.isl.runtime.TransformCompilationException
import com.intuit.isl.runtime.TransformModule
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.utils.Position
import java.util.*

interface IJsonVisitor {
    fun visited(token: IIslToken, json: JsonNode);
}

/**
 * Step 2: Take a Token Graph and build a JSON that can be executed by the ISLJS Runtime.
 */
class IslJsBuilder(
    private val moduleName: String,
    private val rootToken: ModuleImplementationToken,
    // private val buildDecorator: ICommandVisitor<JsonNode>? = null,
    private val jsonVisitor: IJsonVisitor? = null,
    private val moduleFinder: ((name: String) -> ITransformer?)? = null,
) : IIslTokenVisitor<JsonNode> {
    fun build(): JsonNode {
        val module = export(rootToken);
        return module;
    }

//    fun buildExpression(token: IIslToken): JsonNode {
//        when (token) {
//            is StringInterpolateToken -> {
//                val expression = visit(token);
//                return expression;
//            }
//
//            is VariableSelectorValueToken -> {
//                val expression = visit(token);
//                return expression;
//            }
//
//            is SimpleVariableSelectorValueToken -> {
//                val expression = visit(token);
//                return expression;
//            }
//
//            is SimplePropertySelectorValueToken -> {
//                val expression = visit(token);
//                return expression;
//            }
//
//            else -> throw TransformCompilationException(
//                "Could not understand expression. Unknown token type.",
//                Position("expresion.isl", 0, 0)
//            );
//        }
//    }
//
//    // Decorating at build time seems to be the easiest approach - even if we need a visitor for most commands
//    // We could do it reflectively (introspection) but that can be tricky for loops or conditions
//    // where ordering of operations matter
//    private fun decorate(command: JsonNode, vararg children: JsonNode?): JsonNode {
////        val result = if (buildDecorator != null) command.visit(buildDecorator) else command;
////        children.forEach {
////            it?.parent = result;
////        }
//        return command;
//    }

    private fun visit(token: IIslToken, json: JsonNode): JsonNode {
        jsonVisitor?.visited(token, json);
        return json;
    }

    //    private fun decorateNull(command: IXCommand?, vararg children: IXCommand) : IXCommand? {
//        val result = if (buildDecorator != null)  command?.visit(buildDecorator) else command;
//        children.forEach {
//            it.parent = result;
//        }
//        return result;
//    }

    private fun notSupported(token: IIslToken): JsonNode {
        throw TransformCompilationException(
            "Token=[${token::class.simpleName}] ${token} is not supported by IslJS",
            token.position
        );
    }

    override fun visit(token: AssignPropertyToken): JsonNode {
        val value = token.value.visit(this);

        // TODO: Unwrap deep properties into object assignments
        // prop1.prop2.prop3: value should become
        // prop1: { prop2: { prop3: value } } }
        // this means we need to unwrap and bundle multiple lines together into a single object
        // one of the many optimizations we can nicely do
        val root = JsonNodeFactory.instance.objectNode();
        val assign = root.putObject("assignprop");
        assign.put("name", token.name)
        assign.set<JsonNode>("value", value);
        return visit(token, addLocation(root, token));
    }

    override fun visit(token: AssignDynamicPropertyToken): JsonNode {
        return notSupported(token);
    }

    override fun visit(token: AssignVariableToken): JsonNode {
        val value = token.value.visit(this);

        val root = JsonNodeFactory.instance.objectNode();
        val v = root.putObject("assignvar")
        v.put("name", token.name)
        v.set<JsonNode>("value", value);
        return visit(token, addLocation(root, token));
    }

    override fun visit(token: FunctionCallToken): JsonNode {
        val arguments = token.arguments.map { it.visit(this) };

        // val statements = token.statements?.visit(this)
        return visit(
            token, addLocation(
                node {
                    node("call") {
                        put("name", token.name)
                        array("args", arguments)
                        put("_loc", getLocation(token))
                    }
                }.node, token
            )
        );
    }


    override fun visit(token: ParallelForEachToken): JsonNode {
        return notSupported(token);
    }

//    override fun visit(token: WithLockToken): JsonNode {
//        return notSupported(token);
//    }

    override fun visit(token: ForEachToken): JsonNode {
        val source = token.source.visit(this);
        // take care here - we might have statements as StatementToken or as ObjectToken
        // in most scenarios they are identical but inside a ForEach they have different behaviours
//        val statements = when (token.statements) {
//            is StatementsToken -> {
//                val commands = token.statements.map { it.visit(this) };
//                decorate(
//                    StatementsBuildCommand(StatementsToken(token.statements, token.position), commands),
//                    children = commands.toTypedArray()
//                );
//            }
//            else -> token.statements.visit(this);
//        }

        return notSupported(token);
    }

    override fun visit(token: LiteralValueToken): JsonNode {
        return visit(token, addLocation(node {
            node("literal", JsonConvert.convert(token.value))
        }.node, token));
    }

    override fun visit(token: ModifierValueToken): JsonNode {
        val previousValue = token.previousToken.visit(this);
        val arguments = token.arguments.map { it.visit(this) };
        val modifierCommand =
            if (token is FilterModifierValueToken)
//                FilterModifierValueCommand(
//                    token,
//                    previousValue,
//                    token.condition.visit(this) as IEvaluableConditionCommand
//                );
                return notSupported(token)
            else if (token is MapModifierValueToken)
//                MapModifierValueCommand(
//                    token,
//                    previousValue,
//                    token.argument.visit(this)
//                );
                return notSupported(token)
            else if (token is ConditionModifierValueToken) {
                val condition = token.condition.visit(this) as ObjectNode;
                val trueResult = token.trueModifier.visit(this);
                val args = mutableListOf(previousValue);
                args.addAll(arguments)

                // todo - previousValue
                return visit(token, addLocation(node {
                    node("call_if") {
                        merge(condition)    // if
                        node("true", trueResult)
                        node("prev", previousValue)
                        array("args", args)
                    }
                }.node, token));
            } else if (token is GenericConditionalModifierValueToken) {
                // we might have a firstArgument that could be a condition
                // this is more complex to handle - as it's a runtime concern not a compilation concern
                // Look at how Execution Builder does it
                val firstArg = token.firstArgument?.visit(this);
                val args = mutableListOf(previousValue);
                if (firstArg != null)
                    args.add(firstArg);
                args.addAll(arguments);
                return visit(token, addLocation(node {
                    node(token.type) {
                        put("name", token.name)
                        array("args", args)
                        put("_loc", getLocation(token))
                    }
                }.node, token));
            } else {
                // prev value for modifiers is the first param
                val args = mutableListOf(previousValue);
                args.addAll(arguments);
                return visit(token, addLocation(node {
                    node(token.type) {
                        put("name", token.name)
                        array("args", args)
                        put("_loc", getLocation(token))
                    }
                }.node, token));
            }
//            } else    // custom handling for some more complex modifiers - maybe we can make some special list where
//                when (token.name) {   // TODO: Some error handling would be nice
//                    "reduce" -> ReduceModifierValueCommand(
//                        token,
//                        previousValue,
//                        arguments.justOne(token, "Math Expression")
//                    );
//                    else -> {
//                        var command: JsonNode? = null;
//                        if (token.name.contains(".")) {
//                            // be careful - we could have encode.url format or ImportedModule.doStuff format
//                            // we want to see if the first part is an imported module, and then we can hardwire the call
//                            val potentialImportedName = token.name.substringBefore(".");
//                            if (imports.contains(potentialImportedName)) {
//                                // we'll assume this is imported!
//                                val remainingName = token.name.substringAfter(".");
//                                val method = findMethod("$potentialImportedName:$remainingName", token.arguments);
//                                if (method != null) {
//                                    command = HardwiredModifierValueCommand(
//                                        token,
//                                        remainingName,
//                                        previousValue,
//                                        arguments,
//                                        method
//                                    );
//                                }
//                            } else {
//                                // not an import
//                                // assume it's a full modifier name already imported
//                                val nameToSearch = token.name.substringBefore(".") + ".*";
//                                val method = findMethod("Modifier:${nameToSearch}", token.arguments);
//                                if (method != null)
//                                    command = HardwiredModifierValueCommand(
//                                        token,
//                                        token.name,
//                                        previousValue,
//                                        arguments,
//                                        method
//                                    );
//                            }
//                        } else {
//                            var method = findMethod("Modifier:${token.name}", token.arguments);
//                            if (method == null && localFunctionExists("Modifier:${token.name}"))
//                                method = findMethod("this:${token.name}", token.arguments);
//                            if (method != null)
//                                command =
//                                    HardwiredModifierValueCommand(token, token.name, previousValue, arguments, method);
//                        }
//
//                        command = command ?: ModifierValueCommand(token, token.name, previousValue, arguments)
//
//                        command;
//                    };
//                }
//
//
//        // TODO: Optimization: we might be able to hardwire this modifier if it's coming out of one of our imports
//        // see above the function call optimization
//
//        return decorate(modifierCommand, previousValue);
        return notSupported(token);
    }

    override fun visit(token: VariableSelectorValueToken): JsonNode {
        if (token.path.isNullOrBlank())
            return visit(
                token, addLocation(
                    JsonNodeFactory.instance.objectNode()
                        .put(token.type, token.variableName), token
                )
            );
        else
            return visit(
                token, addLocation(
                    JsonNodeFactory.instance.objectNode()
                        .put(token.type, token.variableName + "." + token.path), token
                )
            );
    }

    override fun visit(token: SimpleVariableSelectorValueToken): JsonNode {
        val condition = token.conditionSelector?.visit(this) as IEvaluableConditionCommand?;
//        return decorate(VariableSimpleSelectorCommand(token, condition));
        return notSupported(token);
    }

    override fun visit(token: SimplePropertySelectorValueToken): JsonNode {
//        val previous = token.previousToken.visit(this);
//        val condition = token.conditionSelector?.visit(this) as IEvaluableConditionCommand?;
//        return decorate(VariablePropertySelectorCommand(token, previous, condition));
        //return JsonNodeFactory.instance.objectNode()
        return notSupported(token)
    }


    override fun visit(token: StatementsToken): JsonNode {
        val commands = token.map { it.visit(this) };
//        return decorate(
//            ObjectBuildCommand(DeclareObjectToken(token, token.position), commands.toMutableList()),
//            children = commands.toTypedArray()
//        );
        if (commands.size == 1)
            return commands[0];

        return visit(
            token, JsonNodeFactory.instance.arrayNode()
                .addAll(commands)
        );
    }

    override fun visit(token: DeclareObjectToken): JsonNode {
        val statements = token.statements.visit(this);
        val root = JsonNodeFactory.instance.objectNode();
        val obj = root.putObject("object");
        obj.set<JsonNode>("statements", statements);
        return visit(token, addLocation(root, token));
    }

    override fun visit(token: SpreadToken): JsonNode {
        //val variable = token.variable.visit(this);
        //return SpreadCommand(token, variable);
        return notSupported(token);
    }

    override fun visit(token: ConditionToken): JsonNode {
        val condition = token.expression.visit(this) as ObjectNode;
        val trueResult = token.trueResult.visit(this);
        val falseResult = token.falseResult?.visit(this);

        val root = JsonNodeFactory.instance.objectNode();
        val c = root.putObject("condition")
        c.setAll<ObjectNode>(condition)
        c.set<JsonNode>("true", trueResult);
        if (falseResult != null)
            c.set<JsonNode>("false", falseResult)
        return visit(token, addLocation(root, token));
    }

    // an actual condition
    override fun visit(token: ConditionExpressionToken): JsonNode {
        val left = token.left.visit(this);
//        if (token.right == null)
//            return left;

        val right = token.right?.visit(this);

        val root = JsonNodeFactory.instance.objectNode();
        val cond = root.putObject("if")
        cond.set<ObjectNode>("left", left);
        cond.put("op", token.condition.lowercase());

        if (right != null)
            cond.set<ObjectNode>("right", right);
        addLocation(cond, token)
        return visit(token, addLocation(root, token));
    }

    override fun visit(token: SimpleConditionToken): JsonNode {
        val left = token.left.visit(this);
        val right = token.right?.visit(this);

        val root = JsonNodeFactory.instance.objectNode();
        val cond = root.putObject("if")
        cond.set<ObjectNode>("left", left);
        cond.put("op", token.condition.lowercase());
        if (right != null)
            cond.set<ObjectNode>("right", right);
        return visit(token, addLocation(root, token));
    }

    override fun visit(token: CoalesceToken): JsonNode {
        val left = token.left.visit(this);
        val right = token.right.visit(this);

        return visit(token, addLocation(node {
            node("condition") {
                node("if") {
                    node("left", left)
                    put("op", "exists")
                    put("_loc", getLocation(token))
                }
                node("true", left)
                node("false", right)
            }
        }.node, token));
    }

    override fun visit(token: SwitchCaseToken): JsonNode {
//        val value = token.value.visit(this);
//        val cases = token.cases.map {
//            it.visit(this) as SwitchCaseCommand.SwitchCaseBranchCommand;
//        }.toTypedArray();
//        return SwitchCaseCommand(token, value, cases);
        return notSupported(token);

    }

    override fun visit(token: SwitchCaseBranchToken): JsonNode {
//        val right = token.value.visit(this);
//        val result = token.statements.visit(this);
//        return SwitchCaseCommand.SwitchCaseBranchCommand(token, token.condition.lowercase(), right, result);
        return notSupported(token);
    }

    override fun visit(token: DeclareArrayToken): JsonNode {
        return visit(token, addLocation(node {
            array("array", token.values.map { it.visit(this@IslJsBuilder) })
        }.node, token));
//        val root = JsonNodeFactory.instance.objectNode()
//        val items = root.putArray("array");
//
//        token.values.forEach {
//            items.add(it.visit(this));
//        }
//        return root;
    }

    override fun visit(token: StringInterpolateToken): JsonNode {
        return visit(token, addLocation(node {
            array("interpolate", token.map { it.visit(this@IslJsBuilder) })
        }.node, token));
    }

    override fun visit(token: MathExpressionToken): JsonNode {
//        val left = token.left.visit(this);
//        val right = token.right.visit(this);
//
//        return decorate(MathExpressionCommand(token, left, right, token.operation));
        return notSupported(token);
    }

    // Functions & Modules
    override fun visit(token: AnnotationDeclarationToken): JsonNode {
        // We don't directly visit the Annotations, we do it as part of visiting the parent function
        throw NotImplementedError();
    }

    private fun visitAnnotationDeclarationToken(
        tokens: List<AnnotationDeclarationToken>,
        statements: JsonNode,
        function: FunctionDeclarationToken
    ): JsonNode {
//        var currentCommand = statements
//        for (annotation in tokens.asReversed()) {
//            val arguments = annotation.arguments.map { it.visit(this) };
//            currentCommand = AnnotationCommand(annotation, arguments, currentCommand, function);
//        }
//        return currentCommand;
        throw NotImplementedError();
    }

    override fun visit(token: FunctionDeclarationToken): JsonNode {
        val statements = token.statements.visit(this);
        val root = JsonNodeFactory.instance.objectNode();
        root.putObject("function")
        root.set<JsonNode>("statements", statements);
        return visit(token, addLocation(root, token));

        //return notSupported(token);

        //
//        return JsonNodeFactory.instance.objectNode();
        // check if very last statement is a return - then change it so that it does not throw an exception
//        if (statements is ObjectBuildCommand) {
//            val lastCommand = statements.commands.lastOrNull();
//            if (lastCommand is FunctionReturnCommand) {
//                statements.commands = statements.commands.subList(0, statements.commands.size - 1);
//
//                lastCommand.useReturnValue = true;
//
//                if (statements.commands.size == 0) {
//                    // just a return statement?
//                    statements = lastCommand;
//                } else {
//                    statements = StatementsBuildCommand(token, listOf(statements, lastCommand));
//                }
//            }
//        }
//
//        // Critical - we want to wrap the statements in the FunctionReturnCommand that captures the FunctionReturnException
//        statements = FunctionReturnCommandHandler(token, statements);
//
//        // Add annotation to the command tree via nextCommand
//        // The last annotation command has the statements command as the nextCommand
//        // Return the first annotation command
//        statements = visitAnnotationDeclarationToken(token.annotations, statements, token);

//        return decorate(FunctionDeclarationCommand(token, statements));
    }

    override fun visit(token: FunctionReturnToken): JsonNode {
        val value = token.value.visit(this);
        return visit(token, addLocation(node {
            node("return") {
                node("value", value)
            }
        }.node, token));
    }

    fun export(token: ModuleImplementationToken): JsonNode {
        if (token.imports.isNotEmpty())
            throw TransformCompilationException("Module Imports are not supposed by IslJS")

        // make sure we have no duplicate function names
        val result = JsonNodeFactory.instance.objectNode()
            .put("moduleName", moduleName);

        val functions = result.putObject("fun");

        val set = HashSet<String>();
        token.functions.forEach {
            if (set.contains(it.functionName.lowercase())) {
                throw TransformCompilationException(
                    "Module $moduleName contains more than one function or modifier with name '${it.functionName}'",
                    it.position
                );
            }

            set.add(it.functionName.lowercase());

            val function = compileFunction(it.functionName);
            visit(token, function);
            functions.set<JsonNode>(it.functionName, function)
        }

        return result;
    }

    override fun visit(token: ModuleImplementationToken): TransformModule {
        throw NotImplementedError("Use Export method");
    }

    private val compiledFunctions = TreeMap<String, JsonNode>(String.CASE_INSENSITIVE_ORDER);
    private val compilationStack = TreeSet(String.CASE_INSENSITIVE_ORDER);

    private fun localFunctionExists(functionName: String): Boolean {
        return rootToken.functions.find { it.functionName.equals(functionName, true) } != null;
    }

    private fun compileFunction(functionName: String): JsonNode {
        // this is recursive to allow quick hardwiring as dependencies are found!
        val compiledFunction = compiledFunctions[functionName];
        if (compiledFunction != null)
            return compiledFunction;

        if (compilationStack.contains(functionName))
            throw TransformCompilationException(
                "Circular Dependency in Module $moduleName functions: ${
                    compilationStack.joinToString(
                        " > "
                    )
                } > ${functionName}."
            );

        val function = rootToken.functions.find { it.functionName.equals(functionName, true) }
            ?: throw TransformCompilationException("Could not find Function $functionName in Module $moduleName");
        compilationStack.add(functionName);

        val compiled = visit(function) as JsonNode;
        compiledFunctions[functionName] = compiled;

        compilationStack.remove(function.functionName);
        return compiled;
    }

    override fun visit(token: ImportDeclarationToken): JsonNode {
        return notSupported(token);
    }

    private fun findMethod(methodName: String, arguments: List<IIslToken>): AsyncContextAwareExtensionMethod? {
//        // some methods we can hardwire them - especially if they are coming from known imports
//        val lower = methodName.lowercase();
//        val fromModule = lower.substringBefore(":");
//        val functionName = lower.substringAfter(":");
//
//        // This import - hardwire or fail
//        if (fromModule == "this") {
//            val otherFunction = compileFunction(functionName);
//            return otherFunction.getRunner();
//        } else if (fromModule == "modifier"){
//            // might be a local modifier
//            val localModifier = rootToken.functions.find { it.functionName.equals(functionName, true) && it.functionType == FunctionType.Modifier };
//            if(localModifier!=null){
//                // we found a local modifier - compile it
//                val modifier = compileFunction(functionName);
//                return modifier.getRunner();
//            }
//        }
//
//
//        // external import
//        val import = imports[fromModule];
//        if (import != null) {
//            val importedFunction = (import as Transformer).crossModuleExecuteFunction(functionName)
//                ?: throw TransformCompilationException("Module $moduleName Could not find $functionName in imported module $fromModule");
//            return importedFunction;
//        }
//
//        val existingExtension = RootOperationContext.getExtension("$fromModule.$functionName");
//        if (existingExtension != null) {
//            return existingExtension;
//        }

        // How cool would be to be able to import some declaration files? like the .d.ts for TypeScript, so we can guarantee the methods exist?
        return null;
    }


    override fun visit(token: NoopToken): JsonNode {
        return notSupported(token);
    }

    override fun visit(token: WhileToken): JsonNode {
//        val expression = token.condition.visit(this) as IEvaluableConditionCommand;
//
//        val limit = token.maxLoops?.visit(this);
//
//        val statements = when (token.statements) {
//            is StatementsToken -> {
//                val commands = token.statements.map { it.visit(this) };
//                decorate(
//                    StatementsBuildCommand(StatementsToken(token.statements, token.position), commands),
//                    children = commands.toTypedArray()
//                );
//            }
//            else -> token.statements.visit(this);
//        }
//
//        return decorate(WhileCommand(token, expression, limit, statements));
        return notSupported(token);
    }

    fun addLocation(node: ObjectNode, token: IIslToken): JsonNode {
        // we can disable this if not required
        node.put("_loc", getLocation(token))
//        node.set<ObjectNode>("_token", node {
//            //put("file", token.position.file)
//            put("l", token.position.line.toLong())
//            put("c", token.position.column.toLong())
//            put("el", token.position.endLine?.toLong())
//            put("ec", token.position.endColumn?.toLong())
//        }.node)
        return node;
    }

    fun getLocation(token: IIslToken): String {
        return "${token.position.line}:${token.position.column}/${token.position.endLine}:${token.position.endColumn}";
    }
}