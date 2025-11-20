package com.intuit.isl

import com.intuit.isl.utils.IIslReference
import java.security.Key
/**
 * Java Key wrapper class for isl
 */
class IslSecurityKey(val key: Key) : IIslReference {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IslSecurityKey

        if (key != other) false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}