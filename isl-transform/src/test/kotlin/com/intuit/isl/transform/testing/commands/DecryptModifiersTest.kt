package com.intuit.isl.transform.testing.commands

import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.run

// Commented out - requires encrypted_payload.txt and related crypto key files which were removed
/*
@Suppress("unused")
class DecryptModifiersTest : BaseTransformTest() {

    companion object {
        private lateinit var encryptedPayload: String
        private lateinit var expectedDecryptedNotVerifiedPayload: String
        private lateinit var expectedPayload: String
        private lateinit var privateKey: String
        private lateinit var publicKey: String

        @JvmStatic
        @BeforeAll
        fun setUp() {
            // Load test resources
            encryptedPayload = readResource("decryption/encrypted_payload.txt")
            expectedDecryptedNotVerifiedPayload =
                    readResource("decryption/decrypted_not_verified_payload.txt")
            expectedPayload = readResource("decryption/decrypted_payload.txt")
            privateKey = readResource("decryption/ec_private.pem")
            publicKey = readResource("decryption/rsa_public.jwk")
        }

        @JvmStatic
        fun decryptModifier(): Stream<Arguments> {
            /*
            Usage example for decrypt modifier:
            $payload = "some encrypted text"

            # For EC key type - for RSA private key, use "RSA" instead of "EC"
            $ecPrivateKey = "some private key - EC type"
            value: $payload | crypto.decrypt("EC", $ecPrivateKey)
            */
            val islScript: String =
                    """
                ${'$'}payload = "$encryptedPayload"
                ${'$'}ecPrivateKey = "$privateKey"
                value: ${'$'}payload | crypto.decrypt("EC", ${'$'}ecPrivateKey)
            """.trimIndent()
            return Stream.of(
                    // Pass the decrypt modifier - expected result is JSON object with string value
                    Arguments.of(
                            islScript,
                            "{\"value\":\"$expectedDecryptedNotVerifiedPayload\"}",
                            null
                    ),
            )
        }

        @JvmStatic
        fun verifyModifier(): Stream<Arguments> {
            /*
            Usage example for verify modifier:
            $payload = "some signed text"
            $publicKey = "some public key"
            value: $payload | crypto.verify($publicKey)
            */
            val islScript: String =
                    """
                ${'$'}payload = "$expectedDecryptedNotVerifiedPayload"
                ${'$'}publicKey = $publicKey
                value: ${'$'}payload | crypto.verify("RSA", ${'$'}publicKey)
            """.trimIndent()
            return Stream.of(
                    // Pass the verify modifier - expected result should be plain string
                    Arguments.of(islScript, "{\"value\":\"$expectedPayload\"}", null),
            )
        }

        @JvmStatic
        fun decryptAndVerifyModifier(): Stream<Arguments> {
            /*
            Usage example for decrypt modifier:
            ${'$'}payload = "some encrypted text"

            # For RSA verify key type - for EC public key, use "EC" instead of "RSA"
            ${'$'}publicKey = "some public key - RSA type"

            # For EC key type - for RSA private key, use "RSA" instead of "EC"
            ${'$'}ecPrivateKey = "private key - EC type"

            # Decrypt and verify the payload
            value: ${'$'}payload | crypto.decrypt("EC", ${'$'}ecPrivateKey) | crypto.verify("RSA", ${'$'}publicKey)

             */
            val islScript: String =
                    """
                ${'$'}payload = "$encryptedPayload"
                ${'$'}publicKey = $publicKey
                ${'$'}ecPrivateKey = "$privateKey"
                value: ${'$'}payload | crypto.decrypt("EC", ${'$'}ecPrivateKey) | crypto.verify("RSA", ${'$'}publicKey)
            """.trimIndent()
            return Stream.of(
                    // Pass the verify modifier - expected result should be plain string
                    Arguments.of(islScript, "{\"value\":\"$expectedPayload\"}", null),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("decryptModifier")
    fun runDecrypt(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        assertDoesNotThrow { run(script, expectedResult, map) }
    }

    @ParameterizedTest
    @MethodSource("verifyModifier")
    fun runVerify(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        assertDoesNotThrow { run(script, expectedResult, map) }
    }

    @ParameterizedTest
    @MethodSource("decryptAndVerifyModifier")
    fun runDecryptAndVerify(
            script: String,
            expectedResult: String,
            map: Map<String, Any?>? = null
    ) {
        assertDoesNotThrow { run(script, expectedResult, map) }
    }
}
*/
