package com.intuit.isl.parser

import com.intuit.isl.commands.ConditionEvaluator
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.commands.NoopCommand
import com.intuit.isl.commands.NoopToken
import com.intuit.isl.commands.StatementsBuildCommand
import com.intuit.isl.antlr.IslLexer
import com.intuit.isl.antlr.IslParser
import com.intuit.isl.antlr.IslParser.ArgumentValueContext
import com.intuit.isl.antlr.IslParserBaseVisitor
import com.intuit.isl.parser.tokens.*
import com.intuit.isl.runtime.TransformCompilationException
import com.intuit.isl.types.*
import com.intuit.isl.utils.ConvertUtils
import com.intuit.isl.utils.Position
import com.intuit.isl.utils.fixString
import com.intuit.isl.utils.removeQuotes
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.io.PrintStream
import java.math.BigDecimal

/**
 * Step 1: Parse the Graph and build a Token Graph
 * Visitor parses the string ISL and returns a graph of IIslToken wired all together.
 */
class IslScriptVisitor(private val moduleName: String, val contents: String, private val output: PrintStream) :
    IslParserBaseVisitor<Any?>() {
    private val types = mutableMapOf<String, IslType>();
    private val compilationWarnings = mutableListOf<CompilationWarning>();

    // Warnings & Optimizations best calculated during parsing
    //private var canInline = true;

    private var defaultFunctionName: String = "run";
    private var startLine = 0;
    private var startCol = 0;

    /**
     * The Root IXToken (the complete Spec)
     */
    fun parseIsl(): ModuleImplementationToken {
        val inputStream: CharStream = CharStreams.fromString(contents)
        val lexer = IslLexer(inputStream)
        val commonTokenStream = CommonTokenStream(lexer)
        val parser = IslParser(commonTokenStream)

        // Performance: Use SLL prediction mode for faster parsing
        // Fall back to LL mode if parsing fails
        parser.interpreter.predictionMode = PredictionMode.SLL
        parser.removeErrorListeners()  // Remove default error listener that throws exceptions
        parser.errorHandler = BailErrorStrategy()  // Use bail strategy to throw ParseCancellationException

        try {
            return visit(parser.spec()) as ModuleImplementationToken
        } catch (e: ParseCancellationException) {
            // SLL mode failed, fall back to LL mode for ambiguous grammars
            commonTokenStream.seek(0)
            parser.reset()
            parser.interpreter.predictionMode = PredictionMode.LL
            parser.errorHandler = DefaultErrorStrategy()  // Restore default error handling
            parser.addErrorListener(ParserErrorListener(moduleName, contents))
            
            try {
                return visit(parser.spec()) as ModuleImplementationToken
            } catch (e: InputMismatchException) {
                val m = "Invalid Token: `${e.offendingToken.text}`"
                val p = e.offendingToken.getPosition()
                throw TransformCompilationException(m, p, contents)
            }
        } catch (e: InputMismatchException) {
            // TODO: Convert these messages to some nicer friendlier errors
            val m = "Invalid Token: `${e.offendingToken.text}`"
            val p = e.offendingToken.getPosition()
            throw TransformCompilationException(m, p, contents)
        }
    }

    /**
     * Parse a String Interpolate Expression
     */
    fun parseExpression(): IIslToken {
        if (contents.startsWith("$") && !contents.startsWith("\${")) {
            // this might be just a variable - shortcut
            val inputStream: CharStream = CharStreams.fromString(contents);
            val lexer = IslLexer(inputStream)
            val commonTokenStream = CommonTokenStream(lexer)
            val parser = IslParser(commonTokenStream)

            parser.addErrorListener(ParserErrorListener(moduleName, contents));

            try {
                return visit(parser.variableSelector()) as IIslToken
            } catch (e: InputMismatchException) {
                // ignore and go and try to parse an interpolation
            }
        }
        // interpolation has `` - we need to wrap that
        val inputStream: CharStream = CharStreams.fromString("`$contents`");
        val lexer = IslLexer(inputStream)
        val commonTokenStream = CommonTokenStream(lexer)
        val parser = IslParser(commonTokenStream)

        parser.addErrorListener(ParserErrorListener(moduleName, contents));

        try {
            return visit(parser.interpolate()) as IIslToken
        } catch (e: InputMismatchException) {
            // TODO: Convert these messages to some nicer friendlier errors
            val m = "Invalid Token: `${e.offendingToken.text}`";
            val p = e.offendingToken.getPosition();
            throw TransformCompilationException(m, p, contents);
        }
    }


    override fun aggregateResult(aggregate: Any?, nextResult: Any?): Any? {
        // This protects from silly nulls when next is optional (e.g. SEMICOLON)
        // but we need to review this is we get both values valid
        return nextResult ?: aggregate
    }

    /**
     * Right-hand side that can be assigned to something else
     * Math, objects, inlineifs, foreach, literals, arrays, interpolate, functions, variables - anything pretty much
     */
    override fun visitAssignmentValue(ctx: IslParser.AssignmentValueContext): Any {
        if (ctx.assignmentValueItem() != null)
            return visitAssignmentValueItem(ctx.assignmentValueItem());

        val left = visit(ctx.assignmentValue(0)) as IIslToken;
        val right = visit(ctx.assignmentValue(1)) as IIslToken;

        return CoalesceToken(left, right, ctx.getPosition());
    }

    override fun visitAssignmentValueItem(ctx: IslParser.AssignmentValueItemContext): Any {
        val value: IIslToken = when {
            ctx.math() != null -> visit(ctx.math()) as IIslToken
            ctx.declareObject() != null -> visit(ctx.declareObject()) as IIslToken
            ctx.inlineIf() != null -> visit(ctx.inlineIf()) as IIslToken
            ctx.forEach() != null -> visit(ctx.forEach()) as IIslToken
            ctx.whileLoop() != null -> visit(ctx.whileLoop()) as IIslToken
            ctx.literal() != null -> visit(ctx.literal()) as IIslToken
            ctx.array() != null -> visit(ctx.array()) as IIslToken
            ctx.interpolate() != null -> visit(ctx.interpolate()) as IIslToken
            ctx.functionCall() != null -> visit(ctx.functionCall()) as IIslToken
            ctx.switchCaseStatement() != null -> visit(ctx.switchCaseStatement()) as IIslToken
            ctx.variableSelector() != null -> visitVariableSelector(ctx.variableSelector())
            else -> throw TransformCompilationException("Unknown Assign Token ${ctx.text}", ctx.getPosition(), contents)
        };

        // modifiers
        val modifiedToken = visitModifiers(value, ctx.modifier());

        return modifiedToken;
    }

    override fun visitAssignSelector(ctx: IslParser.AssignSelectorContext): Any? {
        return super.visitAssignSelector(ctx)
    }

    // abc.def: value
    // abc.def: $value.property
    override fun visitAssignProperty(ctx: IslParser.AssignPropertyContext): Any {
        if (ctx.exception != null)
            throw ctx.exception;

        val value = visitAssignmentValue(ctx.assignmentValue()) as IIslToken;

        val islType = resolveIslType(ctx.objectType?.text);
        if (islType != null)
            value.islType = islType;

        // We can have either an assignSelector (fixed property name)
        // or interpolation (dynamic property name)
        if (ctx.assignSelector() != null) {
            // We might have to explode the property identifier
            // prop1.prop2.prop3 should become { prop1: { prop2: { prop3: ... } } }
            val propTokens = ctx.assignSelector().text.split('.');

            if (propTokens.size == 1) {
                return AssignPropertyToken(
                    ParserUtils.cleanPropertyName(ctx.assignSelector().text),
                    value,
                    islType,
                    ctx.getPosition()
                );
            } else {
                // TODO: Fake new tokens and Positions
                var childToken = AssignPropertyToken(
                    ParserUtils.cleanPropertyName(propTokens.last()),
                    value,
                    null,
                    ctx.getPosition()
                ) as IIslToken;

                var root: IIslToken? = null;
                propTokens
                    .asReversed()   // start end the end
                    .subList(1, propTokens.size)  // skip first (that was the child from above)
                    .forEach {
                        val childObject = DeclareObjectToken(
                            StatementsToken(listOf(childToken), ctx.getPosition()),
                            ctx.getPosition()
                        ) as IIslToken;
                        root = AssignPropertyToken(
                            ParserUtils.cleanPropertyName(it),
                            childObject,
                            null,
                            ctx.getPosition()
                        );
                        childToken = root as IIslToken;
                    };

                root?.islType = islType;
                return root as IIslToken;
            }
        } else if (ctx.interpolate() != null) {
            val dynamicName = visitInterpolate(ctx.interpolate());
            return AssignDynamicPropertyToken(
                dynamicName,
                value,
                islType,
                ctx.getPosition()
            );
        } else {
            throw TransformCompilationException("Unknown Property Name Type='${ctx.text}'.", ctx.getPosition());
        }
    }

    override fun visitAssignVariableProperty(ctx: IslParser.AssignVariablePropertyContext): Any {
        if (ctx.exception != null)
            throw ctx.exception;

        val position = ctx.getPosition();
        val value = visitAssignmentValue(ctx.assignmentValue()) as IIslToken;
        val fullName = ctx.assignSelector().text;

        val islType = resolveIslType(ctx.objectType?.text);
        if (islType != null)
            value.islType = islType;

        return ParserUtils.generateAssignVariable(fullName, value, islType, position);
    }

    private fun resolveIslType(typeName: String?): IslType? {
        return if (typeName.isNullOrBlank())
            null
        else
            IslObjectType(typeName);
    }

//    override fun visitIdentifierPart(ctx: IslParser.IdentifierPartContext): String {
//        return ctx.ID().text;//.joinToString(".");
//    }

    // abc: { prop1: value1, prop2: value2 }
    override fun visitDeclareObject(ctx: IslParser.DeclareObjectContext): Any {
        if (ctx.exception != null)
            throw ctx.exception;

        val list = ArrayList<IIslToken>();
        if (ctx.declareObjectStatement() != null) {
            for (s in ctx.declareObjectStatement()) {
                val result = visit(s) as IIslToken
                list.add(result)
            }
        }
        val statements = StatementsToken(list, ctx.getPosition());
        return DeclareObjectToken(statements, ctx.getPosition());
    }

    override fun visitSpreadSelector(ctx: IslParser.SpreadSelectorContext): Any? {
        val variable =
            if (ctx.variableSelector() != null)
                visit(ctx.variableSelector()) as IIslToken;
            else
                visit(ctx.functionCall()) as IIslToken;
        return SpreadToken(variable, ctx.getPosition());
    }

    override fun visitAssignTextProperty(ctx: IslParser.AssignTextPropertyContext): Any? {
        if (ctx.exception != null)
            throw ctx.exception;

        val value = visitAssignmentValue(ctx.assignmentValue()) as IIslToken;

        val islType = resolveIslType(ctx.objectType?.text);
        if (islType != null)
            value.islType = islType;

        return AssignPropertyToken(
            ParserUtils.cleanPropertyName(ctx.QUOTEDSTRING().symbol.text.fixString()),
            value,
            islType,
            ctx.getPosition()
        );
    }

//    override fun visitParallelForEach(ctx: IslParser.ParallelForEachContext): Any? {
//        if (ctx.exception != null)
//            throw ctx.exception;
//
//        val iteratorName = ctx.iterator.text;
//
//
//        val collection = visit(ctx.rhsid()) as IIslToken;
//        val statements = if (ctx.statements() != null) visit(ctx.statements()) as StatementsToken
//        else null;
//
//        // these two are exclusive
//        val objectBuild = if (ctx.declareObject() != null) visit(ctx.declareObject()) as IIslToken else null;
//        val variableSelect = if (ctx.variableSelector() != null) visit(ctx.variableSelector()) as IIslToken else null;
//
//        // we have statements & an object or variable at the end
//        val token = if (statements != null && objectBuild != null) {
//            statements.add(objectBuild);
//            statements as IIslToken;
//        } else if (statements != null && variableSelect != null) {
//            statements.add(variableSelect);
//            statements as IIslToken;
//        } else statements ?: objectBuild ?: variableSelect ?: NoopToken();
//
//        return ParallelForEachToken(
//            iteratorName.lowercase(), options, collection,
//            token, ctx.getPosition()
//        );
//    }

//    override fun visitWithLock(ctx: IslParser.WithLockContext): Any? {
//        if (ctx.exception != null)
//            throw ctx.exception;
//
//        val statements = visit(
//            ctx.statements() ?: ctx.declareObject() ?: ctx.variableSelector()
//        ) as IIslToken;
//        return WithLockToken(statements, ctx.getPosition());
//    }

    override fun visitForEach(ctx: IslParser.ForEachContext): Any {
        if (ctx.exception != null)
            throw ctx.exception;

        val isParallel = ctx.text.lowercase().startsWith("parallel");
        val options = if (ctx.options?.variableSelector() != null) visit(ctx.options?.variableSelector()) as IIslToken
        else if (ctx.options?.declareObject() != null) visit(ctx.options.declareObject()) as IIslToken
        else null;

        val iteratorName = ctx.iterator.text;
        val collection = visit(ctx.rhsid()) as IIslToken;

        val statements = if (ctx.statements() != null) visit(ctx.statements()) as StatementsToken else null;
        // these two are exclusive
        val objectBuild = if (ctx.declareObject() != null) visit(ctx.declareObject()) as IIslToken else null;
        val variableSelect = if (ctx.variableSelector() != null) visit(ctx.variableSelector()) as IIslToken else null;

        val token = if (statements != null && objectBuild != null) {
            statements.add(objectBuild);
            statements as IIslToken;
        } else if (statements != null && variableSelect != null) {
            statements.add(variableSelect);
            statements as IIslToken;
        } else statements ?: objectBuild ?: variableSelect ?: NoopToken();

        return if (isParallel) {
            ParallelForEachToken(
                iteratorName.lowercase(), options, collection,
                token, ctx.getPosition()
            );
        } else {
            ForEachToken(
                iteratorName.lowercase(), collection,
                token, ctx.getPosition()
            )
        };
    }

    override fun visitWhileLoop(ctx: IslParser.WhileLoopContext): Any {
        if (ctx.exception != null)
            throw ctx.exception;

        val expression = visitConditionExpression(ctx.conditionExpression()) as IIslToken;

        val maxLoops = if (ctx.options != null) visit(ctx.options) as IIslToken else null;

        val statements =
            visit(ctx.statements() ?: ctx.bodyDeclareObject ?: ctx.variableDeclaration()) as IIslToken

        return WhileToken(expression, maxLoops, statements, ctx.getPosition());
    }

//    fun visitVariable(ctx: TerminalNode): IIslToken {
//        return VariableSelectorValueToken(
//            ctx.text,
//            ctx.symbol.getPosition()
//        )
//    }

    override fun visitFunctionCall(ctx: IslParser.FunctionCallContext): Any {
        val functionName = ctx.service.text + "." + (ctx.name?.text ?: "Get");

        val arguments = visitArguments(ctx.arguments());

        val statements =
            if (ctx.functionStatements() != null)
                visitFunctionStatements(ctx.functionStatements()) as IIslToken
            else
                null;

        return FunctionCallToken(functionName.lowercase(), arguments, statements, ctx.getPosition());
    }

    override fun visitFunctionStatements(ctx: IslParser.FunctionStatementsContext): Any {
        val list = ArrayList<IIslToken>()
        for (s in ctx.functionStatement()) {
            val result = visit(s) as IIslToken
            list.add(result)
        }
        return StatementsToken(list, ctx.getPosition());
    }

    override fun visitAnnotationParameter(ctx: IslParser.AnnotationParameterContext): Any {
        if (ctx.literal() != null)
            return visitLiteral(ctx.literal());
        else if (ctx.declareObject() != null)
            return visitDeclareObject(ctx.declareObject());
        else if (ctx.array() != null)
            return visitArray(ctx.array());

        throw TransformCompilationException("Unknown Annotation Parameter ${ctx.text}", ctx.getPosition());
    }

    override fun visitAnnotation(ctx: IslParser.AnnotationContext): Any {
        val name = ctx.ID().symbol.text;

        val arguments =
            ctx.annotationArguments()
                ?.annotationParameter()
                ?.map { visitAnnotationParameter(it) as IIslToken } ?: emptyList();

        return AnnotationDeclarationToken(name.lowercase(), arguments, ctx.getPosition());
    }

    override fun visitFunctionDeclaration(ctx: IslParser.FunctionDeclarationContext): Any {
        val functionName = ctx.ID().symbol.text;
        // parse any annotations
        var annotations = ctx.annotation()?.map { visitAnnotation(it) as AnnotationDeclarationToken } ?: emptyList();
        if (ctx.CACHE() != null) {
            annotations = annotations.plus(AnnotationDeclarationToken("cache", emptyList(), ctx.getPosition()))
        }

        // fun - functions, modifier - modifier
        // modifiers can be called via `| modifier( ... )` because why not
        val functionType = if (ctx.FUN() != null) FunctionType.Function else FunctionType.Modifier;
        // we only care about the argument names
        // TBD: If we want to add default values we need to improve this to read the default value for each argument
        val arguments = ctx
            .functionArguments()
            .variableWithType()
            .map {
                val name = it.variableDeclaration().text;
                val type = visitTypeDefinition(it.typeDefinition());
                JsonProperty(name.lowercase(), type)
            };

        val returnType = visitTypeDefinition(ctx.typeDefinition());

        val statements = ctx.functionStatements()
            ?.functionStatement()
            ?.map { visit(it) as IIslToken } ?: listOf();

        return FunctionDeclarationToken(
            functionType,
            functionName.lowercase(),
            annotations,
            arguments,
            StatementsToken(statements, ctx.getPosition()),
            returnType,
            ctx.getPosition()
        );
    }

    override fun visitTypeDefinition(ctx: IslParser.TypeDefinitionContext?): IslType {
        if (ctx == null)
            return IslType.Any;

        val result = when {
            ctx.typeNameDeclaration() != null -> {
                // we can only accept this is it's a known type!
                val type = IslType.Type.fromText(ctx.typeNameDeclaration().text);
                if (type != null)
                    return JsonBasicType(type);
                val id = ctx.typeNameDeclaration().text;
                if (id.isNotBlank()) {
                    val knowType = types[id] ?: IslObjectType(id);
                    return knowType;
                }
                return IslType.Any;
            }

            ctx.objectTypeDefinition() != null -> visitObjectTypeDefinition(ctx.objectTypeDefinition());
            ctx.arrayTypeDefinition() != null -> {
                val arrayDefinition = ctx.arrayTypeDefinition();
                val arrayType = when {
                    arrayDefinition.ID() != null -> {
                        // we can only accept this is it's a known type!
                        val type = IslType.Type.fromText(arrayDefinition.ID().text);
                        if (type != null) {
                            JsonBasicType(type);
                        } else {
                            val id = arrayDefinition.ID().text;
                            types.getOrDefault(id, IslType.Any);
                        }
                    }

                    arrayDefinition.objectTypeDefinition() != null -> visitObjectTypeDefinition(arrayDefinition.objectTypeDefinition());
                    else -> IslType.Any;
                }
                return JsonArrayType(arrayType);
            }

            ctx.enumTypeDefinition() != null -> {
                val items = ctx.enumTypeDefinition().literal()
                    .map { visitLiteral(it).value }
                    .toList();

                return JsonBasicType(IslType.Type.STRING, items);
            }

            else -> IslType.Any
        }

        return result;
    }

    override fun visitObjectTypeDefinition(ctx: IslParser.ObjectTypeDefinitionContext): IslType {
        val properties = ctx
            .declareObjectTypeProperty()
            .map {
                val name = it.shortIdentifier().text;
                val type = visitTypeDefinition(it.typeDefinition());
                JsonProperty(name, type);
            }
        return IslObjectType("", properties)
    }

    override fun visitTypeDeclaration(ctx: IslParser.TypeDeclarationContext): Pair<String, IslType> {
        val name = ctx.ID().symbol.text;

        when {
            ctx.typeDefinition() != null -> {
                val type = visitTypeDefinition(ctx.typeDefinition());
                return name to type;
            }

            ctx.QUOTEDSTRING() != null -> {
                return name to JsonReferenceType(ctx.QUOTEDSTRING().symbol.text);
            }
        }

        throw TransformCompilationException(
            "Could not understand type declaration: ${ctx.text}",
            ctx.getPosition(),
            contents
        );
    }


    override fun visitReturnCall(ctx: IslParser.ReturnCallContext): Any {
        // a function return token "return {something}"
        val value = visit(ctx.assignmentValue()) as IIslToken;
        return FunctionReturnToken(value, ctx.getPosition());
    }

    // right hand side identifier
    override fun visitRhsid(ctx: IslParser.RhsidContext): Any {
        val token = super.visit(ctx.rightSideValue()) as IIslToken

        // apply the modifiers
        val modifiedToken = visitModifiers(token, ctx.modifier());

        return modifiedToken;
    }

    private fun visitModifiers(token: IIslToken, modifiers: List<IslParser.ModifierContext>): IIslToken {
        var currentToken = token;
        for (modifier in modifiers) {
            // TODO: We might consider grouping modifiers by the type of their expressions?
            if (modifier.filterModifier() != null) {
                val expression =
                    visitConditionExpression(modifier.filterModifier().condition().conditionExpression()) as IIslToken;
                currentToken = FilterModifierValueToken(currentToken, expression, modifier.getPosition());
            } else if (modifier.mapModifier() != null) {
                val value =
                    visitArgumentValue(modifier.mapModifier().argumentValue()) as IIslToken;
                currentToken = MapModifierValueToken(currentToken, value, modifier.getPosition());
            } else if (modifier.conditionModifier() != null) {
                val condition = modifier.conditionModifier();
                val expression = visitConditionExpression(condition.condition().conditionExpression()) as IIslToken;

                val arguments = visitArguments(condition.arguments());
                val trueModifier =
                    ModifierValueToken(
                        condition.multiIdent().text,
                        currentToken,
                        arguments,
                        condition.getPosition()
                    );

                currentToken =
                    ConditionModifierValueToken(currentToken, expression, trueModifier, condition.getPosition());
            } else if (modifier.genericConditionModifier() != null) {
                val condition = modifier.genericConditionModifier().conditionExpression();
                val expression = visitConditionExpression(condition) as IIslToken;

                val arguments = visitArguments(modifier.genericConditionModifier().argumentValue());

                // WARNING WARNING: GenericConditionModifier CAN pickup also standard modifiers due to overlap of the grammar
                // | dostuff ( $value ) can be both a condition modifier and a standard modifier
                // if we detect that we'll create the token with both expression and argument and let the command decide later
                val firstArgument =
                    if (expression is SimpleConditionToken && expression.condition == ConditionEvaluator.EXISTS) {
                        expression.left
                    } else {
                        null
                    }

                currentToken = GenericConditionalModifierValueToken(
                    modifier.genericConditionModifier().multiIdent().text,
                    currentToken, expression, firstArgument, arguments, modifier.getPosition()
                )
            } else {
                val arguments = visitArguments(modifier.arguments());
                currentToken =
                    ModifierValueToken(
                        modifier.multiIdent().text,
                        currentToken,
                        arguments,
                        modifier.getPosition()
                    );
            }
        }
        return currentToken
    }

    override fun visitRhsval(ctx: IslParser.RhsvalContext?): Any? {
        return super.visitRhsval(ctx)
    }

    // trim( chars to trim )
    override fun visitArguments(ctx: IslParser.ArgumentsContext?): ArrayList<IIslToken> {
        val result = ArrayList<IIslToken>()
        if (ctx != null) {
            return visitArguments(ctx.argumentValue());
        }
        return result
    }

    private fun visitArguments(ctx: List<ArgumentValueContext>?): ArrayList<IIslToken> {
        val result = ArrayList<IIslToken>()
        if (ctx != null) {
            for (value in ctx) {
                val token = visitArgumentValue(value) as IIslToken
                result.add(token)
            }
        }
        return result
    }


    override fun visitArgumentValue(ctx: IslParser.ArgumentValueContext): Any {
        if (ctx.argumentItem() != null)
            return visitArgumentItem(ctx.argumentItem());
        // we have a coalesce operation
        val left = visit(ctx.argumentValue(0)) as IIslToken;
        val right = visit(ctx.argumentValue(1)) as IIslToken;
        return CoalesceToken(left, right, ctx.getPosition());
    }

    override fun visitArgumentItem(ctx: IslParser.ArgumentItemContext): Any {
        val value: IIslToken = when {
            ctx.math() != null -> visit(ctx.math()) as IIslToken
            ctx.declareObject() != null -> visit(ctx.declareObject()) as IIslToken
            ctx.literal() != null -> visit(ctx.literal()) as IIslToken
            ctx.array() != null -> visit(ctx.array()) as IIslToken
            ctx.interpolate() != null -> visit(ctx.interpolate()) as IIslToken
            ctx.functionCall() != null -> visit(ctx.functionCall()) as IIslToken
            ctx.variableSelector() != null -> visitVariableSelector(ctx.variableSelector())
            else -> throw TransformCompilationException(
                "Unknown Argument Token ${ctx.text}",
                ctx.getPosition(),
                contents
            )
        };

        val modifiedToken = visitModifiers(value, ctx.modifier());
        return modifiedToken;
    }

    override fun visitVariableSelector(ctx: IslParser.VariableSelectorContext): IIslToken {
        // check if we have complex expressions ( either embedded variables or conditions )
        // if we don't then use a simplified VariableSelectorValueToken
        // if we don't have advanced expressions like variableConditionPart we can skip and do a simple variable selector
        val hasComplexConditions =
            (ctx.variableSelectorStart().children?.any() { it is IslParser.VariableConditionPartContext } ?: false)
                    ||
                    (ctx.variableSelectorPart()?.any() { it ->
                        it.children.any() { partIt -> partIt is IslParser.VariableConditionPartContext }
                    } ?: false);

        if (!hasComplexConditions) {
            return VariableSelectorValueToken(ctx.text, ctx.getPosition());
        } else {
            val startSelector = ctx.variableSelectorStart();
            var start = SimpleVariableSelectorValueToken(
                "$" + startSelector.shortIdentifier().text,
                ConvertUtils.tryParseInt(startSelector.variableIndexPart()?.text),
                visitConditionExpression(startSelector.variableConditionPart()?.conditionExpression()),
                startSelector.getPosition()
            ) as IIslToken;

            // chain child properties like modifiers where each property has a link to the previous one)
            ctx.variableSelectorPart()?.forEach {
                start = SimplePropertySelectorValueToken(
                    it.shortIdentifier().text ?: it.variableTextPropertyPart().text,
                    start,
                    ConvertUtils.tryParseInt(it.variableIndexPart()?.text),
                    visitConditionExpression(it.variableConditionPart()?.conditionExpression()),
                    it.getPosition()
                )
            }

            return start;
        }
    }


    override fun visitRightSideValue(ctx: IslParser.RightSideValueContext): Any {
        if (ctx.exception != null)
            throw ctx.exception

        return when {
//            ctx.VARIDENT() != null -> VariableSelectorValueToken(
//                ctx.VARIDENT().text,
//                ParserUtils.getPosition(ctx.VARIDENT().symbol)
//            )
//            ctx.IDENT() != null -> VariableSelectorValueToken(
//                ctx.IDENT().text,
//                ParserUtils.getPosition(ctx.IDENT().symbol)
//            )
            ctx.variableSelector() != null -> visitVariableSelector(ctx.variableSelector())
            ctx.literal() != null -> visitLiteral(ctx.literal())
            ctx.array() != null -> visitArray(ctx.array())
            ctx.interpolate() != null -> visitInterpolate(ctx.interpolate()) as IIslToken
            ctx.functionCall() != null -> visitFunctionCall(ctx.functionCall()) as IIslToken
            else -> throw TransformCompilationException("Unknown Token ${ctx.text}", ctx.getPosition(), contents)
        };
    }

    override fun visitLiteral(ctx: IslParser.LiteralContext): LiteralValueToken {
        if (ctx.exception != null)
            throw ctx.exception

        return when {
            ctx.QUOTEDSTRING() != null -> LiteralValueToken(
                ctx.text.fixString(),
                ParserUtils.getPosition(ctx.QUOTEDSTRING().symbol)
            )

            ctx.BOOL() != null -> {
                if (ctx.BOOL().symbol.text == "null")
                    LiteralValueToken(null, ParserUtils.getPosition(ctx.BOOL().symbol))
                else
                    LiteralValueToken(
                        ctx.BOOL().symbol.text.toBoolean(),
                        ParserUtils.getPosition(ctx.BOOL().symbol)
                    )
            }

            else -> LiteralValueToken(BigDecimal(ctx.NUM().symbol.text), ctx.getPosition())
        }
    }

//    override fun visitIdent(ctx: IslParser.m): Any {
//        if (ctx.exception != null)
//            throw ctx.exception
//
//        return super.visitIdent(ctx)!!
//    }

    // `
    override fun visitInterpolate(ctx: IslParser.InterpolateContext): StringInterpolateToken {
        val list = ArrayList<IIslToken>()

        val parts = ctx.children.subList(1, ctx.children.size - 1);
        for (s in parts) {
            if (s.text != "`") {   // avoid backtick (first and last token) - argh - this needs to be done nicer
                val result = visit(s) as IIslToken
                list.add(result)
            }
        }

        return StringInterpolateToken(list, ctx.getPosition());
    }

    override fun visitFuncCallInterpolate(ctx: IslParser.FuncCallInterpolateContext): Any? {
        val functionName = ctx.multiIdent().text;

        val arguments = visitArguments(ctx.arguments());

        return FunctionCallToken(functionName.lowercase(), arguments, null, ctx.getPosition());
    }

    override fun visitInterpolateText(ctx: IslParser.InterpolateTextContext): Any {
        // replace \$ with $ as per the TEXT interpolation
        val text = ctx.text.replace("\\$", "$");
        return LiteralValueToken(text, ctx.getPosition());
    }

    override fun visitSimpleInterpolateVariable(ctx: IslParser.SimpleInterpolateVariableContext): Any {
        return VariableSelectorValueToken(ctx.text, ctx.getPosition());
    }

    override fun visitExpressionInterpolate(ctx: IslParser.ExpressionInterpolateContext): Any {
        return when {
            ctx.variableWithModifier()?.variableSelector() != null -> {
                val variable = visit(ctx.variableWithModifier().variableSelector()) as IIslToken
                // apply modifier
                val modifiedToken = visitModifiers(variable, ctx.variableWithModifier().modifier());
                modifiedToken;
            }

            ctx.functionCall() != null -> {
                visit(ctx.functionCall()) as IIslToken
            }

            ctx.math() != null -> {
                visit(ctx.math()) as IIslToken
            }

            else -> throw TransformCompilationException(
                "Unknown interpolation ${ctx.text}.",
                ctx.getPosition(),
                contents
            )
        };
    }

    override fun visitVariableDeclaration(ctx: IslParser.VariableDeclarationContext?): Any? {
        return super.visitVariableDeclaration(ctx)
    }

    override fun visitStatements(ctx: IslParser.StatementsContext): Any {
        val list = ArrayList<IIslToken>()
        for (s in ctx.statement()) {
            val result = visit(s) as IIslToken
            list.add(result)
        }
        return StatementsToken(list, ctx.getPosition());
    }

    override fun visitInlineIf(ctx: IslParser.InlineIfContext): Any {
        val expression = visitConditionExpression(ctx.condition().conditionExpression()) as IIslToken;

        val trueStatements = visit(if (ctx.rhsval() != null) ctx.rhsval() else ctx.declareObject()) as IIslToken
        val falseStatements = if (ctx.inlineElse() != null) visit(
            if (ctx.inlineElse().rhsval() != null) ctx.inlineElse().rhsval() else ctx.inlineElse().declareObject()
        ) as IIslToken? else null;

        return ConditionToken(
            expression,
            trueStatements,
            falseStatements,
            ctx.getPosition()
        );
    }

    override fun visitIfStatement(ctx: IslParser.IfStatementContext): Any {
        val trueStatements = visit(ctx.trueStatements) as IIslToken
        val falseStatements =
            if (ctx.elseClause() != null)
                visit(ctx.elseClause().falseStatements) as IIslToken;
            else
                null;

        val expression = visitConditionExpression(ctx.condition().conditionExpression()) as IIslToken;
        return ConditionToken(expression, trueStatements, falseStatements, ctx.getPosition());
    }

    override fun visitConditionExpression(ctx: IslParser.ConditionExpressionContext?): IIslToken? {
        if (ctx == null)
            return null;

        // the simple and naive implementation - this can be heavily optimized - but for now this is correct
        // we'll optimize later
        if (ctx.simpleCondition() != null)
            return visitSimpleCondition(ctx.simpleCondition());

        if (ctx.conditionExpression().size == 1) {
            // shortcut - it's just various brackets
            return visitConditionExpression(ctx.conditionExpression(0));
        } else {
            val left = visitConditionExpression(ctx.leftExpression) as IIslToken;
            val right = visitConditionExpression(ctx.rigthExpression) as IIslToken;
            val operator = ctx.LOP().symbol.text;
            return ConditionExpressionToken(left, operator, right, ctx.getPosition());
        }
    }

    override fun visitSimpleCondition(ctx: IslParser.SimpleConditionContext): IIslToken {
        if (ctx.singleLeft != null) {
            // if ( abc ) or if ( !abc )
            val singleLeft = visit(ctx.singleLeft) as IIslToken;
            val bang = if (ctx.BANG() == null) "exists" else "notexists";
            return SimpleConditionToken(singleLeft, bang, null, ctx.getPosition());
        } else {
            // left operator right
            val left = visit(ctx.leftCondition) as IIslToken;
            // we could have either a relop or a match/notmatch
            if (ctx.relop() != null) {
                val operator = ctx.relop().text;
                val right = visit(ctx.rightCondition) as IIslToken;
                return SimpleConditionToken(left, operator, right, ctx.getPosition());
            } else {
                val operator = ctx.regexrelop().text;
                val right = LiteralValueToken(ctx.regexString().text.removeQuotes(), ctx.regexString().getPosition());
                return SimpleConditionToken(left, operator, right, ctx.getPosition());
            }
        }
    }

    /**
     * switch ( value ) ... endswitch
     */
    override fun visitSwitchCaseStatement(ctx: IslParser.SwitchCaseStatementContext): Any {
        val value = visit(ctx.rhsid()) as IIslToken;
        val cases = ctx.switchCaseCondition()
            .map {
                visitSwitchCaseCondition(it) as SwitchCaseBranchToken;
            }.toTypedArray().toMutableList();

        if (ctx.switchCaseElseCondition() != null) {
            val elseBranch = ctx.switchCaseElseCondition();
            val elseStatements = when {
                elseBranch.functionCall() != null -> visitFunctionCall(elseBranch.functionCall()) as IIslToken
                elseBranch.statements() != null -> visitStatements(elseBranch.statements()) as IIslToken;
                elseBranch.rhsid() != null -> visitRhsid(elseBranch.rhsid()) as IIslToken;
                elseBranch.declareObject() != null -> visitDeclareObject(elseBranch.declareObject()) as IIslToken;
                else -> throw TransformCompilationException(
                    "Unknown Case Token ${elseBranch.text}",
                    ctx.getPosition(),
                    contents
                )
            };
            cases.add(
                SwitchCaseBranchToken(
                    LiteralValueToken("else", elseBranch.getPosition()),
                    "else",
                    elseStatements,
                    ctx.switchCaseElseCondition().getPosition()
                )
            );
        }

        return SwitchCaseToken(value, cases.toTypedArray(), ctx.getPosition());
    }

    override fun visitSwitchCaseCondition(ctx: IslParser.SwitchCaseConditionContext): Any {
        val condition = when {
            ctx.relop() != null -> ctx.relop().text
            ctx.regexString() != null -> "matches"
            else -> "=="
        }

        val value = when {
            ctx.literal() != null -> visitLiteral(ctx.literal())
            ctx.variableSelector() != null -> visitVariableSelector(ctx.variableSelector())
            ctx.array() != null -> visitArray(ctx.array())
            ctx.regexString() != null -> LiteralValueToken(
                ctx.regexString().text.removeQuotes(),
                ctx.regexString().getPosition()
            )

            else -> throw TransformCompilationException(
                "Unknown Case Condition ${ctx.text}",
                ctx.getPosition(),
                contents
            )
        }

        val statements = when {
            ctx.functionCall() != null -> visitFunctionCall(ctx.functionCall()) as IIslToken
            ctx.statements() != null -> visitStatements(ctx.statements()) as IIslToken
            ctx.resultVariable != null -> visitRhsid(ctx.resultVariable) as IIslToken
            ctx.declareObject() != null -> visitDeclareObject(ctx.declareObject()) as IIslToken
            else -> throw TransformCompilationException("Unknown Case Action ${ctx.text}", ctx.getPosition(), contents)
        }

        return SwitchCaseBranchToken(value, condition, statements, ctx.getPosition())
    }

    override fun visitArray(ctx: IslParser.ArrayContext): IIslToken {
        // just an array [ [], [ 123 ], [ 123, 345 ] ... ]
        val items = mutableListOf<IIslToken>();

        for (value in ctx.arrayArgument()) {
            items.add(visit(value.argumentValue() ?: value.spreadSelector()) as IIslToken);
        }
        return DeclareArrayToken(items.toTypedArray(), ctx.getPosition());
    }

    // don't process the injected Groovy
    override fun visitSpec(ctx: IslParser.SpecContext): ModuleImplementationToken {
        if (ctx.statements() != null) {
            val inlineStatements = visit(ctx.statements())!! as IIslToken;
            // wrap around in a 'run' function
            val function =
                FunctionDeclarationToken(
                    FunctionType.Function,
                    defaultFunctionName.lowercase(),
                    listOf(),
                    listOf(),
                    inlineStatements,
                    IslType.Object,
                    ctx.getPosition()
                );

            val module = ModuleImplementationToken(
                moduleName,
                mutableListOf(),
                mutableMapOf(),
                listOf(function),
                ctx.getPosition(),
                compilationWarnings
            );

            return module;
        } else
            return visitModule(ctx.importDeclaration(), ctx.typeDeclaration(), ctx.functionDeclaration(), ctx);
    }

    private fun visitModule(
        importTokens: List<IslParser.ImportDeclarationContext>?,
        typeDeclarations: List<IslParser.TypeDeclarationContext>?,
        functionTokens: List<IslParser.FunctionDeclarationContext>,
        ctx: IslParser.SpecContext
    ): ModuleImplementationToken {
        val imports = importTokens?.map { visit(it) as ImportDeclarationToken } ?: mutableListOf();

        this.types.putAll(typeDeclarations?.associate { visitTypeDeclaration(it) }?.toMutableMap() ?: mutableMapOf());

        val functions = functionTokens.map { visit(it) as FunctionDeclarationToken; }
        return ModuleImplementationToken(
            "",
            imports.toMutableList(),
            types,
            functions,
            ctx.getPosition(),
            compilationWarnings
        );
    }

    override fun visitImportDeclaration(ctx: IslParser.ImportDeclarationContext): Any {
        return ImportDeclarationToken(
            ctx.ID().symbol.text,
            ctx.QUOTEDSTRING().symbol.text.fixString(),
            ctx.getPosition()
        );
    }


    override fun visitMath(ctx: IslParser.MathContext): Any {
        return visitMathExpresion(ctx.mathExpresion());
    }

    override fun visitMathExpresion(ctx: IslParser.MathExpresionContext): Any {
        if (ctx.mathValue() != null) {
            if (ctx.mathValue().NUM() != null)
                return LiteralValueToken(BigDecimal(ctx.mathValue().NUM().symbol.text), ctx.getPosition())
            else if (ctx.mathValue().variableSelector() != null)
                return visitVariableSelector(ctx.mathValue().variableSelector())
            else // function call
                return visitFunctionCall(ctx.mathValue().functionCall())
        } else {
            val left = visit(ctx.mathExpresion(0)) as IIslToken;

            val operator = ctx.MATH_TIMES()?.symbol?.text
                ?: ctx.MATH_DIV()?.symbol?.text
                ?: ctx.MATH_PLUS()?.symbol?.text
                ?: ctx.MATH_MINUS()?.symbol?.text
                ?: "unknown";

            if (ctx.mathExpresion().size > 1) {
                val right = visit(ctx.mathExpresion(1)) as IIslToken
                return MathExpressionToken(left, right, operator, ctx.getPosition());
            }   // else - single expression ( expression )

            return left;
        }
    }

    override fun visitMathValue(ctx: IslParser.MathValueContext): Any {
        if (ctx.NUM() != null)
            return LiteralValueToken(BigDecimal(ctx.NUM().symbol.text), ctx.getPosition())
        else if (ctx.variableSelector() != null)
            return visitVariableSelector(ctx.variableSelector())
        else // function
            return visitFunctionCall(ctx.functionCall());
    }

    private fun ParserRuleContext.getPosition(): Position {
//        println("Position for=${this.text} (stop=${this.stop.text}) ${this.start.line}:[${this.start.startIndex}] " +
//                "- ${this.stop.line}:[${this.stop.charPositionInLine}-${this.stop.text.length}]")
        return Position(
            moduleName,
            this.start.line + startLine,
            this.start.charPositionInLine,  //  + startCol
            this.stop.line + startLine,
            this.stop.charPositionInLine + this.stop.text.trimEnd().length //  + startCol
        );
    }

    private fun Token.getPosition(): Position {
        return Position(
            moduleName,
            this.line + startLine,
            this.charPositionInLine + startCol,
            this.line + startLine,
            this.charPositionInLine + this.text.length + startCol
        );
    }
}