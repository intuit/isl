package com.intuit.isl.utils

import com.intuit.isl.IslSecurityKey
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.IOperationContext
import com.intuit.isl.runtime.TransformException
import com.intuit.isl.utils.crypto.CryptoUtils
import java.io.ByteArrayInputStream
import java.security.Key
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoExtensions {
    fun registerExtensions(context: IOperationContext) {
        context.registerExtensionMethod("Modifier.Crypto.*", CryptoExtensions::crypto)
    }

    private fun crypto(context: FunctionExecuteContext): Any? {
        val type = ConvertUtils.tryToString(context.secondParameter) ?: ""
        // When adding new crypto methods, do not do any encode/decode in the method
        // Let the caller do the encode/decode so that method works on the byte array
        // and can support different encodings
        return when (type.lowercase()) {
            "sha256" -> sha(context, "SHA-256")
            "sha512" -> sha(context, "SHA-512")
            "sha1" -> sha(context, "SHA-1")
            "md5" -> md5(context)
            "hmacsha256" -> hmacSha(context, "HmacSHA256")
            "hmacsha384" -> hmacSha(context, "HmacSHA384")
            "hmacsha512" -> hmacSha(context, "HmacSHA512")
            "hmacsha1" -> hmacSha(context, "HmacSHA1")
            "rsasha256" -> rsasha256(context)
            "tokeystore" -> toKeyStore(context)
            "aes256gcm" -> aes256gcm(context)
            "decrypt" -> decrypt(context)
            "verify" -> verify(context)
            else -> throw TransformException("Unsupported crypto function=$type", context.command.token.position)
        }
    }

    private fun decrypt(context: FunctionExecuteContext): String {
        // JWE decryption using CryptoUtils.decryptJweToString
        val (payload, keyType, keyMaterial) = parseParams(
            context = context,
            missingPayloadMessage = "Missing JWE payload",
            missingKeyMessage = "Missing private key",
        )

        return try {
            CryptoUtils.decryptJwe(
                compactJwe = payload,
                privateKeyPemOrJwk = keyMaterial,
                keyType = keyType,
            )
        } catch (e: Exception) {
            throw TransformException("JWE decrypt failed: ${e.message}", context.command.token.position, e)
        }
    }

    private fun verify(context: FunctionExecuteContext): String {
        // JWS verification using CryptoUtils.verifyJws
        val (payload, keyType, keyMaterial) = parseParams(
            context = context,
            missingPayloadMessage = "Missing JWS payload",
            missingKeyMessage = "Missing public key",
        )

        return try {
            CryptoUtils.verifyJws(payload, keyMaterial, keyType)
        } catch (e: Exception) {
            throw TransformException("JWS verify failed: ${e.message}", context.command.token.position, e)
        }
    }


    private fun sha(context: FunctionExecuteContext, algorithm: String): Any? {
        val value = context.firstParameter;
        val stringValue = ConvertUtils.tryToString(value) ?: "";

        val sha = MessageDigest.getInstance(algorithm);

        sha.update(stringValue.toByteArray(Charsets.UTF_8));

        return sha.digest();
    }

    private fun md5(context: FunctionExecuteContext): Any? {
        val value = context.firstParameter;
        val stringValue = ConvertUtils.tryToString(value) ?: "";

        val md5 = MessageDigest.getInstance("MD5");

        md5.update(stringValue.toByteArray(Charsets.UTF_8));

        return md5.digest();
    }

    private fun hmacSha(context: FunctionExecuteContext, algorithm: String): Any? {
        return doHmac(context, algorithm);
    }

    private fun doHmac(context: FunctionExecuteContext, algorithm: String): Any? {
        val value = ConvertUtils.getByteArray(context.firstParameter);
        val key = ConvertUtils.getByteArray(context.thirdParameter);

        val mac = Mac.getInstance(algorithm);
        mac.init(SecretKeySpec(key, algorithm));

        val result = mac.doFinal(value);

        return result;
    }

    private fun rsasha256(context: FunctionExecuteContext): Any? {
        val value = ConvertUtils.getByteArray(context.firstParameter);
        // secondParameter is the name of `rsasha256`
        // third parameter is the key
        val password = ConvertUtils.tryToString(context.fourthParameter) ?: "";
        val keyAlias = ConvertUtils.tryToString(context.fifthParameter) ?: "";
        val key = readPrivateKey(
            context.thirdParameter, context.command.token.position,
            password, keyAlias
        );

        val signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update(value);
        return signer.sign();
    }

    private fun readPrivateKey(value: Any?, position: Position, password: String, keyAlias: String): PrivateKey {
        val keyStore = when (value) {
            is KeyStore -> value;
            is IslSecurityKeyStore -> value.keyStore;
            is ObjectRefNode -> {
                if (value.value is IslSecurityKeyStore) {
                    value.value.keyStore;
                } else {
                    null
                }
            }

            else -> null;
        }
        val privateKey = if (keyStore != null) {
            // maybe it's key
            keyStore.getKey(keyAlias, password.toCharArray()) as PrivateKey
        } else {
            // maybe the key is in the ref
            when (value) {
                is Key -> value as PrivateKey;  // when called $value | crypto.rsasha256 ( @.IDPS.GetPrivateKey(..) )
                is IslSecurityKey -> value.key as PrivateKey;
                is ObjectRefNode -> {
                    if (value.value is IslSecurityKey)
                        value.value.key as PrivateKey;
                    else
                        null;
                }

                else -> null;
            }
        }
        if (privateKey == null)
            throw TransformException("Parameter is not a Key or a KeyStore.", position);
        return privateKey;
    }

    private fun toKeyStore(context: FunctionExecuteContext): Any? {
        val keyBytes = Base64.getDecoder().decode(ConvertUtils.tryToString(context.firstParameter) ?: "");
        val keystoreType = ConvertUtils.tryToString(context.thirdParameter) ?: "";
        val password = ConvertUtils.tryToString(context.fourthParameter) ?: "";
        val keyStore = KeyStore.getInstance(keystoreType);
        ByteArrayInputStream(keyBytes).use { stream ->
            keyStore.load(stream, password.toCharArray());
        }
        return IslSecurityKeyStore(keyStore);
    }

    private fun aes256gcm(context: FunctionExecuteContext): Any {
        val encryptedText = ConvertUtils.getByteArray(context.firstParameter);
        val encodedKey = ConvertUtils.getByteArray(context.thirdParameter);
        val encodedIv = ConvertUtils.getByteArray(context.fourthParameter);
        val encodedTag = ConvertUtils.getByteArray(context.fifthParameter);

        val keySpec = SecretKeySpec(encodedKey, "AES")
        val gcmParameterSpec = GCMParameterSpec(128, encodedIv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec)
        val decryptedBytes = cipher.doFinal(encryptedText + encodedTag)
        return decryptedBytes
    }

    private data class ParsedParams(
        val payload: String,
        val keyType: CryptoUtils.KeyType,
        val keyMaterial: String,
    )

    private fun parseParams(
        context: FunctionExecuteContext,
        missingPayloadMessage: String,
        missingKeyMessage: String,
    ): ParsedParams {
        val position = context.command.token.position
        val payload = ConvertUtils.tryToString(context.firstParameter)
            ?: throw TransformException(missingPayloadMessage, position)
        val algorithmParam = ConvertUtils.tryToString(context.thirdParameter)
            ?: throw TransformException("Missing algorithm", position)
        val keyMaterial = ConvertUtils.tryToString(context.fourthParameter)
            ?: throw TransformException(missingKeyMessage, position)

        val keyType: CryptoUtils.KeyType = try {
            CryptoUtils.KeyType.valueOf(algorithmParam.trim().uppercase())
        } catch (e: IllegalArgumentException) {
            throw TransformException("Invalid algorithm: $algorithmParam", position)
        }

        return ParsedParams(
            payload = payload,
            keyType = keyType,
            keyMaterial = keyMaterial,
        )
    }
}