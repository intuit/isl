package com.intuit.isl.parser

import com.intuit.isl.runtime.TransformCompilationException
import com.intuit.isl.utils.Position
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import java.util.*

class ParserErrorListener(val fileName: String, val contents: String) : ANTLRErrorListener {
    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        when (offendingSymbol) {
            is Token ->
                throw TransformCompilationException(
                    "Failed parsing `${offendingSymbol.text}` > $msg",
                    Position(fileName, line, charPositionInLine), contents
                )
            else ->
                throw TransformCompilationException(
                    "Failed parsing ${offendingSymbol} $msg ${e.toString()}",
                    Position(fileName, line, charPositionInLine), contents
                )
        }
    }

    override fun reportAmbiguity(
        recognizer: Parser?,
        dfa: DFA?,
        startIndex: Int,
        stopIndex: Int,
        exact: Boolean,
        ambigAlts: BitSet?,
        configs: ATNConfigSet?
    ) {
        // ignore for now
    }

    override fun reportAttemptingFullContext(
        recognizer: Parser?,
        dfa: DFA?,
        startIndex: Int,
        stopIndex: Int,
        conflictingAlts: BitSet?,
        configs: ATNConfigSet?
    ) {
        // ignore for now
    }

    override fun reportContextSensitivity(
        recognizer: Parser?,
        dfa: DFA?,
        startIndex: Int,
        stopIndex: Int,
        prediction: Int,
        configs: ATNConfigSet?
    ) {
        // ignore for now
    }
}