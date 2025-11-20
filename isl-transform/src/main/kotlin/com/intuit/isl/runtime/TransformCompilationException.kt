package com.intuit.isl.runtime

import com.intuit.isl.utils.Position

class TransformCompilationException(message: String?, val position: Position? = null, originalText: String? = null) :
    Exception("${processMessage(message, position, originalText)}\nat $position.") {
}

fun processMessage(message: String?, position: Position?, originalText: String?): String? {
    if (message == null || position == null || originalText == null)
        return message;
    // let's find the position to make it nicely
    val lines = originalText.lines();
    if (position.line > 0 && position.line <= lines.size) {
        val line = lines[position.line - 1];
        if (position.column > 0 && position.column < line.length) {
            val marker = "^".padStart(position.column + 1);
            val newMessage = "$message\n$line\n$marker";
            return newMessage;
        }
    }
    return message;
}
