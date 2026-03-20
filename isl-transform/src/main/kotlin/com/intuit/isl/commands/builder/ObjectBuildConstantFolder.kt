package com.intuit.isl.commands.builder

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.ArrayCommand
import com.intuit.isl.commands.AssignPropertyCommand
import com.intuit.isl.commands.CoalesceCommand
import com.intuit.isl.commands.ConstantObjectBuildCommand
import com.intuit.isl.commands.FunctionCallCommand
import com.intuit.isl.commands.InterpolateCommand
import com.intuit.isl.commands.LiteralValueCommand
import com.intuit.isl.commands.MathExpressionCommand
import com.intuit.isl.commands.ObjectBuildCommand
import com.intuit.isl.commands.IIslCommand
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.common.OperationContext
import kotlinx.coroutines.runBlocking

/**
 * Replaces [ObjectBuildCommand] with [ConstantObjectBuildCommand] when the object consists only of
 * property assignments whose values are compile-time-constant (literals, constant nested objects/arrays,
 * math on constants, string interpolation from literals only, etc.).
 */
object ObjectBuildConstantFolder {

    fun tryFold(cmd: ObjectBuildCommand): IIslCommand {
        if (!isConstantObjectBuild(cmd)) return cmd
        return try {
            val prototype = runBlocking {
                val ctx = ExecutionContext(OperationContext(), null)
                val r = cmd.executeAsync(ctx)
                r.value
            }
            when (prototype) {
                is ObjectNode -> ConstantObjectBuildCommand(cmd.token, prototype)
                else -> cmd
            }
        } catch (_: Exception) {
            cmd
        }
    }

    private fun isConstantObjectBuild(cmd: ObjectBuildCommand): Boolean =
        cmd.commands.all { child ->
            when (child) {
                is AssignPropertyCommand -> isConstantValue(child.value)
                else -> false
            }
        }

    private fun isConstantValue(cmd: IIslCommand): Boolean = when (cmd) {
        is LiteralValueCommand -> true
        is ConstantObjectBuildCommand -> true
        is ObjectBuildCommand -> isConstantObjectBuild(cmd)
        is ArrayCommand -> cmd.elementCommands.all { isConstantValue(it) }
        is MathExpressionCommand -> isConstantValue(cmd.left) && isConstantValue(cmd.right)
        is InterpolateCommand -> cmd.interpolationParts.all { isConstantValue(it) }
        is CoalesceCommand -> isConstantValue(cmd.left) && isConstantValue(cmd.right)
        is FunctionCallCommand -> false
        else -> false
    }
}
