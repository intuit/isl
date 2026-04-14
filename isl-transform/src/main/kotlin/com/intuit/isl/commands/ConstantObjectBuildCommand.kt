package com.intuit.isl.commands

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.parser.tokens.IIslToken

/**
 * Object literal that was proven at compile time to depend only on constant sub-expressions.
 * [prototypeTemplate] is built once during compilation; each execution returns [prototypeTemplate.deepCopy]
 * so callers can mutate the result without affecting other runs.
 */
class ConstantObjectBuildCommand(
    token: IIslToken,
    private val prototypeTemplate: ObjectNode
) : BaseCommand(token) {

    internal val prototypeTemplateForSerialization: ObjectNode get() = prototypeTemplate

    override fun execute(executionContext: ExecutionContext): CommandResult {
        return CommandResult(prototypeTemplate.deepCopy())
    }

    override fun <T> visit(visitor: ICommandVisitor<T>): T {
        return visitor.visit(this)
    }
}
