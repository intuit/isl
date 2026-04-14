package com.intuit.isl.runtime.serialization

import com.intuit.isl.commands.FunctionCallCommand
import com.intuit.isl.commands.HardwiredFunctionCallCommand
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ContextAwareExtensionMethod
import com.intuit.isl.runtime.ITransformer
import com.intuit.isl.runtime.TransformPackage
import com.intuit.isl.runtime.Transformer
import com.intuit.isl.parser.tokens.FunctionType
import java.util.IdentityHashMap
import java.util.TreeMap

/**
 * Resolves [HardwiredFunctionCallCommand.linkedCallback] after a package is loaded from pre-compiled protobuf,
 * mirroring [com.intuit.isl.commands.builder.ExecutionBuilder.findMethod] semantics.
 */
internal object TransformPackageLinker {

    fun link(pkg: TransformPackage) {
        for (moduleName in pkg.modules) {
            val transformer = pkg.getModule(moduleName) ?: continue
            val imports = buildImportMap(transformer, pkg)
            val seen = IdentityHashMap<IIslCommand, Unit>()
            for (fn in transformer.module.functions) {
                CoverageStatementIdAssignerWalk.walk(fn as IIslCommand, seen) { c ->
                    if (c is HardwiredFunctionCallCommand && c.linkedCallback == null) {
                        c.linkedCallback = resolveHardwired(
                            c.token.name.replace(".", ":"),
                            transformer,
                            imports,
                            pkg
                        )
                    }
                }
            }
        }
    }

    private fun buildImportMap(host: Transformer, pkg: TransformPackage): TreeMap<String, ITransformer> {
        val imports = TreeMap<String, ITransformer>(String.CASE_INSENSITIVE_ORDER)
        for (imp in host.module.imports) {
            val dep = pkg.getModule(imp.sourceName)
            if (dep != null) {
                imports[imp.name] = dep
            }
        }
        return imports
    }

    private fun resolveHardwired(
        targetKey: String,
        host: Transformer,
        imports: TreeMap<String, ITransformer>,
        pkg: TransformPackage
    ): ContextAwareExtensionMethod {
        val lower = targetKey.lowercase()
        val fromModule = lower.substringBefore(":")
        val functionName = lower.substringAfter(":")

        if (fromModule == "this") {
            val fn = host.module.getFunction(functionName)
                ?: throw IllegalStateException("Linked package missing this:$functionName in ${host.module.name}")
            return fn.getRunner()
        }
        if (fromModule == "modifier") {
            val mod = host.module.token.functions.find {
                it.functionName.equals(functionName, ignoreCase = true) && it.functionType == FunctionType.Modifier
            } ?: throw IllegalStateException("Missing modifier $functionName in ${host.module.name}")
            val fn = host.module.getFunction(mod.functionName)
                ?: throw IllegalStateException("Missing compiled modifier $functionName")
            return fn.getRunner()
        }

        val imported = imports[fromModule]
            ?: throw IllegalStateException("No import alias '$fromModule' in ${host.module.name} for hardwired $targetKey")
        val runner = (imported as Transformer).crossModuleExecuteFunction(functionName)
            ?: throw IllegalStateException("No function '$functionName' in imported module for $targetKey")
        return runner
    }
}

/**
 * Local copy of child-walk logic (same as [com.intuit.isl.commands.CoverageStatementIdAssigner] but public for linker).
 */
private object CoverageStatementIdAssignerWalk {
    fun walk(cmd: IIslCommand?, seen: IdentityHashMap<IIslCommand, Unit>, visit: (IIslCommand?) -> Unit) {
        if (cmd == null) return
        if (seen.put(cmd, Unit) != null) return
        visit(cmd)
        when (cmd) {
            is com.intuit.isl.commands.FunctionDeclarationCommand -> walk(cmd.statements, seen, visit)
            is com.intuit.isl.commands.RecursiveFunctionDeclarationCommand -> walk(cmd.statements, seen, visit)
            is com.intuit.isl.commands.FunctionReturnCommandHandler -> walk(cmd.statements, seen, visit)
            is com.intuit.isl.commands.FunctionReturnCommand -> walk(cmd.returnExpression, seen, visit)
            is com.intuit.isl.commands.AssignPropertyCommand -> walk(cmd.value, seen, visit)
            is com.intuit.isl.commands.AssignDynamicPropertyCommand -> {
                walk(cmd.name, seen, visit)
                walk(cmd.value, seen, visit)
            }
            is com.intuit.isl.commands.AssignVariableCommand -> walk(cmd.value, seen, visit)
            is com.intuit.isl.commands.StatementsBuildCommand -> cmd.commands.forEach { walk(it, seen, visit) }
            is com.intuit.isl.commands.ObjectBuildCommand -> cmd.commands.forEach { walk(it, seen, visit) }
            is com.intuit.isl.commands.ConstantObjectBuildCommand -> Unit
            is com.intuit.isl.commands.SpreadCommand -> walk(cmd.variable, seen, visit)
            is com.intuit.isl.commands.AnnotationCommand -> {
                cmd.arguments.forEach { walk(it, seen, visit) }
                walk(cmd.nextCommand, seen, visit)
            }
            is com.intuit.isl.commands.ConditionCommand -> {
                walkEvaluable(cmd.branchCondition, seen, visit)
                walk(cmd.trueBranch, seen, visit)
                walk(cmd.falseBranch, seen, visit)
            }
            is com.intuit.isl.commands.ConditionExpressionCommand -> {
                walkEvaluable(cmd.left, seen, visit)
                walkEvaluable(cmd.right, seen, visit)
            }
            is com.intuit.isl.commands.SimpleConditionCommand -> {
                walk(cmd.left, seen, visit)
                walk(cmd.right, seen, visit)
            }
            is com.intuit.isl.commands.CoalesceCommand -> {
                walk(cmd.left, seen, visit)
                walk(cmd.right, seen, visit)
            }
            is com.intuit.isl.commands.WhileCommand -> {
                walkEvaluable(cmd.whileCondition, seen, visit)
                walk(cmd.whileMaxLoops, seen, visit)
                walk(cmd.statements, seen, visit)
            }
            is com.intuit.isl.commands.ForEachCommand -> {
                walk(cmd.foreachSource, seen, visit)
                walk(cmd.statements, seen, visit)
            }
            is com.intuit.isl.commands.ParallelForEachCommand -> {
                walk(cmd.parallelOptions, seen, visit)
                walk(cmd.foreachSource, seen, visit)
                walk(cmd.statements, seen, visit)
            }
            is com.intuit.isl.commands.SwitchCaseCommand -> {
                walk(cmd.value, seen, visit)
                cmd.cases.forEach { walk(it, seen, visit) }
            }
            is com.intuit.isl.commands.SwitchCaseCommand.SwitchCaseBranchCommand -> {
                walk(cmd.right, seen, visit)
                walk(cmd.result, seen, visit)
            }
            is com.intuit.isl.commands.HashDispatchSwitchCommand -> {
                walk(cmd.value, seen, visit)
                cmd.forEachArmCommand { walk(it, seen, visit) }
            }
            is com.intuit.isl.commands.StatementFunctionCallCommand -> {
                cmd.statementArguments.forEach { walk(it, seen, visit) }
                walk(cmd.statementBody, seen, visit)
            }
            is FunctionCallCommand -> cmd.callArguments.forEach { walk(it, seen, visit) }
            is com.intuit.isl.commands.modifiers.GenericConditionalModifierCommand -> {
                walk(cmd.value, seen, visit)
                walkEvaluable(cmd.expression, seen, visit)
                cmd.arguments.forEach { walk(it, seen, visit) }
            }
            is com.intuit.isl.commands.modifiers.PotentialGenericConditionalModifierCommand -> {
                walk(cmd.value, seen, visit)
                walkEvaluable(cmd.expression, seen, visit)
                cmd.arguments.forEach { walk(it, seen, visit) }
                cmd.modifierArgumentCommands.forEach { walk(it, seen, visit) }
            }
            is com.intuit.isl.commands.modifiers.ModifierValueCommand -> {
                walk(cmd.value, seen, visit)
                cmd.modifierArgumentCommands.forEach { walk(it, seen, visit) }
            }
            is com.intuit.isl.commands.modifiers.FilterModifierValueCommand -> {
                walk(cmd.filterSource, seen, visit)
                walkEvaluable(cmd.filterExpression, seen, visit)
            }
            is com.intuit.isl.commands.modifiers.MapModifierValueCommand -> {
                walk(cmd.mapPreviousValue, seen, visit)
                walk(cmd.mapArgument, seen, visit)
            }
            is com.intuit.isl.commands.modifiers.ReduceModifierValueCommand -> {
                walk(cmd.reduceSource, seen, visit)
                walk(cmd.reduceArgument, seen, visit)
            }
            is com.intuit.isl.commands.modifiers.FilterMapModifierValueCommand -> {
                walk(cmd.filterMapSource, seen, visit)
                walkEvaluable(cmd.filterMapPredicate, seen, visit)
                walk(cmd.filterMapMapArgument, seen, visit)
            }
            is com.intuit.isl.commands.modifiers.ConditionModifierValueCommand -> {
                walk(cmd.value, seen, visit)
                walkEvaluable(cmd.expression, seen, visit)
                walk(cmd.trueModifier, seen, visit)
            }
            is com.intuit.isl.commands.VariableSimpleSelectorCommand ->
                walkEvaluable(cmd.indexCondition, seen, visit)
            is com.intuit.isl.commands.VariablePropertySelectorCommand -> {
                walk(cmd.propertyPrevious, seen, visit)
                walkEvaluable(cmd.propertyIndexCondition, seen, visit)
            }
            is com.intuit.isl.commands.VariableSelectorValueCommand -> Unit
            is com.intuit.isl.commands.ArrayCommand -> cmd.elementCommands.forEach { walk(it, seen, visit) }
            is com.intuit.isl.commands.InterpolateCommand -> cmd.interpolationParts.forEach { walk(it, seen, visit) }
            is com.intuit.isl.commands.MathExpressionCommand -> {
                walk(cmd.left, seen, visit)
                walk(cmd.right, seen, visit)
            }
            is com.intuit.isl.commands.LiteralValueCommand, is com.intuit.isl.commands.NoopCommand -> Unit
            else -> Unit
        }
    }

    private fun walkEvaluable(
        e: com.intuit.isl.commands.IEvaluableConditionCommand?,
        seen: IdentityHashMap<IIslCommand, Unit>,
        visit: (IIslCommand?) -> Unit
    ) {
        when (e) {
            is IIslCommand -> walk(e, seen, visit)
            null -> Unit
        }
    }
}
