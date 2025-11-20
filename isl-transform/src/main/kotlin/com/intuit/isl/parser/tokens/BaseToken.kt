package com.intuit.isl.parser.tokens

import com.intuit.isl.types.IslType
import com.intuit.isl.utils.Position

abstract class BaseToken(
    override val position: Position,
    override var islType: IslType? = null,
    type: String? = null,
) : IIslToken {
    override val type: String = type ?: this.javaClass.simpleName.replace("Token", "");
}
