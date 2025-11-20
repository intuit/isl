package com.intuit.isl.utils

import java.security.KeyStore

/**
 * Java KeyStore wrapper class for isl
 */
class IslSecurityKeyStore(val keyStore: KeyStore) : IIslReference {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IslSecurityKeyStore

        if (keyStore != other.keyStore) return false

        return true
    }

    override fun hashCode(): Int {
        return keyStore.hashCode()
    }
}