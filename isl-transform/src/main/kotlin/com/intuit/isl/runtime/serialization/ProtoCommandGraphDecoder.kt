package com.intuit.isl.runtime.serialization

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.*
import com.intuit.isl.commands.modifiers.*
import com.intuit.isl.parser.tokens.*
import com.intuit.isl.serialization.proto.*
import com.intuit.isl.utils.JsonConvert
import com.intuit.isl.utils.Position
import java.util.ArrayList
import java.util.HashMap

internal data class DecodeContext(
    val moduleName: String,
    val owningFunction: FunctionDeclarationToken,
)

internal class ProtoCommandGraphDecoder(
    private val ctx: DecodeContext,
) {
    private fun pos() = Position(ctx.moduleName, 0, 0)

    fun decodeRoot(graph: CommandGraphPB): IIslCommand {
        val n = graph.nodesCount
        val slots = arrayOfNulls<IIslCommand>(n)
        for (i in 0 until n) {
            slots[i] = decodeNode(graph.getNodes(i), slots)
        }
        return slots[graph.rootIndex] ?: throw IllegalStateException("missing root")
    }

    private fun idx(i: Int, slots: Array<IIslCommand?>): IIslCommand =
        slots.getOrNull(i) ?: throw IllegalStateException("bad child index $i")

    private fun decodeEvaluable(i: Int, slots: Array<IIslCommand?>): com.intuit.isl.commands.IEvaluableConditionCommand {
        val c = idx(i, slots)
        return c as? com.intuit.isl.commands.IEvaluableConditionCommand
            ?: throw IllegalStateException("expected evaluable at $i")
    }

    private fun decodeNode(n: CommandNodePB, slots: Array<IIslCommand?>): IIslCommand {
        val p = pos()
        return when {
            n.hasLiteral() -> {
                val tree = JsonConvert.mapper.readTree(n.literal.jsonValue)
                LiteralValueCommand(LiteralValueToken(tree, p))
            }
            n.hasHardwiredCall() -> {
                val h = n.hardwiredCall
                val args = h.argIndicesList.map { idx(it, slots) }
                HardwiredFunctionCallCommand(
                    FunctionCallToken(h.targetKey.replace(":", "."), emptyList(), null, p),
                    args,
                    null
                )
            }
            n.hasFunctionCall() -> {
                val f = n.functionCall
                val args = f.argIndicesList.map { idx(it, slots) }
                FunctionCallCommand(FunctionCallToken(f.name, emptyList(), null, p), args)
            }
            n.hasStmtFunctionCall() -> {
                val f = n.stmtFunctionCall
                val args = f.argIndicesList.map { idx(it, slots) }
                StatementFunctionCallCommand(
                    FunctionCallToken(f.name, emptyList(), null, p),
                    args,
                    idx(f.statementsIndex, slots)
                )
            }
            n.hasVariablePath() -> {
                val v = n.variablePath
                val raw = when (v.pathKind) {
                    VariablePathKind.PATH_FAST_PARTS ->
                        v.variableName.trimStart('$') + "." + v.pathPartsList.joinToString(".")
                    VariablePathKind.PATH_FAST_SINGLE, VariablePathKind.PATH_STANDARD ->
                        if (v.path.isEmpty()) v.variableName.trimStart('$')
                        else v.variableName.trimStart('$') + "." + v.path
                    VariablePathKind.UNRECOGNIZED ->
                        v.variableName.trimStart('$')
                }
                val tok = VariableSelectorValueToken(raw, p)
                when (v.pathKind) {
                    VariablePathKind.PATH_FAST_PARTS ->
                        FastVariableWithPathSelectorValueCommand(tok, v.pathPartsList.toTypedArray())
                    VariablePathKind.PATH_FAST_SINGLE ->
                        FastSingleVariableWithPathSelectorValueCommand(tok)
                    else ->
                        if (tok.path.isNullOrEmpty()) VariableSelectorValueCommand(tok)
                        else VariableWithPathSelectorValueCommand(tok)
                }
            }
            n.hasVariableSimple() -> {
                val v = n.variableSimple
                val cond =
                    if (v.conditionIndex >= 0) decodeEvaluable(v.conditionIndex, slots) else null
                VariableSimpleSelectorCommand(
                    SimpleVariableSelectorValueToken(
                        v.name,
                        v.indexSelector.takeIf { it >= 0 },
                        null,
                        p
                    ),
                    cond
                )
            }
            n.hasVariableProperty() -> {
                val v = n.variableProperty
                VariablePropertySelectorCommand(
                    SimplePropertySelectorValueToken(v.property, NoopToken(), null, null, p),
                    idx(v.previousIndex, slots),
                    null
                )
            }
            n.hasFunctionReturnCmd() -> {
                val v = n.functionReturnCmd
                FunctionReturnCommand(
                    FunctionReturnToken(NoopToken(), p),
                    idx(v.valueIndex, slots)
                )
            }
            n.hasReturnHandler() ->
                FunctionReturnCommandHandler(ctx.owningFunction, idx(n.returnHandler.innerIndex, slots))
            n.hasAnnotation() -> {
                val a = n.annotation
                val args = a.argIndicesList.map { idx(it, slots) }
                val annTok = AnnotationDeclarationToken(a.annotationName, emptyList(), p)
                AnnotationCommand(annTok, args, idx(a.nextIndex, slots), ctx.owningFunction)
            }
            n.hasStatements() -> {
                val st = n.statements
                val cmds = st.commandIndicesList.map { idx(it, slots) }
                StatementsBuildCommand(StatementsToken(emptyList(), p), cmds)
            }
            n.hasObjectBuild() -> {
                val ob = n.objectBuild
                val cmds = ob.commandIndicesList.map { idx(it, slots) }.toMutableList()
                val tok = DeclareObjectToken(StatementsToken(emptyList(), p), p)
                ObjectBuildCommand(tok, cmds)
            }
            n.hasConstantObjectBuild() -> {
                val on = JsonConvert.mapper.readValue(
                    n.constantObjectBuild.jsonValue,
                    ObjectNode::class.java
                ) as ObjectNode
                ConstantObjectBuildCommand(NoopToken(), on)
            }
            n.hasAssignProperty() -> {
                val a = n.assignProperty
                AssignPropertyCommand(
                    AssignPropertyToken(a.property, NoopToken(), null, p),
                    idx(a.valueIndex, slots)
                )
            }
            n.hasAssignDynamicProperty() -> {
                val a = n.assignDynamicProperty
                AssignDynamicPropertyCommand(
                    AssignDynamicPropertyToken(StringInterpolateToken(ArrayList(), p), NoopToken(), null, p),
                    idx(a.nameExprIndex, slots),
                    idx(a.valueIndex, slots)
                )
            }
            n.hasAssignVariable() -> {
                val a = n.assignVariable
                val top = a.topPropertyName.takeIf { it.isNotEmpty() }
                val varRaw = a.variableName.removePrefix("$").lowercase()
                AssignVariableCommand(
                    AssignVariableToken(varRaw, top, null, NoopToken(), p),
                    idx(a.valueIndex, slots)
                )
            }
            n.hasSpread() -> SpreadCommand(NoopToken(), idx(n.spread.variableIndex, slots))
            n.hasCondition() -> {
                val c = n.condition
                ConditionCommand(
                    NoopToken(),
                    decodeEvaluable(c.conditionIndex, slots),
                    idx(c.thenIndex, slots),
                    if (c.elseIndex >= 0) idx(c.elseIndex, slots) else null
                )
            }
            n.hasSimpleCondition() -> {
                val c = n.simpleCondition
                SimpleConditionCommand(
                    NoopToken(),
                    idx(c.leftIndex, slots),
                    c.condition,
                    if (c.rightIndex >= 0) idx(c.rightIndex, slots) else null
                )
            }
            n.hasCoalesce() -> {
                val c = n.coalesce
                CoalesceCommand(NoopToken(), idx(c.leftIndex, slots), idx(c.rightIndex, slots))
            }
            n.hasConditionExpr() -> {
                val c = n.conditionExpr
                ConditionExpressionCommand(
                    NoopToken(),
                    decodeEvaluable(c.leftIndex, slots),
                    c.condition,
                    decodeEvaluable(c.rightIndex, slots)
                )
            }
            n.hasWhileCmd() -> {
                val w = n.whileCmd
                WhileCommand(
                    WhileToken(
                        NoopToken(),
                        if (w.maxLoopsIndex >= 0) NoopToken() else null,
                        NoopToken(),
                        p
                    ),
                    decodeEvaluable(w.conditionIndex, slots),
                    if (w.maxLoopsIndex >= 0) idx(w.maxLoopsIndex, slots) else null,
                    idx(w.statementsIndex, slots)
                )
            }
            n.hasForeach() -> {
                val f = n.foreach
                ForEachCommand(
                    ForEachToken(f.iterator, NoopToken(), NoopToken(), p),
                    idx(f.sourceIndex, slots),
                    idx(f.statementsIndex, slots)
                )
            }
            n.hasParallelForeach() -> {
                val f = n.parallelForeach
                ParallelForEachCommand(
                    ParallelForEachToken(f.iterator, null, NoopToken(), NoopToken(), p),
                    if (f.optionsIndex >= 0) idx(f.optionsIndex, slots) else null,
                    idx(f.sourceIndex, slots),
                    idx(f.statementsIndex, slots)
                )
            }
            n.hasSwitchCase() -> {
                val s = n.switchCase
                val branches = s.branchesList.map { br ->
                    SwitchCaseCommand.SwitchCaseBranchCommand(
                        NoopToken(),
                        br.condition,
                        idx(br.rightIndex, slots),
                        idx(br.bodyIndex, slots)
                    )
                }.toTypedArray()
                SwitchCaseCommand(NoopToken(), idx(s.selectorIndex, slots), branches)
            }
            n.hasHashSwitch() -> {
                val h = n.hashSwitch
                val map = HashMap<String, IIslCommand>()
                for (e in h.casesMap.entries) {
                    map[e.key] = idx(e.value, slots)
                }
                val def = if (h.defaultIndex >= 0) idx(h.defaultIndex, slots) else null
                HashDispatchSwitchCommand(NoopToken(), idx(h.selectorIndex, slots), map, def)
            }
            n.hasArrayCmd() -> {
                val a = n.arrayCmd
                val els = a.elementIndicesList.map { idx(it, slots) }.toCollection(ArrayList())
                ArrayCommand(
                    DeclareArrayToken(emptyArray<IIslToken>(), p),
                    els,
                    a.arrayName.takeIf { it.isNotEmpty() }
                )
            }
            n.hasInterpolate() -> {
                val parts = ArrayList(n.interpolate.partIndicesList.map { idx(it, slots) })
                InterpolateCommand(NoopToken(), parts)
            }
            n.hasMathExpr() -> {
                val m = n.mathExpr
                MathExpressionCommand(
                    NoopToken(),
                    idx(m.leftIndex, slots),
                    idx(m.rightIndex, slots),
                    m.operator
                )
            }
            n.hasGenericCondModifier() -> {
                val g = n.genericCondModifier
                val args = g.argumentIndicesList.map { idx(it, slots) }
                val tok = GenericConditionalModifierValueToken(
                    g.name,
                    NoopToken(),
                    NoopToken(),
                    null,
                    emptyList(),
                    p
                )
                GenericConditionalModifierCommand(
                    g.name,
                    tok,
                    idx(g.valueIndex, slots),
                    decodeEvaluable(g.expressionIndex, slots),
                    args
                )
            }
            n.hasModifierValue() -> {
                val m = n.modifierValue
                val args = m.argIndicesList.map { idx(it, slots) }
                val mtok = ModifierValueToken(m.modifierName, NoopToken(), emptyList(), p)
                ModifierValueCommand(mtok, m.modifierName, idx(m.valueIndex, slots), args)
            }
            n.hasFilterModifier() -> {
                val f = n.filterModifier
                FilterModifierValueCommand(
                    FilterModifierValueToken(NoopToken(), NoopToken(), p),
                    idx(f.valueIndex, slots),
                    decodeEvaluable(f.expressionIndex, slots)
                )
            }
            n.hasMapModifier() -> {
                val m = n.mapModifier
                MapModifierValueCommand(
                    MapModifierValueToken(NoopToken(), NoopToken(), p),
                    idx(m.previousIndex, slots),
                    idx(m.argumentIndex, slots)
                )
            }
            n.hasReduceModifier() -> {
                val r = n.reduceModifier
                ReduceModifierValueCommand(
                    ModifierValueToken("reduce", NoopToken(), emptyList(), p),
                    idx(r.sourceIndex, slots),
                    idx(r.argumentIndex, slots)
                )
            }
            n.hasFilterMapModifier() -> {
                val f = n.filterMapModifier
                FilterMapModifierValueCommand(
                    MapModifierValueToken(NoopToken(), NoopToken(), p),
                    idx(f.sourceIndex, slots),
                    decodeEvaluable(f.predicateIndex, slots),
                    idx(f.mapArgumentIndex, slots)
                )
            }
            n.hasCondModifier() -> {
                val c = n.condModifier
                ConditionModifierValueCommand(
                    ConditionModifierValueToken(NoopToken(), NoopToken(), NoopToken(), p),
                    idx(c.valueIndex, slots),
                    decodeEvaluable(c.expressionIndex, slots),
                    idx(c.trueModifierIndex, slots)
                )
            }
            n.hasNoop() -> NoopCommand(NoopToken())
            else -> throw UnsupportedCommandForPreCompileException("unknownNode:${n.nodeCase}")
        }
    }
}
