package com.intuit.isl.commands

import com.intuit.isl.parser.tokens.IIslToken

abstract class BaseCommand(override val token: IIslToken, override var parent: IIslCommand? = null) :IIslCommand{
    override fun toString(): String {
        return token.toString();
    }
}