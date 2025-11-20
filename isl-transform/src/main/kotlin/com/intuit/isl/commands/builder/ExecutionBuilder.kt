package com.intuit.isl.commands.builder

import com.intuit.isl.commands.*
import com.intuit.isl.commands.modifiers.*
import com.intuit.isl.common.*
import com.intuit.isl.parser.tokens.*
import com.intuit.isl.runtime.ITransformer
import com.intuit.isl.runtime.TransformCompilationException
import com.intuit.isl.runtime.TransformModule
import com.intuit.isl.runtime.Transformer
import com.intuit.isl.utils.Position
import com.intuit.isl.utils.justOne
import com.intuit.isl.commands.modifiers.ConditionModifierValueCommand
import com.intuit.isl.commands.modifiers.MapModifierValueCommand
import com.intuit.isl.commands.modifiers.ReduceModifierValueCommand
import com.intuit.isl.utils.parseSimpleJsonPath
import java.util.*

/**
 * Step 2: Take a Token Graph and build a Command Graph.
 * Execution builder takes a root token and builds an execution graph out of a combination of commands.
 * Execution builder can also apply static optimizations at this level.
 */
class ExecutionBuilder(
    private val moduleName: String,
    private val rootToken: ModuleImplementationToken,
    private val buildDecorator: ICommandVisitor<IIslCommand>? = null,
    private val moduleFinder: ((name: String) -> ITransformer?)? = null,

    /* Custom Binded Operation Context for Hardwiring of function calls */
    private val operationContext: IOperationContext? = null
) : IIslTokenVisitor<IIslCommand> {

    // Used for hard-wiring all internal methods & extensions
    companion object {
    }

    fun build(): TransformModule {
        val module = visit(rootToken);

        // TODO: optimize graph
        // prop1.prop2.prop3: value1
        // prop1.prop2.prop4: value2 should be bundles into one bigger statement
        // prop1: { prop2: { prop3: value1, prop4: value2 } } }
        // this means we need to unwrap and bundle multiple lines together into a single object
        // one of the many optimizations we can nicely do

        // TODO: Hardwired cross function calls @.This.Method ()
        return module;
    }

    fun buildExpression(token: IIslToken): IIslCommand {
        when (token) {
            is StringInterpolateToken -> {
                val expression = visit(token) as InterpolateCommand;
                return expression.trySimplify();
            }

            is VariableSelectorValueToken -> {
                val expression = visit(token);
                return expression;
            }

            is SimpleVariableSelectorValueToken -> {
                val expression = visit(token);
                return expression;
            }

            is SimplePropertySelectorValueToken -> {
                val expression = visit(token);
                return expression;
            }

            else -> throw TransformCompilationException(
                "Could not understand expression. Unknown token type.",
                Position("expresion.isl", 0, 0)
            );
        }
    }

    // Decorating at build time seems to be the easiest approach - even if we need a visitor for most commands
    // We could do it reflectively (introspection) but that can be tricky for loops or conditions
    // where ordering of operations matter
    private fun decorate(command: IIslCommand, vararg children: IIslCommand?): IIslCommand {
        val result = if (buildDecorator != null) command.visit(buildDecorator) else command;
        children.forEach {
            it?.parent = result;
        }
        return result;
    }

    //    private fun decorateNull(command: IXCommand?, vararg children: IXCommand) : IXCommand? {
//        val result = if (buildDecorator != null)  command?.visit(buildDecorator) else command;
//        children.forEach {
//            it.parent = result;
//        }
//        return result;
//    }
    override fun visit(token: AssignPropertyToken): IIslCommand {
        val value = token.value.visit(this);

        // TODO: Unwrap deep properties into object assignments
        // prop1.prop2.prop3: value should become
        // prop1: { prop2: { prop3: value } } }
        // this means we need to unwrap and bundle multiple lines together into a single object
        // one of the many optimizations we can nicely do
        return decorate(AssignPropertyCommand(token, value), value);
    }

    override fun visit(token: AssignDynamicPropertyToken): IIslCommand {
        val name = token.name.visit(this);
        val value = token.value.visit(this);

        return decorate(AssignDynamicPropertyCommand(token, name, value), value);
    }

    override fun visit(token: AssignVariableToken): IIslCommand {
        val value = token.value.visit(this);

        return decorate(AssignVariableCommand(token, value), value);
    }

    override fun visit(token: FunctionCallToken): IIslCommand {
        val arguments = token.arguments.map { it.visit(this) };

        val statements = token.statements?.visit(this)

        // Optimization: we might be able to hardwired this function if it's coming out of one of our imports
        // we use ":" internally to separate the filename from the function name
        val existingMethod = findMethod(token.name.replace(".", ":"));

        // due to an odd bug in the design of the language we could have the following scenario
        // @.Method.Name( .. ) { a: true } - we declare an object immediately after a function
        // If the Method.Name is a Statement Function than this makes sense as it's a list of statements
        // but if Method.Name is just a normal function then the {...} is an object definition, like an object
        // used as a return from a foreach or function block
        return if (existingMethod != null) {
            if (statements != null) {
                throw TransformCompilationException(
                    "Object Block {...} that looks like a StatementFunction detected. Use ';' to separate the object declaration from the function=@.${token.name}.",
                    token.statements.position
                );
            }
            decorate(HardwiredFunctionCallCommand(token, arguments, existingMethod));
        } else {
            if (statements != null)
                decorate(StatementFunctionCallCommand(token, arguments, statements));
            else {
                decorate(FunctionCallCommand(token, arguments));
            }
        }
    }

//    override fun visit(token: WithLockToken): IIslCommand {
//        val statements = when (token.statements) {
//            is StatementsToken -> {
//                val commands = token.statements.map { it.visit(this) };
//                decorate(
//                    StatementsBuildCommand(StatementsToken(token.statements, token.position), commands),
//                    children = commands.toTypedArray()
//                );
//            }
//
//            else -> token.statements.visit(this);
//        }
//
//        return decorate(WithLockCommand(token, statements));
//    }

    override fun visit(token: ParallelForEachToken): IIslCommand {
        val source = token.source.visit(this);
        val options = token.options?.visit(this);
        // take care here - we might have statements as StatementToken or as ObjectToken
        // in most scenarios they are identical but inside a ForEach they have different behaviours
        val statements = when (token.statements) {
            is StatementsToken -> {
                val commands = token.statements.map { it.visit(this) };
                decorate(
                    StatementsBuildCommand(StatementsToken(token.statements, token.position), commands),
                    children = commands.toTypedArray()
                );
            }

            else -> token.statements.visit(this);
        }

        return decorate(ParallelForEachCommand(token, options, source, statements));
    }

    override fun visit(token: ForEachToken): IIslCommand {
        val source = token.source.visit(this);
        // take care here - we might have statements as StatementToken or as ObjectToken
        // in most scenarios they are identical but inside a ForEach they have different behaviours
        val statements = when (token.statements) {
            is StatementsToken -> {
                val commands = token.statements.map { it.visit(this) };
                decorate(
                    StatementsBuildCommand(StatementsToken(token.statements, token.position), commands),
                    children = commands.toTypedArray()
                );
            }

            else -> token.statements.visit(this);
        }

        return decorate(ForEachCommand(token, source, statements));
    }

    override fun visit(token: LiteralValueToken): IIslCommand {
        return decorate(LiteralValueCommand(token));
    }

    override fun visit(token: ModifierValueToken): IIslCommand {
        val previousValue = token.previousToken.visit(this);
        val arguments = token.arguments.map { it.visit(this) };

        var modifierCommand: IIslCommand?;
        if (token is FilterModifierValueToken)
            modifierCommand = FilterModifierValueCommand(
                token,
                previousValue,
                token.condition.visit(this) as IEvaluableConditionCommand
            );
        else if (token is MapModifierValueToken)
            modifierCommand = MapModifierValueCommand(
                token,
                previousValue,
                token.argument.visit(this)
            );
        else if (token is ConditionModifierValueToken) {
            val trueResult = token.trueModifier.visit(this);
            modifierCommand = ConditionModifierValueCommand(
                token,
                previousValue,
                token.condition.visit(this) as IEvaluableConditionCommand,
                trueResult
            );
        } else if (token.name == "reduce") {
            modifierCommand = ReduceModifierValueCommand(
                token,
                previousValue,
                arguments.justOne(token, "Math Expression")
            )
        } else if (token is GenericConditionalModifierValueToken) {
            val expression = token.condition.visit(this) as IEvaluableConditionCommand;

            if (token.firstArgument != null) {
                val allArguments = listOf(token.firstArgument, *token.arguments.toTypedArray()).toList();

                val firstCommand = token.firstArgument.visit(this);
                val allArgumentCommands = arrayOf(firstCommand, *arguments.toTypedArray()).toList()

                // let's see if this can be hardwired so it's clearly a simple modifier
                val standardModifier = buildModifierCommand(
                    ModifierValueToken(
                        token.name,
                        token.previousToken,
                        allArguments,
                        token.position
                    ), previousValue, allArgumentCommands
                )
                if (standardModifier is HardwiredModifierValueCommand || token.firstArgument is LiteralValueToken)
                    modifierCommand = standardModifier;
                else {
                    // One more optimization here is to look if this is a native ISL conditional
                    // we should convert this to a HardwiredGenericConditionalModifierCommand but we don't have that yet
                    //val existingExtension = RootOperationContext.getConditionalExtension("modifier.to");


                    // this could potentially be either a | doStuff ( condition, arguments ) or | dostuff( allArguments )
                    modifierCommand = PotentialGenericConditionalModifierCommand(
                        token.name,
                        token,
                        previousValue,
                        expression,
                        arguments,

                        allArgumentCommands
                    );
                }
            } else {
                // no first argument, no point in debating
                modifierCommand =
                    GenericConditionalModifierCommand(token.name, token, previousValue, expression, arguments)
            }
        } else {
            modifierCommand = buildModifierCommand(token, previousValue, arguments)
        }

        return decorate(modifierCommand, previousValue);
    }

    private fun buildModifierCommand(
        token: ModifierValueToken,
        previousValue: IIslCommand,
        arguments: List<IIslCommand>
    ): IIslCommand {
        var command: IIslCommand? = null;
        if (token.name.contains(".")) {
            // be careful - we could have encode.url format or ImportedModule.doStuff format
            // we want to see if the first part is an imported module, and then we can hardwire the call
            val potentialImportedName = token.name.substringBefore(".");
            if (imports.contains(potentialImportedName)) {
                // we'll assume this is imported!
                val remainingName = token.name.substringAfter(".");
                val method = findMethod("$potentialImportedName:$remainingName");
                if (method != null) {
                    command = HardwiredModifierValueCommand(
                        token,
                        remainingName,
                        previousValue,
                        arguments,
                        method
                    );
                }
            } else {
                // not an import
                // assume it's a full modifier name already imported
                val nameToSearch = token.name.substringBefore(".") + ".*";
                val method = findMethod("Modifier:${nameToSearch}");
                if (method != null)
                    command = HardwiredModifierValueCommand(
                        token,
                        token.name,
                        previousValue,
                        arguments,
                        method
                    );
            }
        } else {
            var method = findMethod("Modifier:${token.name}");
            if (method == null && localFunctionExists("Modifier:${token.name}"))
                method = findMethod("this:${token.name}");
            if (method != null)
                command =
                    HardwiredModifierValueCommand(token, token.name, previousValue, arguments, method);
        }

        command = command ?: ModifierValueCommand(token, token.name, previousValue, arguments)

        return command;
    }

    override fun visit(token: VariableSelectorValueToken): IIslCommand {
        if (token.path.isNullOrBlank())
            return decorate(VariableSelectorValueCommand(token));
        else {
            val parts = parseSimpleJsonPath("$." + token.path);
            return if (parts != null) {
                if (parts.size == 1)
                    decorate(FastSingleVariableWithPathSelectorValueCommand(token));
                else
                    decorate(FastVariableWithPathSelectorValueCommand(token, parts))
            } else
                decorate(VariableWithPathSelectorValueCommand(token));
        }
    }

    override fun visit(token: SimpleVariableSelectorValueToken): IIslCommand {
        val condition = token.conditionSelector?.visit(this) as IEvaluableConditionCommand?;
        return decorate(VariableSimpleSelectorCommand(token, condition));
    }

    override fun visit(token: SimplePropertySelectorValueToken): IIslCommand {
        val previous = token.previousToken.visit(this);
        val condition = token.conditionSelector?.visit(this) as IEvaluableConditionCommand?;
        return decorate(VariablePropertySelectorCommand(token, previous, condition));
    }

    override fun visit(token: StatementsToken): IIslCommand {
        val commands = token.map { it.visit(this) };
        return decorate(
            ObjectBuildCommand(DeclareObjectToken(token, token.position), commands.toMutableList()),
            children = commands.toTypedArray()
        );
    }

    override fun visit(token: DeclareObjectToken): IIslCommand {
        val commands = token.statements.map { it.visit(this) };
        return decorate(
            ObjectBuildCommand(token, commands.toMutableList()),
            children = commands.toTypedArray()
        );
    }

    override fun visit(token: SpreadToken): IIslCommand {
        val variable = token.variable.visit(this);
        return SpreadCommand(token, variable);
    }

    override fun visit(token: ConditionToken): IIslCommand {
        val condition = token.expression.visit(this) as IEvaluableConditionCommand;
        val trueResult = token.trueResult.visit(this);
        val falseResult = token.falseResult?.visit(this);

        return decorate(
            ConditionCommand(token, condition, trueResult, falseResult),
            condition as IIslCommand,
            trueResult,
            falseResult
        );
    }

    // an actual condition
    override fun visit(token: ConditionExpressionToken): IIslCommand {
        val left = token.left.visit(this) as IEvaluableConditionCommand;
        if (token.right == null)
            return left as IIslCommand;

        val right = token.right.visit(this) as IEvaluableConditionCommand;
        return ConditionExpressionCommand(token, left, token.condition.lowercase(), right);
    }

    override fun visit(token: SimpleConditionToken): IIslCommand {
        val left = token.left.visit(this);
        val right = token.right?.visit(this);
        return SimpleConditionCommand(token, left, token.condition.lowercase(), right);
    }

    override fun visit(token: CoalesceToken): IIslCommand {
        val left = token.left.visit(this);
        val right = token.right.visit(this);
        return CoalesceCommand(token, left, right);
    }

    override fun visit(token: SwitchCaseToken): IIslCommand {
        val value = token.value.visit(this);
        val cases = token.cases.map {
            it.visit(this) as SwitchCaseCommand.SwitchCaseBranchCommand;
        }.toTypedArray();
        return SwitchCaseCommand(token, value, cases);
    }

    override fun visit(token: SwitchCaseBranchToken): IIslCommand {
        val right = token.value.visit(this);
        val result = token.statements.visit(this);
        return SwitchCaseCommand.SwitchCaseBranchCommand(token, token.condition.lowercase(), right, result);
    }

    override fun visit(token: DeclareArrayToken): IIslCommand {
        val resultValues = ArrayList<IIslCommand>();
        token.values.forEach {
            resultValues.add(it.visit(this));
        }
        return decorate(ArrayCommand(token, resultValues));
    }

    override fun visit(token: StringInterpolateToken): IIslCommand {
        val resultValues = ArrayList<IIslCommand>();
        token.forEach {
            resultValues.add(it.visit(this));
        }
        return decorate(InterpolateCommand(token, resultValues));
    }

    override fun visit(token: MathExpressionToken): IIslCommand {
        val left = token.left.visit(this);
        val right = token.right.visit(this);

        return decorate(MathExpressionCommand(token, left, right, token.operation));
    }

    // Functions & Modules
    override fun visit(token: AnnotationDeclarationToken): IIslCommand {
        // We don't directly visit the Annotations, we do it as part of visiting the parent function
        throw NotImplementedError();
    }

    private fun visitAnnotationDeclarationToken(
        tokens: List<AnnotationDeclarationToken>,
        statements: IIslCommand,
        function: FunctionDeclarationToken
    ): IIslCommand {
        var currentCommand = statements
        for (annotation in tokens.asReversed()) {
            val arguments = annotation.arguments.map { it.visit(this) };
            currentCommand = AnnotationCommand(annotation, arguments, currentCommand, function);
        }
        return currentCommand;
    }

    override fun visit(token: FunctionDeclarationToken): IIslCommand {

        var statements = token.statements.visit(this);

        // check if very last statement is a return - then change it so that it does not throw an exception
        if (statements is ObjectBuildCommand) {
            val lastCommand = statements.commands.lastOrNull();
            if (lastCommand is FunctionReturnCommand) {
                statements.commands = statements.commands.subList(0, statements.commands.size - 1);

                lastCommand.useReturnValue = true;

                if (statements.commands.size == 0) {
                    // just a return statement?
                    statements = lastCommand;
                } else {
                    statements = StatementsBuildCommand(token, listOf(statements, lastCommand));
                }
            }
        }

        // Critical - we want to wrap the statements in the FunctionReturnCommand that captures the FunctionReturnException
        statements = FunctionReturnCommandHandler(token, statements);

        // Add annotation to the command tree via nextCommand
        // The last annotation command has the statements command as the nextCommand
        // Return the first annotation command
        statements = visitAnnotationDeclarationToken(token.annotations, statements, token);

        return decorate(FunctionDeclarationCommand(token, statements));
    }

    override fun visit(token: FunctionReturnToken): IIslCommand {
        val value = token.value.visit(this);
        return decorate(FunctionReturnCommand(token, value));
    }

    private val imports = TreeMap<String, ITransformer>(String.CASE_INSENSITIVE_ORDER);

    override fun visit(token: ModuleImplementationToken): TransformModule {
        // Import all external modules first
        token.imports
            .map {
                val otherModule = moduleFinder?.invoke(it.sourceName)
                    ?: throw TransformCompilationException("Module $moduleName could not find required 'import ${it.name} from ${it.sourceName}'");
                imports[it.name] = otherModule;
            }

        // we need to do this recursively - as the FunctionCallCommand visitor above will try to hardwire all functions

        // make sure we have no duplicate function names
        val set = HashSet<String>();
        token.functions.forEach {
            if (set.contains(it.functionName.lowercase())) {
                throw TransformCompilationException(
                    "Module $moduleName contains more than one function or modifier with name '${it.functionName}'",
                    it.position
                );
            }

            set.add(it.functionName.lowercase());

            compileFunction(it.functionName);
        }

        return TransformModule(moduleName, compiledFunctions.values.toList(), token);
    }

    private val compiledFunctions = TreeMap<String, IFunctionDeclarationCommand>(String.CASE_INSENSITIVE_ORDER);
    private val compilationStack = TreeSet(String.CASE_INSENSITIVE_ORDER);

    private fun localFunctionExists(functionName: String): Boolean {
        return rootToken.functions.find { it.functionName.equals(functionName, true) } != null;
    }

    private fun compileFunction(functionName: String): IFunctionDeclarationCommand {
        // this is recursive to allow quick hardwiring as dependencies are found!
        val compiledFunction = compiledFunctions[functionName];
        if (compiledFunction != null)
            return compiledFunction;

        val function = rootToken.functions.find { it.functionName.equals(functionName, true) }
            ?: throw TransformCompilationException("Could not find Function $functionName in Module $moduleName");

        if (compilationStack.contains(functionName)) {
            return RecursiveFunctionDeclarationCommand(function, compiledFunctions);
//            throw TransformCompilationException(
//                "Circular Dependency in Module $moduleName functions: ${
//                    compilationStack.joinToString(
//                        " > "
//                    )
//                } > ${functionName}."
//            );
        }

        compilationStack.add(functionName);

        val compiled = visit(function) as IFunctionDeclarationCommand;
        compiledFunctions[functionName] = compiled;

        compilationStack.remove(function.functionName);
        return compiled;
    }

    override fun visit(token: ImportDeclarationToken): IIslCommand {
        throw NotImplementedError();
    }

    private fun findMethod(methodName: String): AsyncContextAwareExtensionMethod? {
        // some methods we can hardwire them - especially if they are coming from known imports
        val lower = methodName.lowercase();
        val fromModule = lower.substringBefore(":");
        val functionName = lower.substringAfter(":");

        // This import - hardwire or fail
        if (fromModule == "this") {
            val otherFunction = compileFunction(functionName);
            return otherFunction.getRunner();
        } else if (fromModule == "modifier") {
            // might be a local modifier
            val localModifier = rootToken.functions.find {
                it.functionName.equals(
                    functionName,
                    true
                ) && it.functionType == FunctionType.Modifier
            };
            if (localModifier != null) {
                // we found a local modifier - compile it
                val modifier = compileFunction(functionName);
                return modifier.getRunner();
            }
        }


        // external import
        val import = imports[fromModule];
        if (import != null) {
            val importedFunction = (import as Transformer).crossModuleExecuteFunction(functionName)
                ?: throw TransformCompilationException("Module $moduleName Could not find $functionName in imported module $fromModule");
            return importedFunction;
        }

        val existingExtension = RootOperationContext.getExtension("$fromModule.$functionName");
        if (existingExtension != null) {
            return existingExtension;
        }

        val hostExtension = operationContext?.getExtension("$fromModule.$functionName");
        if (hostExtension != null) {
            return hostExtension;
        }

        // How cool would be to be able to import some declaration files? like the .d.ts for TypeScript, so we can guarantee the methods exist?
        return null;
    }


    override fun visit(token: NoopToken): IIslCommand {
        return NoopCommand(token);
    }

    override fun visit(token: WhileToken): IIslCommand {
        val expression = token.condition.visit(this) as IEvaluableConditionCommand;

        val limit = token.maxLoops?.visit(this);

        val statements = when (token.statements) {
            is StatementsToken -> {
                val commands = token.statements.map { it.visit(this) };
                decorate(
                    StatementsBuildCommand(StatementsToken(token.statements, token.position), commands),
                    children = commands.toTypedArray()
                );
            }

            else -> token.statements.visit(this);
        }

        return decorate(WhileCommand(token, expression, limit, statements));
    }
}