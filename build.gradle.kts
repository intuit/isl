plugins {
    kotlin("jvm") version "2.1.10" apply false
    id("org.jetbrains.kotlin.plugin.allopen") version "2.1.10" apply false
    id("com.vanniktech.maven.publish") version "0.35.0" apply false
}

group = "com.intuit.isl"
version = project.property("version") as String

allprojects {
    repositories {
        mavenCentral()
    }
}

allprojects {
    group = "com.intuit.isl"
    version = rootProject.version
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")
    // Apply publishing plugin to library modules only

    if (name in listOf("isl-transform", "isl-validation", "isl-cmd")) {
        apply(plugin = "com.vanniktech.maven.publish")
        
        // Apply signing plugin if credentials are available
        val signingKeyEnv = System.getenv("SIGNING_KEY")
        val signingKeyFileEnv = System.getenv("SIGNING_KEY_FILE")
        val signingPasswordEnv = System.getenv("SIGNING_PASSWORD")
        val signingPasswordFileEnv = System.getenv("SIGNING_PASSWORD_FILE")
        
        val hasSigningKey = signingKeyEnv != null || signingKeyFileEnv != null
        val hasSigningPassword = signingPasswordEnv != null || signingPasswordFileEnv != null
        var signingConfigured = false
        
println(">>>>>>")
println(">>>>>>")
println(">>>>>>")
println(">>>>>> $signingKeyFileEnv")

        if (hasSigningKey && hasSigningPassword) {
            apply(plugin = "signing")

            configure<org.gradle.plugins.signing.SigningExtension> {
                // Read signing key from environment variable or file
                val signingKeyContent = when {
                    signingKeyEnv != null -> signingKeyEnv
                    signingKeyFileEnv != null -> {
                        val keyFile = file(signingKeyFileEnv)
                        if (keyFile.exists()) {
                            keyFile.readText(Charsets.UTF_8).trim()
                        } else {
                            throw GradleException("Signing key file not found: $signingKeyFileEnv")
                        }
                    }
                    else -> throw GradleException("Neither SIGNING_KEY nor SIGNING_KEY_FILE is set")
                }
                

println(">>>> Key=$signingKeyContent")

                // Read signing password from environment variable or file
                val signingPassword = when {
                    signingPasswordEnv != null -> signingPasswordEnv
                    signingPasswordFileEnv != null -> {
                        val passwordFile = file(signingPasswordFileEnv)
                        if (passwordFile.exists()) {
                            passwordFile.readText(Charsets.UTF_8).trim()
                        } else {
                            throw GradleException("Signing password file not found: $signingPasswordFileEnv")
                        }
                    }
                    else -> ""
                }
                
                // Decode base64 to ASCII-armored format if needed
                //val signingKey = when {
                    // If already ASCII-armored (starts with PGP header), use directly
                    // signingKeyContent.trim().startsWith("-----BEGIN PGP") -> {
                        // Preserve the exact format including newlines
                        val signingKey = signingKeyContent.trim()
                    // }
                    // // Otherwise, try to decode as base64
                    // else -> {
                    //     try {
                    //         // Remove all whitespace (spaces, newlines, tabs) from base64 string before decoding
                    //         val cleanBase64 = signingKeyContent.replace(Regex("\\s+"), "")
                            
                    //         // Decode base64
                    //         val decodedBytes = java.util.Base64.getDecoder().decode(cleanBase64)
                    //         val decoded = String(decodedBytes, Charsets.UTF_8)
                            
                    //         // Verify decoded content is a valid GPG secret key
                    //         if (decoded.contains("-----BEGIN PGP PRIVATE KEY BLOCK-----") || 
                    //             decoded.contains("-----BEGIN PGP SECRET KEY BLOCK-----")) {
                    //             // Preserve the exact format - don't trim excessively, just normalize
                    //             decoded.trim()
                    //         } else {
                    //             throw GradleException(
                    //                 "Decoded key does not appear to be a GPG secret key. " +
                    //                 "Expected '-----BEGIN PGP PRIVATE KEY BLOCK-----' or '-----BEGIN PGP SECRET KEY BLOCK-----'. " +
                    //                 "Got: ${decoded.take(100)}..."
                    //             )
                    //         }
                    //     } catch (e: java.lang.IllegalArgumentException) {
                    //         // Base64 decoding failed - might be malformed base64 or already ASCII-armored
                    //         // Try using as-is if it looks like it might be a key
                    //         if (signingKeyContent.contains("BEGIN") || signingKeyContent.contains("END")) {
                    //             signingKeyContent.trim()
                    //         } else {
                    //             throw GradleException(
                    //                 "Signing key appears to be base64-encoded but decoding failed: ${e.message}. " +
                    //                 "Please ensure the key is either ASCII-armored (starts with -----BEGIN PGP) " +
                    //                 "or valid base64-encoded ASCII-armored key."
                    //             )
                    //         }
                    //     } catch (e: Exception) {
                    //         throw GradleException("Failed to process signing key: ${e.message}", e)
                    //     }
                    // }
                //}
                
                // Verify the final key is a secret key (required for signing)
                // if (!signingKey.contains("PRIVATE KEY") && !signingKey.contains("SECRET KEY")) {
                //     throw GradleException(
                //         "Signing key must be a GPG PRIVATE/SECRET key, not a public key. " +
                //         "The key should start with '-----BEGIN PGP PRIVATE KEY BLOCK-----' or '-----BEGIN PGP SECRET KEY BLOCK-----'"
                //     )
                // }
                
                // // Validate key structure
                // if (!signingKey.startsWith("-----BEGIN PGP")) {
                //     throw GradleException("Signing key must start with '-----BEGIN PGP'")
                // }
                // if (!signingKey.contains("-----END PGP")) {
                //     throw GradleException("Signing key must contain '-----END PGP' footer")
                // }
                
println(">>>> Signing key format check:")
println(">>>>   Key length: ${signingKey.length} bytes")
println(">>>>   Key starts with: ${signingKey.take(60)}...")
println(">>>>   Key ends with: ...${signingKey.takeLast(60)}")
println(">>>>   Is secret key: ${signingKey.contains("PRIVATE KEY") || signingKey.contains("SECRET KEY")}")
println(">>>>   Has proper header: ${signingKey.startsWith("-----BEGIN PGP")}")
println(">>>>   Has proper footer: ${signingKey.contains("-----END PGP")}")
println(">>>>   Password length: ${signingPassword.length}")
println(">>>>   Password is empty: ${signingPassword.isEmpty()}")

                // Normalize line endings to Unix format (LF only)
                // BouncyCastle is sensitive to line ending format
                val normalizedKey = signingKey
                    .replace("\r\n", "\n")  // Windows CRLF -> LF
                    .replace("\r", "\n")     // Old Mac CR -> LF
                    .trim()                  // Remove leading/trailing whitespace
                
                // Ensure key ends with newline (some parsers expect this)
                val finalKey = if (normalizedKey.endsWith("\n")) normalizedKey else "$normalizedKey\n"
                
                try {
                    println(">>>>>>>>>>>>>>>>>> Use In Memory")
                    useInMemoryPgpKeys(finalKey, signingPassword)
                    println(">>>>>>>>>>>>>>>>>> DONE Use In Memory")
                    signingConfigured = true
                    
                } catch (e: Exception) {
                    // Check if it's a BouncyCastle PGPException (checksum or other PGP errors)
                    val exceptionClassName = e.javaClass.name
                    val isPgpException = exceptionClassName.contains("bouncycastle") && 
                                        exceptionClassName.contains("PGPException")
                    val isChecksumError = e.message?.contains("checksum", ignoreCase = true) == true
                    
                    if (isPgpException && isChecksumError) {
                        throw GradleException(
                            "GPG key checksum mismatch. This usually means:\n" +
                            "1. The signing key is corrupted or incomplete\n" +
                            "2. The passphrase is incorrect\n" +
                            "3. The key was incorrectly encoded/decoded\n\n" +
                            "Please verify:\n" +
                            "- The key is a complete PRIVATE/SECRET key (not public)\n" +
                            "- The key was exported correctly: gpg --export-secret-keys --armor KEY_ID\n" +
                            "- If base64-encoded, ensure it's the full key, properly encoded\n" +
                            "- The passphrase matches the key\n\n" +
                            "Original error: ${e.message}",
                            e
                        )
                    } else if (isPgpException) {
                        // Other PGP-related errors
                        throw GradleException(
                            "GPG key processing error: ${e.message}\n\n" +
                            "This might indicate:\n" +
                            "- Invalid key format\n" +
                            "- Corrupted key data\n" +
                            "- Unsupported key algorithm\n\n" +
                            "Please verify the key is valid and properly formatted.",
                            e
                        )
                    } else {
                        // Re-throw other exceptions as-is
                        throw e
                    }
                }
            }
        }

        // Configure publishing for library modules
        configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            val mavenCentralUsername = System.getenv("MAVEN_CENTRAL_USERNAME") ?: project.findProperty("mavenCentralUsername") as String?
            val mavenCentralPassword = System.getenv("MAVEN_CENTRAL_PASSWORD") ?: project.findProperty("mavenCentralPassword") as String?
            
            println(">>>> Connecting to MAVEN $mavenCentralUsername : $mavenCentralPassword")

            // Only configure Maven Central publishing if credentials are available
            // The plugin reads credentials from MAVEN_CENTRAL_USERNAME and MAVEN_CENTRAL_PASSWORD env vars
            if (mavenCentralUsername != null && mavenCentralPassword != null) {
                publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
            }
            
            // Only sign if signing was configured (key loaded successfully)
            // signAllPublications() must be called AFTER useInMemoryPgpKeys() has been executed
            if (hasSigningKey && hasSigningPassword && signingConfigured) {
                println(">>>> Signing all publications (signingConfigured=$signingConfigured)")
                signAllPublications()
                println(">>>> DONE SIGNING all publications")
            } else {
                println(">>>> Skipping signing - signing not configured (hasKey=$hasSigningKey, hasPassword=$hasSigningPassword, configured=$signingConfigured)")
            }

            // Configure additional repositories for publishing
            repositories {
                mavenLocal()
            }

            pom {
                name.set("ISL")
                description.set("ISL - JSON transformation scripting language")
                url.set("https://github.com/intuit/isl")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("intuit")
                        name.set("Intuit Inc.")
                        email.set("opensource@intuit.com")
                    }
                }

                scm {
                    url.set("https://github.com/intuit/isl")
                    connection.set("scm:git:https://github.com/intuit/isl.git")
                    developerConnection.set("scm:git:https://github.com/intuit/isl.git")
                }
            }
        }
    }

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        withSourcesJar()
    }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

// Publishing configuration is handled in subprojects that have the plugin applied

