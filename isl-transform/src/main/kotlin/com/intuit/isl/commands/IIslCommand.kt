package com.intuit.isl.commands

import com.intuit.isl.common.ExecutionContext
import com.intuit.isl.commands.builder.ICommandVisitor
import com.intuit.isl.parser.tokens.IIslToken

/**
 * A command is generally (not always) associated with an IXToken and can run it
 * All commands return a result that can be used or added to the parent.
 * 
 * Commands now execute synchronously on virtual threads for better performance.
 */
interface IIslCommand {
    val token: IIslToken;

    var parent: IIslCommand?;

    fun execute(executionContext: ExecutionContext): CommandResult
    fun <T> visit(visitor: ICommandVisitor<T>): T;
}