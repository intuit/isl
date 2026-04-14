package com.intuit.isl.runtime.serialization

import com.fasterxml.jackson.databind.JsonNode
import com.intuit.isl.commands.*
import com.intuit.isl.commands.modifiers.*
import com.intuit.isl.parser.tokens.VariableSelectorValueToken
import com.intuit.isl.serialization.proto.*
import com.intuit.isl.utils.JsonConvert
import java.util.IdentityHashMap

internal class ProtoCommandGraphEncoder(private val moduleName: String) {

    private val memo = IdentityHashMap<IIslCommand, Int>()
    private val nodes = mutableListOf<CommandNodePB>()

    fun encodeRoot(root: IIslCommand): CommandGraphPB {
        memo.clear()
        nodes.clear()
        val rootIdx = encodeCommand(root)
        return CommandGraphPB.newBuilder()
            .addAllNodes(nodes)
            .setRootIndex(rootIdx)
            .build()
    }

    private fun encodeEvaluable(e: com.intuit.isl.commands.IEvaluableConditionCommand): Int {
        return when (e) {
            is IIslCommand -> encodeCommand(e)
            else -> throw UnsupportedCommandForPreCompileException("evaluable:${e::class.java.name}")
        }
    }

    private fun encodeCommand(cmd: IIslCommand): Int {
        memo[cmd]?.let { return it }
        if (cmd is RecursiveFunctionDeclarationCommand) {
            return encodeCommand(cmd.statements)
        }
        val built = when (cmd) {
            is LiteralValueCommand -> {
                val v = cmd.token.value
                val json = when (v) {
                    null -> "null"
                    is JsonNode -> JsonConvert.mapper.writeValueAsString(v)
                    else -> JsonConvert.mapper.writeValueAsString(JsonConvert.convert(v))
                }
                node { setLiteral(LiteralPB.newBuilder().setJsonValue(json)) }
            }
            is HardwiredFunctionCallCommand -> {
                val args = cmd.callArguments.map { encodeCommand(it) }
                node {
                    setHardwiredCall(
                        HardwiredCallPB.newBuilder()
                            .setTargetKey(cmd.token.name.replace(".", ":"))
                            .addAllArgIndices(args)
                    )
                }
            }
            is FunctionCallCommand -> {
                val args = cmd.callArguments.map { encodeCommand(it) }
                node {
                    setFunctionCall(
                        FunctionCallPB.newBuilder().setName(cmd.token.name).addAllArgIndices(args)
                    )
                }
            }
            is StatementFunctionCallCommand -> {
                val args = cmd.statementArguments.map { encodeCommand(it) }
                val st = encodeCommand(cmd.statementBody)
                node {
                    setStmtFunctionCall(
                        StmtFunctionCallPB.newBuilder().setName(cmd.token.name).addAllArgIndices(args)
                            .setStatementsIndex(st)
                    )
                }
            }
            is VariableSelectorValueCommand -> encodeVariablePath(cmd)
            is VariablePropertySelectorCommand -> {
                val prev = encodeCommand(cmd.propertyPrevious)
                node {
                    setVariableProperty(
                        VariablePropertyPB.newBuilder()
                            .setProperty(cmd.token.name)
                            .setVariableName("")
                            .setPreviousIndex(prev)
                    )
                }
            }
            is VariableSimpleSelectorCommand -> {
                val condIdx = cmd.indexCondition?.let { encodeEvaluable(it) } ?: -1
                node {
                    setVariableSimple(
                        VariableSimplePB.newBuilder()
                            .setName(cmd.token.name)
                            .setIndexSelector(cmd.token.indexSelector ?: -1)
                            .setConditionIndex(condIdx)
                    )
                }
            }
            is FunctionReturnCommand -> {
                val v = encodeCommand(cmd.returnExpression)
                node { setFunctionReturnCmd(FunctionReturnCmdPB.newBuilder().setValueIndex(v)) }
            }
            is FunctionReturnCommandHandler -> {
                val inner = encodeCommand(cmd.statements)
                node { setReturnHandler(ReturnHandlerPB.newBuilder().setInnerIndex(inner)) }
            }
            is AnnotationCommand -> {
                val args = cmd.arguments.map { encodeCommand(it) }
                val next = encodeCommand(cmd.nextCommand)
                node {
                    setAnnotation(
                        AnnotationPB.newBuilder()
                            .setAnnotationName(cmd.token.annotationName)
                            .addAllArgIndices(args)
                            .setNextIndex(next)
                            .setOwningFunctionName(cmd.function.functionName)
                    )
                }
            }
            is StatementsBuildCommand -> {
                val idxs = cmd.commands.map { encodeCommand(it) }
                node { setStatements(StatementsPB.newBuilder().addAllCommandIndices(idxs)) }
            }
            is ObjectBuildCommand -> {
                val idxs = cmd.commands.map { encodeCommand(it) }
                node { setObjectBuild(ObjectBuildPB.newBuilder().addAllCommandIndices(idxs)) }
            }
            is ConstantObjectBuildCommand -> {
                val json = JsonConvert.mapper.writeValueAsString(cmd.prototypeTemplateForSerialization)
                node { setConstantObjectBuild(ConstantObjectBuildPB.newBuilder().setJsonValue(json)) }
            }
            is AssignPropertyCommand -> {
                val v = encodeCommand(cmd.value)
                node {
                    setAssignProperty(
                        AssignPropertyPB.newBuilder().setProperty(cmd.token.name).setDynamic(false).setValueIndex(v)
                    )
                }
            }
            is AssignDynamicPropertyCommand -> {
                val n = encodeCommand(cmd.name)
                val v = encodeCommand(cmd.value)
                node {
                    setAssignDynamicProperty(
                        AssignDynamicPropertyPB.newBuilder().setNameExprIndex(n).setValueIndex(v)
                    )
                }
            }
            is AssignVariableCommand -> {
                val v = encodeCommand(cmd.value)
                node {
                    setAssignVariable(
                        AssignVariablePB.newBuilder()
                            .setVariableName(cmd.token.name)
                            .setTopPropertyName(cmd.token.topPropertyName ?: "")
                            .setValueIndex(v)
                    )
                }
            }
            is SpreadCommand -> {
                val v = encodeCommand(cmd.variable)
                node { setSpread(SpreadPB.newBuilder().setVariableIndex(v)) }
            }
            is ConditionCommand -> {
                val c = encodeEvaluable(cmd.branchCondition)
                val t = encodeCommand(cmd.trueBranch)
                val e = cmd.falseBranch?.let { encodeCommand(it) } ?: -1
                node {
                    setCondition(
                        ConditionPB.newBuilder().setConditionIndex(c).setThenIndex(t).setElseIndex(e)
                    )
                }
            }
            is SimpleConditionCommand -> {
                val l = encodeCommand(cmd.left)
                val r = cmd.right?.let { encodeCommand(it) } ?: -1
                node {
                    setSimpleCondition(
                        SimpleConditionPB.newBuilder().setLeftIndex(l).setCondition(cmd.condition).setRightIndex(r)
                    )
                }
            }
            is CoalesceCommand -> {
                val l = encodeCommand(cmd.left)
                val r = encodeCommand(cmd.right)
                node { setCoalesce(CoalescePB.newBuilder().setLeftIndex(l).setRightIndex(r)) }
            }
            is ConditionExpressionCommand -> {
                val l = encodeEvaluable(cmd.left)
                val r = encodeEvaluable(cmd.right)
                node {
                    setConditionExpr(
                        ConditionExprPB.newBuilder().setLeftIndex(l).setCondition(cmd.condition).setRightIndex(r)
                    )
                }
            }
            is WhileCommand -> {
                val c = encodeEvaluable(cmd.whileCondition)
                val m = cmd.whileMaxLoops?.let { encodeCommand(it) } ?: -1
                val s = encodeCommand(cmd.statements)
                node {
                    setWhileCmd(WhilePB.newBuilder().setConditionIndex(c).setMaxLoopsIndex(m).setStatementsIndex(s))
                }
            }
            is ParallelForEachCommand -> {
                val src = encodeCommand(cmd.foreachSource)
                val opt = cmd.parallelOptions?.let { encodeCommand(it) } ?: -1
                val st = encodeCommand(cmd.statements)
                CommandNodePB.newBuilder()
                    .setParallelForeach(
                        ParallelForEachPB.newBuilder()
                            .setIterator(cmd.token.iterator)
                            .setSourceIndex(src)
                            .setOptionsIndex(opt)
                            .setStatementsIndex(st)
                    )
                    .build()
            }
            is ForEachCommand -> {
                val src = encodeCommand(cmd.foreachSource)
                val st = encodeCommand(cmd.statements)
                CommandNodePB.newBuilder()
                    .setForeach(
                        ForEachPB.newBuilder()
                            .setIterator(cmd.token.iterator)
                            .setSourceIndex(src)
                            .setStatementsIndex(st)
                    )
                    .build()
            }
            is SwitchCaseCommand -> {
                val sel = encodeCommand(cmd.value)
                val branches = cmd.cases.map { br ->
                    SwitchBranchPB.newBuilder()
                        .setCondition(br.condition)
                        .setRightIndex(encodeCommand(br.right))
                        .setBodyIndex(encodeCommand(br.result))
                        .build()
                }
                node {
                    setSwitchCase(
                        SwitchCasePB.newBuilder().setSelectorIndex(sel).addAllBranches(branches)
                    )
                }
            }
            is HashDispatchSwitchCommand -> {
                val sel = encodeCommand(cmd.value)
                val b = HashDispatchSwitchPB.newBuilder().setSelectorIndex(sel)
                for ((k, v) in cmd.armsByStringKey) {
                    b.putCases(k, encodeCommand(v))
                }
                cmd.defaultArm?.let { b.setDefaultIndex(encodeCommand(it)) }
                CommandNodePB.newBuilder().setHashSwitch(b.build()).build()
            }
            is ArrayCommand -> {
                val els = cmd.elementCommands.map { encodeCommand(it) }
                node {
                    setArrayCmd(
                        ArrayPB.newBuilder().addAllElementIndices(els).setArrayName(cmd.seedVariableName ?: "")
                    )
                }
            }
            is InterpolateCommand -> {
                val parts = cmd.interpolationParts.map { encodeCommand(it) }
                node { setInterpolate(InterpolatePB.newBuilder().addAllPartIndices(parts)) }
            }
            is MathExpressionCommand -> {
                val l = encodeCommand(cmd.left)
                val r = encodeCommand(cmd.right)
                node {
                    setMathExpr(MathExprPB.newBuilder().setLeftIndex(l).setRightIndex(r).setOperator(cmd.operator))
                }
            }
            is PotentialGenericConditionalModifierCommand ->
                throw UnsupportedCommandForPreCompileException("PotentialGenericConditionalModifierCommand")
            is GenericConditionalModifierCommand -> {
                val args = cmd.arguments.map { encodeCommand(it) }
                val v = encodeCommand(cmd.value)
                val e = encodeEvaluable(cmd.expression)
                node {
                    setGenericCondModifier(
                        GenericCondModifierPB.newBuilder()
                            .setName(cmd.name)
                            .setValueIndex(v)
                            .setExpressionIndex(e)
                            .addAllArgumentIndices(args)
                    )
                }
            }
            is FilterModifierValueCommand -> {
                val v = encodeCommand(cmd.filterSource)
                val e = encodeEvaluable(cmd.filterExpression)
                node { setFilterModifier(FilterModifierPB.newBuilder().setValueIndex(v).setExpressionIndex(e)) }
            }
            is MapModifierValueCommand -> {
                val p = encodeCommand(cmd.mapPreviousValue)
                val a = encodeCommand(cmd.mapArgument)
                node { setMapModifier(MapModifierPB.newBuilder().setPreviousIndex(p).setArgumentIndex(a)) }
            }
            is ReduceModifierValueCommand -> {
                val s = encodeCommand(cmd.reduceSource)
                val a = encodeCommand(cmd.reduceArgument)
                node { setReduceModifier(ReduceModifierPB.newBuilder().setSourceIndex(s).setArgumentIndex(a)) }
            }
            is FilterMapModifierValueCommand -> {
                val s = encodeCommand(cmd.filterMapSource)
                val p = encodeEvaluable(cmd.filterMapPredicate)
                val m = encodeCommand(cmd.filterMapMapArgument)
                node {
                    setFilterMapModifier(
                        FilterMapModifierPB.newBuilder()
                            .setSourceIndex(s)
                            .setPredicateIndex(p)
                            .setMapArgumentIndex(m)
                    )
                }
            }
            is ConditionModifierValueCommand -> {
                val v = encodeCommand(cmd.value)
                val e = encodeEvaluable(cmd.expression)
                val t = encodeCommand(cmd.trueModifier)
                node {
                    setCondModifier(CondModifierPB.newBuilder().setValueIndex(v).setExpressionIndex(e).setTrueModifierIndex(t))
                }
            }
            is ModifierValueCommand -> {
                val args = cmd.modifierArgumentCommands.map { encodeCommand(it) }
                val v = encodeCommand(cmd.value)
                val realName = cmd.token.name
                node {
                    setModifierValue(
                        ModifierValuePB.newBuilder().setModifierName(realName).setValueIndex(v)
                            .addAllArgIndices(args)
                    )
                }
            }
            is NoopCommand -> node { setNoop(NoopPB.newBuilder()) }
            is FunctionDeclarationCommand ->
                throw UnsupportedCommandForPreCompileException("nested FunctionDeclarationCommand")
            else -> throw UnsupportedCommandForPreCompileException(cmd::class.java.name)
        }
        val idx = nodes.size
        nodes.add(built)
        memo[cmd] = idx
        return idx
    }

    private fun encodeVariablePath(cmd: VariableSelectorValueCommand): CommandNodePB {
        val vn = cmd.variableName
        return when (cmd) {
            is FastVariableWithPathSelectorValueCommand -> {
                val parts = cmd.pathPartsForSerialization.toList()
                node {
                    setVariablePath(
                        VariablePathPB.newBuilder()
                            .setVariableName(vn)
                            .setPathKind(VariablePathKind.PATH_FAST_PARTS)
                            .addAllPathParts(parts)
                    )
                }
            }
            is FastSingleVariableWithPathSelectorValueCommand -> {
                val path = (cmd.token as VariableSelectorValueToken).path ?: ""
                node {
                    setVariablePath(
                        VariablePathPB.newBuilder()
                            .setVariableName(vn)
                            .setPath(path)
                            .setPathKind(VariablePathKind.PATH_FAST_SINGLE)
                    )
                }
            }
            is VariableWithPathSelectorValueCommand -> {
                val path = (cmd.token as VariableSelectorValueToken).path ?: ""
                node {
                    setVariablePath(
                        VariablePathPB.newBuilder()
                            .setVariableName(vn)
                            .setPath(path)
                            .setPathKind(VariablePathKind.PATH_STANDARD)
                    )
                }
            }
            else -> {
                val tok = (cmd as VariableSelectorValueCommand).token as VariableSelectorValueToken
                val path = tok.path ?: ""
                node {
                    setVariablePath(
                        VariablePathPB.newBuilder()
                            .setVariableName(vn)
                            .setPath(path)
                            .setPathKind(VariablePathKind.PATH_STANDARD)
                    )
                }
            }
        }
    }

    private fun node(block: CommandNodePB.Builder.() -> Unit): CommandNodePB {
        val b = CommandNodePB.newBuilder()
        b.apply(block)
        return b.build()
    }
}
