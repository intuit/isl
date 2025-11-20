package com.intuit.isl.transform.testing.commands

import com.intuit.isl.IslSecurityKey
import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.utils.IslSecurityKeyStore
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.FileInputStream
import java.nio.file.Paths
import java.security.KeyStore
import java.security.PrivateKey
import java.util.Base64
import java.util.stream.Stream

// We are trying to simulate various signature algorithms
@Suppress("unused")
class SignatureTest : BaseTransformTest() {
	companion object {
		@JvmStatic
		fun sha256(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					"r: \"\" | crypto.sha256 | to.hex",
					"""{ "r": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" }""",
					null
				),

				// default Base64 conversion
				Arguments.of(
					"r: \"\" | crypto.sha256",
					"""{ "r": "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=" }""",
					null
				),

				// explicit base64 conversion
				Arguments.of(
					"r: \"\" | crypto.sha256 | encode.base64",
					"""{ "r": "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=" }""",
					null
				),

				Arguments.of(
					"r: \"my sha test\" | crypto.sha256 | to.hex",
					"""{ "r": "77f03ce7057b27586d97cc432ac033b628494e651c90131c63ee8b651a6f9e18" }""",
					null
				),

				// HMAC Algorithms
				Arguments.of(
					"r: \"my sha test\" | crypto.hmacsha384(\"mykey\")",
					"""{ "r": "vf9f8K19036cMyc6T3GMO0uc6PQ9ust0NhNTRXwDdMgjIWV6uI6qNuZyoEQc8gAX" }""",
					null
				),

				Arguments.of(
					"r: \"my sha test\" | crypto.hmacsha512(\"mykey\")",
					"""{ "r": "234P4bI5itKmiiEI9dMZqh5Mbwua5irVjVE2EB1DV5s2OJmOdQo4n6ap9b0BjXgnx/WXLr9SBYWLLqXS8jOOtQ==" }""",
					null
				),

				// https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html

				Arguments.of(
// we are using # which will replace with $ as it's too complicated to embed $s in Kotlin strings
					"""
    #h = { host:"iam.amazonaws.com", "content-type": "application/x-www-form-urlencoded; charset=utf-8" } 
    #q = { "Action":"ListUsers", "Version":"2010-05-08" }
    #h.["x-amz-date"] = "20150830T123600Z";
    #h = #h | sort;
    #r = {
        method: "GET",
        path: "/",
        query: #q,
        headers: #h
    };
    #b = "";
    
    #toSign: [ 
        #r.method, 
        #r.path | encode.path,
        #q | join.query( "&", "=" ),
        #h | join.string( "\n", ":" ),
        #b, 
        #h | keys | join.string ( ";" ),
        #b | crypto.sha256 | to.hex
     ] | join.string( "\n" )
     
     @.Log.Info( #toSign );
     
    result: #toSign | crypto.sha256 | to.hex 
""".trimIndent().replace("#", "$"),
					"""{ "result": "f536975d06c0309214f805bb90ccff089219ecd68b2577efef23edd43b7e1a59" }""",
					null
				),
			);
		}

		@JvmStatic
		fun aes256gcm(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					"r: \"iH/L0g4ZPgONGOAosr5wOrVpkVofAGq4HNz23Jh0ZWM=\" | decode.base64 | crypto.aes256gcm( \"6RS0M1cpaBE5VKVrQJTbW02+aoF+W62IA7ItBNRkTXI=\" | decode.base64, \"OZLm5zHZYXuWUKDLpyCbww==\" | decode.base64, \"1DlEoR1wIeBPlN7/c+DOYw==\" | decode.base64 ) | to.string",
					"""{ "r": "Hello, this is a secret message!" }""",
					null
				),)
		}

		@JvmStatic
		fun sha512(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					"r: \"\" | crypto.sha512 | to.hex",
					"""{ "r": "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e" }""",
					null
				),

				// default Base64 conversion
				Arguments.of(
					"r: \"\" | crypto.sha512",
					"""{ "r": "z4PhNX7vuL3xVChQ1m2AB9Yg5AULVxXcg/SpIdNs6c5H0NE8XYXysP+DGNKHfuwvY7kxvUdBeoGlODJ6+SfaPg==" }""",
					null
				),

				// explicit base64 conversion
				Arguments.of(
					"r: \"\" | crypto.sha512 | encode.base64",
					"""{ "r": "z4PhNX7vuL3xVChQ1m2AB9Yg5AULVxXcg/SpIdNs6c5H0NE8XYXysP+DGNKHfuwvY7kxvUdBeoGlODJ6+SfaPg==" }""",
					null
				),

				Arguments.of(
					"r: \"my sha test\" | crypto.sha512 | to.hex",
					"""{ "r": "5e3c77245fa1ceeafccc815f2e34c65b39e00c85bcb95556d5ff4913fc8544aa461b604db1f5e5dd2b2fe37f84236bf263694cdd5adf4040faa10e1c37addbbb" }""",
					null
				)
			)
		}

		@JvmStatic
		fun sha1(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					"r: \"\" | crypto.sha1 | to.hex",
					"""{ "r": "da39a3ee5e6b4b0d3255bfef95601890afd80709" }""",
					null
				),

				// default Base64 conversion
				Arguments.of(
					"r: \"\" | crypto.sha1",
					"""{ "r": "2jmj7l5rSw0yVb/vlWAYkK/YBwk=" }""",
					null
				),

				// explicit base64 conversion
				Arguments.of(
					"r: \"\" | crypto.sha1 | encode.base64",
					"""{ "r": "2jmj7l5rSw0yVb/vlWAYkK/YBwk=" }""",
					null
				),

				Arguments.of(
					"r: \"my sha test\" | crypto.sha1 | to.hex",
					"""{ "r": "ec781dee9e4d75d5b77f66aba96d70fe9555df5a" }""",
					null
				)
			)
		}

		@JvmStatic
		fun hmacSha256(): Stream<Arguments> {
			// https://docs.aws.amazon.com/general/latest/gr/sigv4-calculate-signature.html
			return Stream.of(
				Arguments.of(
					"\$k = \"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY\";\n" +
							// Amazon silly magic
							"\$kDate =      \"20150830\" | crypto.hmacsha256 ( `AWS4\$k` );\n" +
							"\$kRegion =    \"us-east-1\" | crypto.hmacsha256 ( \$kDate );\n" +
							"\$kService =   \"iam\" | crypto.hmacsha256 ( \$kRegion );\n" +
							"\$p =          \"aws4_request\" | crypto.hmacsha256 ( \$kService );\n" +

							"b64: \$p\n" +
							"r: \$p | to.hex",
					// base 64 encoded (default for byte arrays)
					// and full hex encoded
					"""{ "b64":"xK+xzFdx2HF2Ojk+RLcDVxtVzChCTRpehtpu08FUpLk=",
                        "r": "c4afb1cc5771d871763a393e44b703571b55cc28424d1a5e86da6ed3c154a4b9"
                    }""".trimMargin(),
					null
				),

				// https://docs.aws.amazon.com/general/latest/gr/sigv4-calculate-signature.html
				Arguments.of(
					"\$k = \"c4afb1cc5771d871763a393e44b703571b55cc28424d1a5e86da6ed3c154a4b9\";\n" +
							"\$s = [ \n" +
							"   \"AWS4-HMAC-SHA256\", \n" +
							"   \"20150830T123600Z\", \n" +
							"   \"20150830/us-east-1/iam/aws4_request\", \n" +
							"   \"f536975d06c0309214f805bb90ccff089219ecd68b2577efef23edd43b7e1a59\" \n" +
							"] | join.string(\"\n\") ;\n" +
							"\$p = \$s | crypto.hmacsha256( \$k | hex.tobinary ) | to.hex;" +
							"r: \$p",
					"""{ "r": "5d672d79c15b13162d9279b0855cfba6789a8edb4c82c400e06b5924a6f2b5d7" }""",
					null
				),
			)
		}

		@JvmStatic
		fun hmacSha1(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					"r: \"my sha test\" | crypto.hmacsha1(\"mykey\")",
					"""{ "r": "kLWSj/tDxHynrE6b02ikGWRcnuM=" }""",
					null
				),
				
				Arguments.of(
					"r: \"my sha test\" | crypto.hmacsha1(\"mykey\") | to.hex",
					"""{ "r": "90b5928ffb43c47ca7ac4e9bd368a419645c9ee3" }""",
					null
				),
				
				Arguments.of(
					"r: \"\" | crypto.hmacsha1(\"mykey\")",
					"""{ "r": "W7nAZqM28ObxfX3axOQ956lKbJo=" }""",
					null
				)
			)
		}

		@JvmStatic
		fun md5(): Stream<Arguments> {
			return Stream.of(
				// Base64-encoded hash
				Arguments.of(
					"md5_hash: \"MD5 hash me\" | crypto.md5",
					"""{ "md5_hash": "RuYhRiGrN6V8jkGrtFP3UA==" }""",
					null
				),

				// Explicitly convert to base64
				Arguments.of("md5_hash: \"MD5 hash me\" | crypto.md5 | encode.base64",
					"""{ "md5_hash": "RuYhRiGrN6V8jkGrtFP3UA==" }""",
					null
				),

				// Explicitly convert to hexadecimal
				Arguments.of("md5_hash: \"MD5 hash me\" | crypto.md5 | to.hex",
					"""{ "md5_hash": "46e6214621ab37a57c8e41abb453f750" }""",
					null
				)
			);
		}

		@JvmStatic
		fun encoding(): Stream<Arguments> {
			return Stream.of(
				// Standard base64 encoding
				Arguments.of(
					"r: \"Hello World\" | encode.base64",
					"""{ "r": "SGVsbG8gV29ybGQ=" }""",
					null
				),

				// Base64 encoding without padding
				Arguments.of(
					"r: \"Hello World\" | encode.base64({ \"withoutPadding\": true })",
					"""{ "r": "SGVsbG8gV29ybGQ" }""",
					null
				),

				// Standard base64url encoding
				Arguments.of(
					"r: \"Hello World!!!\" | encode.base64url",
					"""{ "r": "SGVsbG8gV29ybGQhISE=" }""",
					null
				),

				// Base64url encoding without padding
				Arguments.of(
					"r: \"Hello World!!!\" | encode.base64url({ \"withoutPadding\": true })",
					"""{ "r": "SGVsbG8gV29ybGQhISE" }""",
					null
				),

				// Test with empty string
				Arguments.of(
					"r: \"\" | encode.base64",
					"""{ "r": "" }""",
					null
				),

				// Test with empty string without padding
				Arguments.of(
					"r: \"\" | encode.base64({ \"withoutPadding\": true })",
					"""{ "r": "" }""",
					null
				),

				// Test with special characters that need base64url
				Arguments.of(
					"r: \"???>\" | encode.base64url",
					"""{ "r": "Pz8_Pg==" }""",
					null
				),

				// Test with special characters that need base64url without padding
				Arguments.of(
					"r: \"???>\" | encode.base64url({ \"withoutPadding\": true })",
					"""{ "r": "Pz8_Pg" }""",
					null
				)
			)
		}

		@JvmStatic
		fun toKeyStore(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					"fun run() {\n" +
							"    \$privateKeyString = @.Test.loadPKCS12SigningKeyAsString();\n" +
							"    \$keyStore = \$privateKeyString | crypto.tokeystore(\"PKCS12\",\"keystorepassword\");\n" +
						    "	return { \"r\": \"my auth header\" | crypto.rsasha256(\$keyStore, \"keystorepassword\",\"keyalias\") | encode.base64 };\n" +
							"}",
					"""{ "r": "Xlbkb2R2FRePDRtgtQCz9CUkUKhA92g/IkcoI42PwaV84RH3sJ9fYKl34W6Jvy8J998LHxNrTu7GftgE/hQKMRGSphd2dF8jmVT2BwNeMOjYUTxQXd22gEGDZnIlVxr+HqQxj27Jdw2qXFUu6yEnhMqu5BP1LByIdtfuGJ22lPgVcAmz+LbgiDc6dMqWyFa5oQYUhN3qug/HcMlT1gmHnj/IW6qN+VT4P58XT/8WvzvLyEKVwS/2aqNpErigTu+POSIvrsg3clv5oUXPEf1p0ohlvMPy48ZffnKeJgjwwtCL09LQ3cOzy6eTjYkiSaYsNggdpOff7jhWclord55JGw==" }""",
					null
				))
			
		}

		@JvmStatic
		fun rsaSha256WithPrivateKey(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					"fun run() {\n" +
							"    \$privateKey = @.Test.loadPKCS12SigningKeyFromFileAsPrivateKey();\n" +
							"    return { \"r\": \"my auth header\" | crypto.rsasha256(\$privateKey, \"keystorepassword\",\"keyalias\") | encode.base64 };\n" +
							"}",
					"""{ "r": "Xlbkb2R2FRePDRtgtQCz9CUkUKhA92g/IkcoI42PwaV84RH3sJ9fYKl34W6Jvy8J998LHxNrTu7GftgE/hQKMRGSphd2dF8jmVT2BwNeMOjYUTxQXd22gEGDZnIlVxr+HqQxj27Jdw2qXFUu6yEnhMqu5BP1LByIdtfuGJ22lPgVcAmz+LbgiDc6dMqWyFa5oQYUhN3qug/HcMlT1gmHnj/IW6qN+VT4P58XT/8WvzvLyEKVwS/2aqNpErigTu+POSIvrsg3clv5oUXPEf1p0ohlvMPy48ZffnKeJgjwwtCL09LQ3cOzy6eTjYkiSaYsNggdpOff7jhWclord55JGw==" }""",
					null
				))
		}

		@JvmStatic
		fun rsaSha256WithPrivateKeyInIslSecurityKey(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					"fun run() {\n" +
							"    \$islKeyStore = @.Test.loadPKCS12SigningKeyAsIslKey();\n" +
							"    return { \"r\": \"my auth header\" | crypto.rsasha256(\$islKeyStore, \"keystorepassword\",\"keyalias\") | encode.base64 };\n" +
							"}",
					"""{ "r": "Xlbkb2R2FRePDRtgtQCz9CUkUKhA92g/IkcoI42PwaV84RH3sJ9fYKl34W6Jvy8J998LHxNrTu7GftgE/hQKMRGSphd2dF8jmVT2BwNeMOjYUTxQXd22gEGDZnIlVxr+HqQxj27Jdw2qXFUu6yEnhMqu5BP1LByIdtfuGJ22lPgVcAmz+LbgiDc6dMqWyFa5oQYUhN3qug/HcMlT1gmHnj/IW6qN+VT4P58XT/8WvzvLyEKVwS/2aqNpErigTu+POSIvrsg3clv5oUXPEf1p0ohlvMPy48ZffnKeJgjwwtCL09LQ3cOzy6eTjYkiSaYsNggdpOff7jhWclord55JGw==" }""",
					null
				))
		}

		@JvmStatic
		fun rsaSha256WithKeyStore(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					"fun run() {\n" +
							"    \$islKeyStore = @.Test.loadPKCS12SigningKeyAsKeyStore();\n" +
							"    return { \"r\": \"my auth header\" | crypto.rsasha256(\$islKeyStore, \"keystorepassword\",\"keyalias\") | encode.base64 };\n" +
							"}",
					"""{ "r": "Xlbkb2R2FRePDRtgtQCz9CUkUKhA92g/IkcoI42PwaV84RH3sJ9fYKl34W6Jvy8J998LHxNrTu7GftgE/hQKMRGSphd2dF8jmVT2BwNeMOjYUTxQXd22gEGDZnIlVxr+HqQxj27Jdw2qXFUu6yEnhMqu5BP1LByIdtfuGJ22lPgVcAmz+LbgiDc6dMqWyFa5oQYUhN3qug/HcMlT1gmHnj/IW6qN+VT4P58XT/8WvzvLyEKVwS/2aqNpErigTu+POSIvrsg3clv5oUXPEf1p0ohlvMPy48ZffnKeJgjwwtCL09LQ3cOzy6eTjYkiSaYsNggdpOff7jhWclord55JGw==" }""",
					null
				))
		}

		fun loadPKCS12SigningKeyAsKeyStore(context: FunctionExecuteContext): KeyStore? {
			val pkcs12KeyStore = loadSigningKey("./src/test/resources/keys/private_key.p12", "PKCS12", "keystorepassword");
			return pkcs12KeyStore
		}

		fun loadPKCS12SigningKeyFromFileAsIslSecurityKeyStore(context: FunctionExecuteContext): IslSecurityKeyStore? {
			val pkcs12KeyStore = loadSigningKey("./src/test/resources/keys/private_key.p12", "PKCS12", "keystorepassword");
			return IslSecurityKeyStore(pkcs12KeyStore)
		}

		fun loadPKCS12SigningKeyAsIslKey(context: FunctionExecuteContext): IslSecurityKey? {
			val pkcs12KeyStore = loadSigningKey("./src/test/resources/keys/private_key.p12", "PKCS12", "keystorepassword");
			val privateKey = pkcs12KeyStore.getKey("keyalias", "keystorepassword".toCharArray()) as PrivateKey
			return IslSecurityKey(privateKey)
		}

		fun loadPKCS12SigningKeyFromFileAsString(context: FunctionExecuteContext): String? {
			val stream = openFileStream("./src/test/resources/keys/private_key.p12")
			val text = Base64.getEncoder().encodeToString(stream.readAllBytes())
			return text;
		}

		fun loadPKCS12SigningKeyFromFileAsPrivateKey(context: FunctionExecuteContext): PrivateKey? {
			val pkcs12KeyStore = loadSigningKey("./src/test/resources/keys/private_key.p12", "PKCS12", "keystorepassword");
			val privateKey = pkcs12KeyStore.getKey("keyalias", "keystorepassword".toCharArray()) as PrivateKey
			return privateKey
		}

		private fun loadSigningKey(pkcs12KeyFilePath: String, keystoreType:String, password:String): KeyStore  {
			// Add to IslSecurityKey
			val keystore = KeyStore.getInstance(keystoreType)
            openFileStream(pkcs12KeyFilePath).use { stream ->
                keystore.load(stream, password.toCharArray())
            }
			return keystore
		}

		private fun openFileStream(filePath: String): FileInputStream {
			val file = Paths.get(filePath)
			return FileInputStream(file.toFile())
		}
	}


	@ParameterizedTest
	@MethodSource(
		"sha256",
		"sha512",
		"sha1",
		"hmacSha256",
		"hmacSha1",
		"md5",
		// Commented out - requires private_key.p12 file which was removed
		// "rsasha256",
		"encoding",
		// Commented out - requires private_key.p12 file which was removed
		// "toKeyStore",
		// "rsaSha256WithPrivateKey",
		// "rsaSha256WithPrivateKeyInIslSecurityKey",
		// "rsaSha256WithKeyStore",
		"aes256gcm"
	)
	fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
		run(script, expectedResult, map);
	}

	// supply key in the context to run the test?

	override fun onRegisterExtensions(context: OperationContext) {
		context.registerExtensionMethod("Test.loadPKCS12SigningKeyAsIslKeyStore", SignatureTest::loadPKCS12SigningKeyFromFileAsIslSecurityKeyStore);
		context.registerExtensionMethod("Test.loadPKCS12SigningKeyAsString", SignatureTest::loadPKCS12SigningKeyFromFileAsString);
		context.registerExtensionMethod("Test.loadPKCS12SigningKeyFromFileAsPrivateKey", SignatureTest::loadPKCS12SigningKeyFromFileAsPrivateKey);
		context.registerExtensionMethod("Test.loadPKCS12SigningKeyAsIslKey", SignatureTest::loadPKCS12SigningKeyAsIslKey);
		context.registerExtensionMethod("Test.loadPKCS12SigningKeyAsKeyStore", SignatureTest::loadPKCS12SigningKeyAsKeyStore);
		super.onRegisterExtensions(context)
	}
}