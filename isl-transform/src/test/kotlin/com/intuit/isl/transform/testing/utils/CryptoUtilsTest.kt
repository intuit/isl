package com.intuit.isl.transform.testing.utils

import com.intuit.isl.transform.testing.commands.BaseTransformTest
import com.intuit.isl.utils.crypto.CryptoUtils
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.stream.Stream
import kotlin.test.*
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

// Commented out - requires encrypted_payload.txt and related crypto key files which were removed
/*
class CryptoUtilsTest : BaseTransformTest() {

    companion object {
        @JvmStatic
        fun decryptKeyFormatData(): Stream<Arguments> =
                Stream.of(
                        Arguments.of("EC PEM format", "ec_private.pem"),
                        Arguments.of("EC JWK format", "ec_private.jwk"),
                        Arguments.of("EC JWKS format", "ec_private.jwks"),
                        Arguments.of("RSA PEM format", "rsa_private.pem")
                )

        @JvmStatic
        fun verifyKeyFormatData(): Stream<Arguments> =
                Stream.of(
                        Arguments.of("RSA JWK format", "rsa_public_standalone.jwk"),
                        Arguments.of("RSA PEM format", "rsa_public.pem"),
                        Arguments.of("RSA JWKS format", "rsa_public.jwks"),
                        Arguments.of("EC PEM format", "ec_public.pem")
                )

        @JvmStatic
        fun emptyInputErrorData(): Stream<Arguments> =
                Stream.of(
                        Arguments.of(
                                "decrypt",
                                "empty_payload",
                                "General error during JWE decryption"
                        ),
                        Arguments.of("verify", "empty_payload", "JWS verify failed"),
                        Arguments.of("decrypt", "empty_key", "Empty key material"),
                        Arguments.of("verify", "empty_key", "Empty key material")
                )

        @JvmStatic
        fun happyPathOperationsData(): Stream<Arguments> =
                Stream.of(
                        Arguments.of("decrypt", "EC", "existing EC encrypted payload"),
                        Arguments.of("decrypt", "RSA", "dynamically created RSA encrypted payload"),
                        Arguments.of("verify", "RSA", "existing RSA signed payload"),
                        Arguments.of("verify", "EC", "dynamically created EC signed payload")
                )

        @JvmStatic
        fun errorScenariosData(): Stream<Arguments> =
                Stream.of(
                        // Decrypt error scenarios
                        Arguments.of(
                                "decrypt",
                                "invalid_payload",
                                "General error during JWE decryption"
                        ),
                        Arguments.of(
                                "decrypt",
                                "malformed_key",
                                "General error during JWE decryption"
                        ),
                        Arguments.of(
                                "decrypt",
                                "wrong_key_type",
                                "General error during JWE decryption"
                        ),
                        // Verify error scenarios
                        Arguments.of("verify", "invalid_signature", "JWS verify failed"),
                        Arguments.of("verify", "malformed_key", "JWS verify failed"),
                        Arguments.of(
                                "verify",
                                "signature_verification_failure",
                                "JWS verify failed"
                        ),
                        Arguments.of(
                                "verify",
                                "incompatible_key_algorithm",
                                "is not compatible with"
                        )
                )

        @JvmStatic
        fun jweAllowlistNegativeData(): Stream<Arguments> =
                Stream.of(Arguments.of(JWEAlgorithm.ECDH_ES, EncryptionMethod.A128GCM))
    }

    private lateinit var encryptedPayload: String
    private lateinit var expectedDecryptedPayload: String
    private lateinit var decryptedNotVerifiedPayload: String
    private lateinit var ecPrivateKey: String
    private lateinit var ecPrivateKeyJwk: String
    private lateinit var rsaPublicKey: String
    private lateinit var rsaPublicKeyPem: String
    private lateinit var ecPublicKeyPem: String

    @BeforeEach
    fun setUp() {
        // Load test data from resources
        encryptedPayload = readResource("decryption/encrypted_payload.txt")
        expectedDecryptedPayload = readResource("decryption/decrypted_payload.txt")
        decryptedNotVerifiedPayload =
                readResource("decryption/decrypted_not_verified_payload.txt")

        // Load EC private keys for decryption (in different formats)
        ecPrivateKey = readResource("decryption/ec_private.pem")
        ecPrivateKeyJwk = readResource("decryption/ec_private.jwk")

        // Load RSA public keys for verification (in different formats)
        rsaPublicKey = readResource("decryption/rsa_public_standalone.jwk")
        rsaPublicKeyPem = readResource("decryption/rsa_public.pem")
        ecPublicKeyPem = readResource("decryption/ec_public.pem")
    }

    @ParameterizedTest(name = "decryptJwe should handle {0}")
    @MethodSource("decryptKeyFormatData")
    fun `decryptJwe should handle different key formats`(
            @Suppress("UNUSED_PARAMETER") formatName: String,
            keyFileName: String
    ) {
        val keyContent = readResource("decryption/$keyFileName")
        val keyType =
                if (keyFileName.startsWith("rsa_")) CryptoUtils.KeyType.RSA
                else CryptoUtils.KeyType.EC

        if (keyType == CryptoUtils.KeyType.EC) {
            val decrypted = CryptoUtils.decryptJwe(encryptedPayload, keyContent, keyType)

            assertNotNull(decrypted)
            assertTrue(decrypted.isNotEmpty())
            assertEquals(decryptedNotVerifiedPayload, decrypted)
        } else {
            val testPayload = "Test RSA payload for $keyFileName"
            val rsaEncrypted = CryptoHelpers.createRSAEncryptedPayload(testPayload, keyContent)
            val decrypted = CryptoUtils.decryptJwe(rsaEncrypted, keyContent, keyType)

            assertNotNull(decrypted)
            assertTrue(decrypted.isNotEmpty())
            assertTrue(decrypted.contains(testPayload))
        }
    }

    @ParameterizedTest(name = "verifyJws should handle {0}")
    @MethodSource("verifyKeyFormatData")
    fun `verifyJws should handle different key formats`(
            @Suppress("UNUSED_PARAMETER") formatName: String,
            keyFileName: String
    ) {
        val keyContent = readResource("decryption/$keyFileName")
        val keyType =
                if (keyFileName.startsWith("ec_")) CryptoUtils.KeyType.EC
                else CryptoUtils.KeyType.RSA

        if (keyType == CryptoUtils.KeyType.RSA) {
            val verified = CryptoUtils.verifyJws(decryptedNotVerifiedPayload, keyContent, keyType)

            assertNotNull(verified)
            assertTrue(verified.isNotEmpty())
            assertEquals(expectedDecryptedPayload, verified)
        } else {
            val ecPrivateKey = readResource("decryption/ec_private.pem")
            val testPayload = "Test EC payload for $keyFileName"
            val ecSigned = CryptoHelpers.createECSignedPayload(testPayload, ecPrivateKey)
            val verified = CryptoUtils.verifyJws(ecSigned, keyContent, keyType)

            assertNotNull(verified)
            assertTrue(verified.isNotEmpty())
            val decodedPayload = String(Base64.getDecoder().decode(verified))
            assertTrue(decodedPayload.contains(testPayload))
        }
    }

    @Test
    fun `verifyJws should reject alg none`() {
        val headerJson = "{" + "\"alg\":\"none\"" + "}"
        val headerB64 =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(headerJson.toByteArray(StandardCharsets.UTF_8))
        val payloadB64 =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString("none-payload".toByteArray(StandardCharsets.UTF_8))
        val compact = "$headerB64.$payloadB64."
        assertThrows(RuntimeException::class.java) {
            CryptoUtils.verifyJws(compact, rsaPublicKeyPem, CryptoUtils.KeyType.RSA)
        }
    }

    @ParameterizedTest(name = "decryptJwe should reject disallowed alg/enc {0}/{1}")
    @MethodSource("jweAllowlistNegativeData")
    fun `decryptJwe should reject disallowed alg enc`(alg: JWEAlgorithm, enc: EncryptionMethod) {
        val headerJson =
                "{" + "\"alg\":\"" + alg.name + "\"," + "\"enc\":\"" + enc.name + "\"" + "}"
        val headerB64 =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(headerJson.toByteArray(StandardCharsets.UTF_8))
        val ivB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(byteArrayOf(1, 2, 3))
        val ciphertextB64 =
                Base64.getUrlEncoder().withoutPadding().encodeToString(byteArrayOf(4, 5, 6))
        val tagB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(byteArrayOf(7, 8, 9))
        val jwe = "$headerB64..$ivB64.$ciphertextB64.$tagB64"
        val ecPriv = readResource("decryption/ec_private.pem")
        val ex =
                assertThrows(RuntimeException::class.java) {
                    CryptoUtils.decryptJwe(jwe, ecPriv, CryptoUtils.KeyType.EC)
                }
        assertTrue(ex.message!!.contains("Unsupported JWE header combination"))
    }

    @ParameterizedTest(name = "{0} should handle {1} gracefully")
    @MethodSource("emptyInputErrorData")
    fun `crypto operations should handle empty inputs gracefully`(
            operation: String,
            inputType: String,
            expectedErrorFragment: String
    ) {
        val exception =
                assertThrows(RuntimeException::class.java) {
                    when (operation) {
                        "decrypt" -> {
                            when (inputType) {
                                "empty_payload" ->
                                        CryptoUtils.decryptJwe(
                                                "",
                                                ecPrivateKey,
                                                CryptoUtils.KeyType.EC
                                        )
                                "empty_key" ->
                                        CryptoUtils.decryptJwe(
                                                encryptedPayload,
                                                "",
                                                CryptoUtils.KeyType.EC
                                        )
                                else ->
                                        throw IllegalArgumentException(
                                                "Unknown input type: $inputType"
                                        )
                            }
                        }
                        "verify" -> {
                            when (inputType) {
                                "empty_payload" ->
                                        CryptoUtils.verifyJws(
                                                "",
                                                rsaPublicKey,
                                                CryptoUtils.KeyType.RSA
                                        )
                                "empty_key" ->
                                        CryptoUtils.verifyJws(
                                                decryptedNotVerifiedPayload,
                                                "",
                                                CryptoUtils.KeyType.RSA
                                        )
                                else ->
                                        throw IllegalArgumentException(
                                                "Unknown input type: $inputType"
                                        )
                            }
                        }
                        else -> throw IllegalArgumentException("Unknown operation: $operation")
                    }
                }
        assertTrue(exception.message!!.contains(expectedErrorFragment))
    }

    @ParameterizedTest(name = "{0} should work with {1} - {2}")
    @MethodSource("happyPathOperationsData")
    fun `crypto operations should work correctly`(
            operation: String,
            keyType: String,
            @Suppress("UNUSED_PARAMETER") description: String
    ) {
        when (operation) {
            "decrypt" -> {
                when (keyType) {
                    "EC" -> {
                        val decrypted =
                                CryptoUtils.decryptJwe(
                                        encryptedPayload,
                                        ecPrivateKey,
                                        CryptoUtils.KeyType.EC
                                )
                        assertNotNull(decrypted)
                        assertTrue(decrypted.isNotEmpty())
                        assertEquals(decryptedNotVerifiedPayload, decrypted)
                    }
                    "RSA" -> {
                        val rsaPrivateKey = readResource("decryption/rsa_private.pem")
                        val testPayload = "Simple RSA test payload"
                        val rsaEncrypted =
                                CryptoHelpers.createRSAEncryptedPayload(testPayload, rsaPrivateKey)
                        val decrypted =
                                CryptoUtils.decryptJwe(
                                        rsaEncrypted,
                                        rsaPrivateKey,
                                        CryptoUtils.KeyType.RSA
                                )
                        assertNotNull(decrypted)
                        assertTrue(decrypted.isNotEmpty())
                        assertTrue(decrypted.contains(testPayload))
                    }
                }
            }
            "verify" -> {
                when (keyType) {
                    "RSA" -> {
                        val verified =
                                CryptoUtils.verifyJws(
                                        decryptedNotVerifiedPayload,
                                        rsaPublicKey,
                                        CryptoUtils.KeyType.RSA
                                )
                        assertNotNull(verified)
                        assertTrue(verified.isNotEmpty())
                        assertEquals(expectedDecryptedPayload, verified)
                    }
                    "EC" -> {
                        val ecPrivateKey = readResource("decryption/ec_private.pem")
                        val ecPublicKey = readResource("decryption/ec_public.pem")
                        val testPayload = "Simple EC test payload"
                        val ecSigned =
                                CryptoHelpers.createECSignedPayload(testPayload, ecPrivateKey)
                        val verified =
                                CryptoUtils.verifyJws(ecSigned, ecPublicKey, CryptoUtils.KeyType.EC)
                        assertNotNull(verified)
                        assertTrue(verified.isNotEmpty())
                        val decodedPayload = String(Base64.getDecoder().decode(verified))
                        assertTrue(decodedPayload.contains(testPayload))
                    }
                }
            }
        }
    }

    @ParameterizedTest(name = "{0} should handle {1} error gracefully")
    @MethodSource("errorScenariosData")
    fun `crypto operations should handle error scenarios gracefully`(
            operation: String,
            scenario: String,
            expectedErrorFragment: String
    ) {
        val exception =
                assertThrows(RuntimeException::class.java) {
                    when (operation) {
                        "decrypt" -> {
                            when (scenario) {
                                "invalid_payload" ->
                                        CryptoUtils.decryptJwe(
                                                "invalid_payload",
                                                ecPrivateKey,
                                                CryptoUtils.KeyType.EC
                                        )
                                "malformed_key" ->
                                        CryptoUtils.decryptJwe(
                                                encryptedPayload,
                                                "invalid_key",
                                                CryptoUtils.KeyType.EC
                                        )
                                "wrong_key_type" ->
                                        CryptoUtils.decryptJwe(
                                                encryptedPayload,
                                                ecPrivateKey,
                                                CryptoUtils.KeyType.RSA
                                        )
                                else ->
                                        throw IllegalArgumentException(
                                                "Unknown scenario: $scenario"
                                        )
                            }
                        }
                        "verify" -> {
                            when (scenario) {
                                "invalid_signature" ->
                                        CryptoUtils.verifyJws(
                                                "invalid_signature",
                                                rsaPublicKey,
                                                CryptoUtils.KeyType.RSA
                                        )
                                "malformed_key" ->
                                        CryptoUtils.verifyJws(
                                                decryptedNotVerifiedPayload,
                                                "invalid_key",
                                                CryptoUtils.KeyType.RSA
                                        )
                                "signature_verification_failure" -> {
                                    val invalidJws =
                                            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.invalid_signature"
                                    CryptoUtils.verifyJws(
                                            invalidJws,
                                            rsaPublicKey,
                                            CryptoUtils.KeyType.RSA
                                    )
                                }
                                "incompatible_key_algorithm" ->
                                        CryptoUtils.verifyJws(
                                                decryptedNotVerifiedPayload,
                                                rsaPublicKey,
                                                CryptoUtils.KeyType.EC
                                        )
                                else ->
                                        throw IllegalArgumentException(
                                                "Unknown scenario: $scenario"
                                        )
                            }
                        }
                        else -> throw IllegalArgumentException("Unknown operation: $operation")
                    }
                }
        assertTrue(exception.message!!.contains(expectedErrorFragment))
    }
}
*/
