package com.intuit.isl.commands

import com.intuit.isl.commands.modifiers.*
import com.intuit.isl.debug.CoverableStatementMeta
import com.intuit.isl.debug.SourceCoverageSpan
import com.intuit.isl.runtime.TransformModule
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Assigns a stable positive [BaseCommand.coverageStatementId] to every command in a compiled module
 * (depth-first pre-order). Shared subgraphs are visited once ([IdentityHashMap]).
 *
 * Invoked only when an [com.intuit.isl.debug.IExecutionHook] with [IExecutionHook.preparesStatementIds] runs
 * (e.g. [com.intuit.isl.debug.CodeCoverageHook]), not during [com.intuit.isl.commands.builder.ExecutionBuilder.build].
 * Each [TransformModule] instance is walked at most once per JVM ([preparedModules]).
 */
object CoverageStatementIdAssigner {

    private val preparedModules: MutableSet<TransformModule> = ConcurrentHashMap.newKeySet()

    fun assign(module: TransformModule) {
        if (!preparedModules.add(module)) return
        val next = AtomicInteger(1)
        val seen = IdentityHashMap<IIslCommand, Unit>()
        for (fn in module.functions) {
            assignDeep(fn as IIslCommand, next, seen)
        }
    }

    /**
     * All statements that received a positive [BaseCommand.coverageStatementId] during [assign] for this module.
     * Call after execution when building coverage reports (includes never-executed branches).
     */
    fun listCoverableStatements(module: TransformModule): List<CoverableStatementMeta> {
        val seen = IdentityHashMap<IIslCommand, Unit>()
        val out = mutableListOf<CoverableStatementMeta>()
        for (fn in module.functions) {
            walkCommandGraph(fn as IIslCommand, seen) { cmd ->
                if (cmd is BaseCommand && cmd.coverageStatementId != 0) {
                    val p = cmd.token.position
                    out.add(
                        CoverableStatementMeta(
                            cmd.coverageStatementId,
                            cmd::class.simpleName ?: "?",
                            SourceCoverageSpan.fromPosition(p)
                        )
                    )
                }
            }
        }
        return out
    }

    private fun assignDeep(cmd: IIslCommand?, next: AtomicInteger, seen: IdentityHashMap<IIslCommand, Unit>) {
        walkCommandGraph(cmd, seen) { c ->
            if (c is BaseCommand && c.coverageStatementId == 0) {
                c.coverageStatementId = next.getAndIncrement()
            }
        }
    }

    /**
     * Depth-first walk of the command graph (shared subgraphs visited once per [seen] map).
     */
    internal fun walkCommandGraph(cmd: IIslCommand?, seen: IdentityHashMap<IIslCommand, Unit>, visit: (IIslCommand) -> Unit) {
        if (cmd == null) return
        if (seen.put(cmd, Unit) != null) return
        visit(cmd)
        when (cmd) {
            is FunctionDeclarationCommand -> walkCommandGraph(cmd.statements, seen, visit)
            is RecursiveFunctionDeclarationCommand -> walkCommandGraph(cmd.statements, seen, visit)
            is FunctionReturnCommandHandler -> walkCommandGraph(cmd.statements, seen, visit)
            is FunctionReturnCommand -> walkCommandGraph(cmd.returnExpression, seen, visit)
            is AssignPropertyCommand -> walkCommandGraph(cmd.value, seen, visit)
            is AssignDynamicPropertyCommand -> {
                walkCommandGraph(cmd.name, seen, visit)
                walkCommandGraph(cmd.value, seen, visit)
            }
            is AssignVariableCommand -> walkCommandGraph(cmd.value, seen, visit)
            is StatementsBuildCommand -> cmd.commands.forEach { walkCommandGraph(it, seen, visit) }
            is ObjectBuildCommand -> cmd.commands.forEach { walkCommandGraph(it, seen, visit) }
            is ConstantObjectBuildCommand -> Unit
            is SpreadCommand -> walkCommandGraph(cmd.variable, seen, visit)
            is AnnotationCommand -> {
                cmd.arguments.forEach { walkCommandGraph(it, seen, visit) }
                walkCommandGraph(cmd.nextCommand, seen, visit)
            }
            is ConditionCommand -> {
                walkEvaluable(cmd.branchCondition, seen, visit)
                walkCommandGraph(cmd.trueBranch, seen, visit)
                walkCommandGraph(cmd.falseBranch, seen, visit)
            }
            is ConditionExpressionCommand -> {
                walkEvaluable(cmd.left, seen, visit)
                walkEvaluable(cmd.right, seen, visit)
            }
            is SimpleConditionCommand -> {
                walkCommandGraph(cmd.left, seen, visit)
                walkCommandGraph(cmd.right, seen, visit)
            }
            is CoalesceCommand -> {
                walkCommandGraph(cmd.left, seen, visit)
                walkCommandGraph(cmd.right, seen, visit)
            }
            is WhileCommand -> {
                walkEvaluable(cmd.whileCondition, seen, visit)
                walkCommandGraph(cmd.whileMaxLoops, seen, visit)
                walkCommandGraph(cmd.statements, seen, visit)
            }
            is ForEachCommand -> {
                walkCommandGraph(cmd.foreachSource, seen, visit)
                walkCommandGraph(cmd.statements, seen, visit)
            }
            is ParallelForEachCommand -> {
                walkCommandGraph(cmd.parallelOptions, seen, visit)
                walkCommandGraph(cmd.foreachSource, seen, visit)
                walkCommandGraph(cmd.statements, seen, visit)
            }
            is SwitchCaseCommand -> {
                walkCommandGraph(cmd.value, seen, visit)
                cmd.cases.forEach { walkCommandGraph(it, seen, visit) }
            }
            is SwitchCaseCommand.SwitchCaseBranchCommand -> {
                walkCommandGraph(cmd.right, seen, visit)
                walkCommandGraph(cmd.result, seen, visit)
            }
            is HashDispatchSwitchCommand -> {
                walkCommandGraph(cmd.value, seen, visit)
                cmd.forEachArmCommand { walkCommandGraph(it, seen, visit) }
            }
            is StatementFunctionCallCommand -> {
                cmd.statementArguments.forEach { walkCommandGraph(it, seen, visit) }
                walkCommandGraph(cmd.statementBody, seen, visit)
            }
            is FunctionCallCommand -> cmd.callArguments.forEach { walkCommandGraph(it, seen, visit) }
            is GenericConditionalModifierCommand -> {
                walkCommandGraph(cmd.value, seen, visit)
                walkEvaluable(cmd.expression, seen, visit)
                cmd.arguments.forEach { walkCommandGraph(it, seen, visit) }
            }
            is PotentialGenericConditionalModifierCommand -> {
                walkCommandGraph(cmd.value, seen, visit)
                walkEvaluable(cmd.expression, seen, visit)
                cmd.modifierArgumentCommands.forEach { walkCommandGraph(it, seen, visit) }
            }
            is ModifierValueCommand -> {
                walkCommandGraph(cmd.value, seen, visit)
                cmd.modifierArgumentCommands.forEach { walkCommandGraph(it, seen, visit) }
            }
            is FilterModifierValueCommand -> {
                walkCommandGraph(cmd.filterSource, seen, visit)
                walkEvaluable(cmd.filterExpression, seen, visit)
            }
            is MapModifierValueCommand -> {
                walkCommandGraph(cmd.mapPreviousValue, seen, visit)
                walkCommandGraph(cmd.mapArgument, seen, visit)
            }
            is ReduceModifierValueCommand -> {
                walkCommandGraph(cmd.reduceSource, seen, visit)
                walkCommandGraph(cmd.reduceArgument, seen, visit)
            }
            is FilterMapModifierValueCommand -> {
                walkCommandGraph(cmd.filterMapSource, seen, visit)
                walkEvaluable(cmd.filterMapPredicate, seen, visit)
                walkCommandGraph(cmd.filterMapMapArgument, seen, visit)
            }
            is ConditionModifierValueCommand -> {
                walkCommandGraph(cmd.value, seen, visit)
                walkEvaluable(cmd.expression, seen, visit)
                walkCommandGraph(cmd.trueModifier, seen, visit)
            }
            is VariableSimpleSelectorCommand -> walkEvaluable(cmd.indexCondition, seen, visit)
            is VariablePropertySelectorCommand -> {
                walkCommandGraph(cmd.propertyPrevious, seen, visit)
                walkEvaluable(cmd.propertyIndexCondition, seen, visit)
            }
            is VariableSelectorValueCommand -> Unit
            is ArrayCommand -> cmd.elementCommands.forEach { walkCommandGraph(it, seen, visit) }
            is InterpolateCommand -> cmd.interpolationParts.forEach { walkCommandGraph(it, seen, visit) }
            is MathExpressionCommand -> {
                walkCommandGraph(cmd.left, seen, visit)
                walkCommandGraph(cmd.right, seen, visit)
            }
            is LiteralValueCommand, is NoopCommand -> Unit
            else -> Unit
        }
    }

    private fun walkEvaluable(e: IEvaluableConditionCommand?, seen: IdentityHashMap<IIslCommand, Unit>, visit: (IIslCommand) -> Unit) {
        when (e) {
            is IIslCommand -> walkCommandGraph(e, seen, visit)
            null -> Unit
        }
    }
}
