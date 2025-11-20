package com.intuit.isl.utils.crypto

import com.intuit.isl.utils.JsonConvert
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.RSADecrypter
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import org.slf4j.LoggerFactory

object CryptoUtils {
    /**
     * Cryptographic utilities for JWE decryption and JWS verification.
     *
     * Supported JWS algorithms (whitelist): ES256/ES384/ES512 and RS256/RS384/RS512. The `none`
     * algorithm is explicitly rejected.
     *
     * Allowed JWE header combinations:
     * - EC keys: alg=ECDH-ES, enc=A256GCM
     * - RSA keys: alg=RSA-OAEP-256, enc=A256GCM
     */
    class DecryptionException(message: String, cause: Throwable? = null) :
            RuntimeException(message, cause)
    class VerificationException(message: String, cause: Throwable? = null) :
            RuntimeException(message, cause)
    class KeyParseException(message: String, cause: Throwable? = null) :
            RuntimeException(message, cause)

    private val logger = LoggerFactory.getLogger(CryptoUtils::class.java)

    private enum class KeyMaterialKind {
        JWK,
        JWK_SET,
        PEM,
    }

    enum class KeyType {
        EC,
        RSA,
    }

    private val EC_JWS_ALGORITHMS =
            setOf(JWSAlgorithm.ES256, JWSAlgorithm.ES384, JWSAlgorithm.ES512)
    private val RSA_JWS_ALGORITHMS =
            setOf(JWSAlgorithm.RS256, JWSAlgorithm.RS384, JWSAlgorithm.RS512)

    /**
     * Decrypt JWE using EC or RSA private key (JWK/JWKSet/PEM) and return the decrypted payload as
     * a string.
     * @param compactJwe JWE compact string
     * @param privateKeyPemOrJwk Private key in PEM, JWK, or JWKSet format
     * @param keyType Private key type (EC or RSA)
     * @return Decrypted payload string
     * @throws DecryptionException if decryption fails
     */
    fun decryptJwe(compactJwe: String, privateKeyPemOrJwk: String, keyType: KeyType): String =
            try {
                logger.debug("Starting JWE decryption using {}", keyType)
                val jweObject = JWEObject.parse(compactJwe)
                val headerAlg = jweObject.header.algorithm
                val headerEnc = jweObject.header.encryptionMethod

                // Enforce explicit JWE header allowlist before attempting decryption
                val allowed =
                        when (keyType) {
                            KeyType.EC ->
                                    ((headerAlg == JWEAlgorithm.ECDH_ES ||
                                            headerAlg == JWEAlgorithm.ECDH_ES_A256KW) &&
                                            headerEnc == EncryptionMethod.A256GCM)
                            KeyType.RSA ->
                                    headerAlg == JWEAlgorithm.RSA_OAEP_256 &&
                                            headerEnc == EncryptionMethod.A256GCM
                        }
                if (!allowed) {
                    throw DecryptionException(
                            "Unsupported JWE header combination alg=$headerAlg enc=$headerEnc for $keyType"
                    )
                }
                val decrypter =
                        when (keyType) {
                            KeyType.EC -> {
                                val ecPrivateKey = parseECPrivateKey(privateKeyPemOrJwk)
                                ECDHDecrypter(ecPrivateKey)
                            }
                            KeyType.RSA -> {
                                val rsaPrivateKey = parseRSAPrivateKey(privateKeyPemOrJwk)
                                RSADecrypter(rsaPrivateKey)
                            }
                        }

                logger.debug(
                        "Decrypting JWE with alg={}, enc={}",
                        jweObject.header.algorithm,
                        jweObject.header.encryptionMethod
                )
                jweObject.decrypt(decrypter)
                logger.debug("JWE decryption finished.")

                jweObject.payload.toString()
            } catch (e: JOSEException) {
                logger.error("Error during decryption", e)
                throw DecryptionException("Error during decryption", e)
            } catch (e: Exception) {
                logger.error("General error during JWE decryption", e)
                throw DecryptionException(
                        "General error during JWE decryption with keyType=$keyType: ${e.message}",
                        e,
                )
            }

    /**
     * Verify JWS using the provided public key material (RSA or EC; JWK/JWKSet/PEM). When a JWKSet
     * is provided, attempts verification against all compatible keys; fails only if none verify.
     * @param compactJws JWS compact string
     * @param publicKeyPemOrJwk Public key in PEM, JWK, or JWKSet format
     * @param keyType Key type (EC or RSA) - must match the JWS algorithm
     * @return Verified payload as Base64-encoded string (for legacy compatibility)
     * @throws VerificationException if verification fails or algorithm is incompatible
     */
    fun verifyJws(compactJws: String, publicKeyPemOrJwk: String, keyType: KeyType): String =
            try {
                val jwsObject = JWSObject.parse(compactJws)
                val alg = jwsObject.header.algorithm
                val headerKeyType = keyTypeFromJwsAlgorithm(alg)
                if (headerKeyType != keyType) {
                    throw VerificationException(
                            "JWS algorithm $alg is not compatible with $keyType"
                    )
                }

                val candidateKeys =
                        extractCandidatePublicKeysByKeyType(headerKeyType, publicKeyPemOrJwk)

                val verified =
                        candidateKeys.any { jwk ->
                            when (headerKeyType) {
                                KeyType.EC -> {
                                    if (jwk is ECKey) {
                                        val verifier =
                                                ECDSAVerifier(jwk.toECPublicKey() as ECPublicKey)
                                        jwsObject.verify(verifier)
                                    } else {
                                        false
                                    }
                                }
                                KeyType.RSA -> {
                                    if (jwk is RSAKey) {
                                        val verifier =
                                                RSASSAVerifier(jwk.toRSAPublicKey() as RSAPublicKey)
                                        jwsObject.verify(verifier)
                                    } else {
                                        false
                                    }
                                }
                            }
                        }

                if (!verified) throw VerificationException("JWS signature verification failed")

                Base64.getEncoder().encodeToString(jwsObject.payload.toBytes())
            } catch (e: Exception) {
                throw VerificationException("JWS verify failed: ${e.message}", e)
            }

    /**
     * Extract candidate public keys from the provided key material, filtered by the requested
     * [keyType].
     *
     * Supported input formats for [publicKeyData]:
     * - JWK Set (JWKS): JSON object containing a "keys" array
     * - Single JWK: JSON representation of an EC or RSA key
     * - PEM: PEM-encoded public key material
     *
     * For EC requests only EC keys are returned; for RSA requests only RSA keys are returned. If
     * the input contains no compatible keys, an empty list is returned.
     *
     * @param keyType The expected asymmetric key type to filter for (EC or RSA).
     * @param publicKeyData Raw key material (JWK, JWKSet, or PEM).
     * @return A list of JWKs compatible with [keyType]. May be empty when no matching keys exist.
     * @throws KeyParseException If the key material is empty or in an unsupported format.
     */
    private fun extractCandidatePublicKeysByKeyType(
            keyType: KeyType,
            publicKeyData: String
    ): List<JWK> {
        val data = publicKeyData.trim()
        val isEc = keyType == KeyType.EC
        val isRsa = keyType == KeyType.RSA

        return when (classifyKeyMaterial(data)) {
            KeyMaterialKind.JWK_SET -> {
                val set = JWKSet.parse(data)
                if (isEc) set.keys.filterIsInstance<ECKey>()
                else set.keys.filterIsInstance<RSAKey>()
            }
            KeyMaterialKind.JWK -> {
                val jwk = JWK.parse(data)
                when {
                    isEc && jwk is ECKey -> listOf(jwk)
                    isRsa && jwk is RSAKey -> listOf(jwk)
                    else -> emptyList()
                }
            }
            KeyMaterialKind.PEM -> {
                val jwk = JWK.parseFromPEMEncodedObjects(data)
                when {
                    isEc && jwk is ECKey -> listOf(jwk)
                    isRsa && jwk is RSAKey -> listOf(jwk)
                    else -> emptyList()
                }
            }
        }
    }

    /**
     * Classify the key material into a specific type.
     * @param input The key material to classify.
     * @return The key material type.
     * @throws KeyParseException If the key material is empty or in an unsupported format.
     */
    private fun classifyKeyMaterial(input: String): KeyMaterialKind {
        val data = input.trim()
        if (data.isEmpty()) throw KeyParseException("Empty key material")
        if (data.contains("-----BEGIN")) return KeyMaterialKind.PEM
        try {
            val node = JsonConvert.mapper.readTree(data)
            if (!node.isObject) {
                throw KeyParseException("Unsupported key material format: JSON must be an object")
            }
            val keysNode = node.get("keys")
            return if (keysNode != null && keysNode.isArray) KeyMaterialKind.JWK_SET
            else KeyMaterialKind.JWK
        } catch (e: Exception) {
            throw KeyParseException("Invalid JSON key material: ${e.message}", e)
        }
    }

    /**
     * Get the key type from the JWS algorithm.
     * @param alg The JWS algorithm.
     * @return The key type.
     * @throws VerificationException If the JWS algorithm is not supported.
     */
    private fun keyTypeFromJwsAlgorithm(alg: JWSAlgorithm): KeyType {
        return when (alg) {
            in EC_JWS_ALGORITHMS -> KeyType.EC
            in RSA_JWS_ALGORITHMS -> KeyType.RSA
            JWSAlgorithm.NONE -> throw VerificationException("JWS alg 'none' is not supported")
            else -> throw VerificationException("Unsupported JWS algorithm: $alg")
        }
    }

    /**
     * Parse the EC private key from the JWK set.
     * @param jwkSetJson The JWK set JSON.
     * @return The EC private key.
     * @throws KeyParseException If the JWK set does not contain an EC private key.
     */
    private fun parseECPrivateKeyFromJwkSet(jwkSetJson: String): ECPrivateKey {
        val jwkSet = JWKSet.parse(jwkSetJson)
        val chosen = jwkSet.keys.firstOrNull { it is ECKey && it.isPrivate } as? ECKey
        return (chosen ?: throw KeyParseException("No EC private key found in JWKSet"))
                .toECPrivateKey()
    }

    /**
     * Parse the EC private key from the JWK.
     * @param jwkJson The JWK JSON.
     * @return The EC private key.
     * @throws KeyParseException If the JWK is not an EC private key.
     */
    private fun parseECPrivateKeyFromJwk(jwkJson: String): ECPrivateKey {
        val jwk = JWK.parse(jwkJson)
        if (jwk is ECKey && jwk.isPrivate) return jwk.toECPrivateKey()
        throw KeyParseException("Provided JWK is not an EC private key")
    }

    /**
     * Parse the EC private key from the PEM.
     * @param pem The PEM.
     * @return The EC private key.
     * @throws KeyParseException If the PEM does not contain an EC private key.
     */
    private fun parseECPrivateKeyFromPem(pem: String): ECPrivateKey {
        val ecKey = ECKey.parseFromPEMEncodedObjects(pem)
        if (ecKey is ECKey && ecKey.isPrivate) return ecKey.toECPrivateKey()
        throw KeyParseException("PEM does not contain an EC private key")
    }

    /**
     * Parse the EC private key from the key data.
     * @param keyData The key data.
     * @return The EC private key.
     * @throws KeyParseException If the key data is not an EC private key.
     */
    private fun parseECPrivateKey(keyData: String): ECPrivateKey {
        return parsePrivateKeyByKind(
                keyData = keyData,
                fromJwkSet = ::parseECPrivateKeyFromJwkSet,
                fromJwk = ::parseECPrivateKeyFromJwk,
                fromPem = ::parseECPrivateKeyFromPem,
        )
    }

    /**
     * Parse the RSA private key from the JWK set.
     * @param jwkSetJson The JWK set JSON.
     * @return The RSA private key.
     * @throws KeyParseException If the JWK set does not contain an RSA private key.
     */
    private fun parseRSAPrivateKeyFromJwkSet(jwkSetJson: String): RSAPrivateKey {
        val jwkSet = JWKSet.parse(jwkSetJson)
        val chosen = jwkSet.keys.firstOrNull { it is RSAKey && it.isPrivate } as? RSAKey
        return (chosen ?: throw KeyParseException("No RSA private key found in JWKSet"))
                .toRSAPrivateKey()
    }

    /**
     * Parse the RSA private key from the JWK.
     * @param jwkJson The JWK JSON.
     * @return The RSA private key.
     * @throws KeyParseException If the JWK is not an RSA private key.
     */
    private fun parseRSAPrivateKeyFromJwk(jwkJson: String): RSAPrivateKey {
        val jwk = JWK.parse(jwkJson)
        if (jwk is RSAKey && jwk.isPrivate) return jwk.toRSAPrivateKey()
        throw KeyParseException("Provided JWK is not an RSA private key")
    }

    /**
     * Parse the RSA private key from the PEM.
     * @param pem The PEM.
     * @return The RSA private key.
     * @throws KeyParseException If the PEM does not contain an RSA private key.
     */
    private fun parseRSAPrivateKeyFromPem(pem: String): RSAPrivateKey {
        val rsaKey = RSAKey.parseFromPEMEncodedObjects(pem)
        if (rsaKey is RSAKey && rsaKey.isPrivate) return rsaKey.toRSAPrivateKey()
        throw KeyParseException("PEM does not contain an RSA private key")
    }

    /**
     * Parse the RSA private key from the key data.
     * @param keyData The key data.
     * @return The RSA private key.
     * @throws KeyParseException If the key data is not an RSA private key.
     */
    private fun parseRSAPrivateKey(keyData: String): RSAPrivateKey {
        return parsePrivateKeyByKind(
                keyData = keyData,
                fromJwkSet = ::parseRSAPrivateKeyFromJwkSet,
                fromJwk = ::parseRSAPrivateKeyFromJwk,
                fromPem = ::parseRSAPrivateKeyFromPem,
        )
    }

    /**
     * Parse the private key from the key data.
     * @param keyData The key data.
     * @param fromJwkSet The function to parse the private key from the JWK set.
     * @param fromJwk The function to parse the private key from the JWK.
     * @param fromPem The function to parse the private key from the PEM.
     * @return The private key.
     * @throws KeyParseException If the key data is not a private key.
     */
    private fun <T> parsePrivateKeyByKind(
            keyData: String,
            fromJwkSet: (String) -> T,
            fromJwk: (String) -> T,
            fromPem: (String) -> T,
    ): T {
        val trimmed = keyData.trim()
        return when (classifyKeyMaterial(trimmed)) {
            KeyMaterialKind.JWK_SET -> fromJwkSet(trimmed)
            KeyMaterialKind.JWK -> fromJwk(trimmed)
            KeyMaterialKind.PEM -> fromPem(trimmed)
        }
    }
}
