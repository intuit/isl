package com.intuit.isl.commands

import com.intuit.isl.parser.tokens.IIslToken

abstract class BaseCommand(override val token: IIslToken, override var parent: IIslCommand? = null) :IIslCommand{

    /**
     * Stable id for this command node within a compiled module (see [CoverageStatementIdAssigner], run only when needed).
     * Used by coverage-style hooks; 0 means unassigned until then.
     */
    var coverageStatementId: Int = 0
    override fun toString(): String {
        return token.toString();
    }
}