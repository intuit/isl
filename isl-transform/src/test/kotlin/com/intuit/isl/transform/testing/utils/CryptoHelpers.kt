package com.intuit.isl.transform.testing.utils

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.RSAEncrypter
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.interfaces.ECPublicKey

/** Helper utilities for creating test crypto payloads (JWE/JWS) for testing purposes. */
object CryptoHelpers {

    /**
     * Create a simple JWE encrypted with RSA key for testing RSA decryption.
     *
     * @param payload The plaintext payload to encrypt
     * @param rsaPrivateKeyPem RSA private key in PEM format (used to extract public key for
     * encryption)
     * @return JWE compact serialization string
     */
    fun createRSAEncryptedPayload(payload: String, rsaPrivateKeyPem: String): String {
        val rsaKey = RSAKey.parseFromPEMEncodedObjects(rsaPrivateKeyPem)

        val header = JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM).build()

        val jweObject = JWEObject(header, Payload(payload))
        val encrypter = RSAEncrypter(rsaKey as RSAKey)
        jweObject.encrypt(encrypter)

        return jweObject.serialize()
    }

    /**
     * Create a JWE using arbitrary alg/enc for negative testing of allowlist. For EC: use ECDH-ES
     * with a non-allowed enc (e.g., A128GCM). For RSA, caller can pass RSA key.
     */
    fun createECEncryptedPayloadWithAlgEnc(
            payload: String,
            ecPublicKeyPem: String,
            alg: JWEAlgorithm,
            enc: EncryptionMethod
    ): String {
        val ecKey: ECKey = ECKey.parseFromPEMEncodedObjects(ecPublicKeyPem) as ECKey
        val header = JWEHeader.Builder(alg, enc).build()
        val jweObject = JWEObject(header, Payload(payload))
        val ecPublicKey = ecKey.toECPublicKey()
        val encrypter = ECDHEncrypter(ecPublicKey as ECPublicKey)
        jweObject.encrypt(encrypter)
        return jweObject.serialize()
    }

    /**
     * Create a simple JWS signed with EC key for testing EC signature verification.
     *
     * @param payload The plaintext payload to sign
     * @param ecPrivateKeyPem EC private key in PEM format for signing
     * @return JWS compact serialization string
     */
    fun createECSignedPayload(payload: String, ecPrivateKeyPem: String): String {
        val ecKey = ECKey.parseFromPEMEncodedObjects(ecPrivateKeyPem)

        val header = JWSHeader.Builder(JWSAlgorithm.ES256).build()
        val signedJWT = SignedJWT(header, JWTClaimsSet.Builder().claim("data", payload).build())

        val signer = ECDSASigner(ecKey as ECKey)
        signedJWT.sign(signer)

        return signedJWT.serialize()
    }
}
