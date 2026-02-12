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

// Configure signing for library modules
configure(subprojects.filter { it.name in listOf("isl-transform", "isl-validation", "isl-cmd") }) {
    // Apply signing plugin if credentials are available
    val signingKeyEnv = System.getenv("SIGNING_KEY")
    val signingKeyFileEnv = System.getenv("SIGNING_KEY_FILE")
    val signingPasswordEnv = System.getenv("SIGNING_PASSWORD")
    val signingPasswordFileEnv = System.getenv("SIGNING_PASSWORD_FILE")
    
    val hasSigningKey = signingKeyEnv != null || signingKeyFileEnv != null
    val hasSigningPassword = signingPasswordEnv != null || signingPasswordFileEnv != null
    
    if (hasSigningKey && hasSigningPassword) {
        apply(plugin = "signing")
    }
}

// Configure signing and publishing for library modules (must be in afterEvaluate)
configure(subprojects.filter { it.name in listOf("isl-transform", "isl-validation", "isl-cmd") }) {
    afterEvaluate {
        val signingKeyEnv = System.getenv("SIGNING_KEY")
        val signingKeyFileEnv = System.getenv("SIGNING_KEY_FILE")
        val signingPasswordEnv = System.getenv("SIGNING_PASSWORD")
        val signingPasswordFileEnv = System.getenv("SIGNING_PASSWORD_FILE")
        
        val hasSigningKey = signingKeyEnv != null || signingKeyFileEnv != null
        val hasSigningPassword = signingPasswordEnv != null || signingPasswordFileEnv != null
        
        // Configure signing extension FIRST (before signAllPublications is called)
        if (hasSigningKey && hasSigningPassword && plugins.hasPlugin("signing")) {
            extensions.configure<org.gradle.plugins.signing.SigningExtension> {
                // Read signing key from environment variable or file
                val signingKeySource = if (signingKeyEnv != null) "SIGNING_KEY env" else "SIGNING_KEY_FILE: $signingKeyFileEnv"
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
                
                // Log first chars of raw key content (for verification only)
                val keyPreviewLen = 60
                val keyContentPreview = signingKeyContent.take(keyPreviewLen).replace("\n", "\\n").replace("\r", "\\r")
                println("[signing] Key source: $signingKeySource")
                println("[signing] Key content length: ${signingKeyContent.length}")
                println("[signing] Key content first ${keyPreviewLen} chars: $keyContentPreview${if (signingKeyContent.length > keyPreviewLen) "..." else ""}")
                
                // Read signing password from environment variable or file
                val signingPasswordSource = if (signingPasswordEnv != null) "SIGNING_PASSWORD env" else "SIGNING_PASSWORD_FILE: $signingPasswordFileEnv"
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
                
                // Log password length and first 2 chars only (for verification, minimal exposure)
                println("[signing] Password source: $signingPasswordSource")
                println("[signing] Password length: ${signingPassword.length}")
                if (signingPassword.isNotEmpty()) {
                    val pwdPreview = signingPassword.take(2).map { if (it.isLetterOrDigit()) it else '*' }.joinToString("")
                    println("[signing] Password first 2 chars: $pwdPreview***")
                } else {
                    println("[signing] Password first 2 chars: (empty)")
                }
                
                // Decode base64 to ASCII-armored format if needed
                val signingKey = when {
                    // If already ASCII-armored (starts with PGP header), use directly
                    signingKeyContent.trim().startsWith("-----BEGIN PGP") -> {
                        signingKeyContent.trim()
                    }
                    // Otherwise, try to decode as base64
                    else -> {
                        try {
                            // Remove all whitespace (spaces, newlines, tabs) from base64 string before decoding
                            val cleanBase64 = signingKeyContent.replace(Regex("\\s+"), "")
                            
                            // Decode base64
                            val decodedBytes = java.util.Base64.getDecoder().decode(cleanBase64)
                            val decoded = String(decodedBytes, Charsets.UTF_8)
                            
                            // Verify decoded content is a valid GPG secret key
                            if (decoded.contains("-----BEGIN PGP PRIVATE KEY BLOCK-----") || 
                                decoded.contains("-----BEGIN PGP SECRET KEY BLOCK-----")) {
                                decoded.trim()
                            } else {
                                throw GradleException(
                                    "Decoded key does not appear to be a GPG secret key. " +
                                    "Expected '-----BEGIN PGP PRIVATE KEY BLOCK-----' or '-----BEGIN PGP SECRET KEY BLOCK-----'. " +
                                    "Got: ${decoded.take(100)}..."
                                )
                            }
                        } catch (e: java.lang.IllegalArgumentException) {
                            // Base64 decoding failed - might be malformed base64 or already ASCII-armored
                            if (signingKeyContent.contains("BEGIN") || signingKeyContent.contains("END")) {
                                signingKeyContent.trim()
                            } else {
                                throw GradleException(
                                    "Signing key appears to be base64-encoded but decoding failed: ${e.message}. " +
                                    "Please ensure the key is either ASCII-armored (starts with -----BEGIN PGP) " +
                                    "or valid base64-encoded ASCII-armored key."
                                )
                            }
                        } catch (e: Exception) {
                            throw GradleException("Failed to process signing key: ${e.message}", e)
                        }
                    }
                }
                
                // Verify the final key is a secret key (required for signing)
                if (!signingKey.contains("PRIVATE KEY") && !signingKey.contains("SECRET KEY")) {
                    throw GradleException(
                        "Signing key must be a GPG PRIVATE/SECRET key, not a public key. " +
                        "The key should start with '-----BEGIN PGP PRIVATE KEY BLOCK-----' or '-----BEGIN PGP SECRET KEY BLOCK-----'"
                    )
                }
                
                // Validate key structure
                if (!signingKey.startsWith("-----BEGIN PGP")) {
                    throw GradleException("Signing key must start with '-----BEGIN PGP'")
                }
                if (!signingKey.contains("-----END PGP")) {
                    throw GradleException("Signing key must contain '-----END PGP' footer")
                }
                
                // Normalize line endings to Unix format (LF only)
                // BouncyCastle is sensitive to line ending format
                val normalizedKey = signingKey
                    .replace("\r\n", "\n")  // Windows CRLF -> LF
                    .replace("\r", "\n")     // Old Mac CR -> LF
                    .trim()                  // Remove leading/trailing whitespace
                
                // Ensure key ends with newline (some parsers expect this)
                val finalKey = if (normalizedKey.endsWith("\n")) normalizedKey else "$normalizedKey\n"
                
                // Log processed key (first chars) for verification
                val finalKeyPreview = finalKey.take(keyPreviewLen).replace("\n", "\\n").replace("\r", "\\r")
                println("[signing] Final key length: ${finalKey.length}")
                println("[signing] Final key first ${keyPreviewLen} chars: $finalKeyPreview${if (finalKey.length > keyPreviewLen) "..." else ""}")
                println("[signing] Final key ends with newline: ${finalKey.endsWith("\n")}")
                println("[signing] Key type: ${if (finalKey.contains("PRIVATE KEY")) "PRIVATE" else if (finalKey.contains("SECRET KEY")) "SECRET" else "unknown"}")
                
                // Configure the signatory - this must be done before signAllPublications() is called
                useInMemoryPgpKeys(finalKey, signingPassword)
            }
        }
        
        // Configure publishing extension AFTER signing is configured
        extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            // This tells the plugin to use the NEW Central Portal API
            val mavenCentralUsername = System.getenv("MAVEN_CENTRAL_USERNAME") ?: project.findProperty("mavenCentralUsername") as String?
            val mavenCentralPassword = System.getenv("MAVEN_CENTRAL_PASSWORD") ?: project.findProperty("mavenCentralPassword") as String?
            
            if (mavenCentralUsername != null && mavenCentralPassword != null) {
                publishToMavenCentral()
            }

            // Automatically signs artifacts (only if signing is configured)
            if (hasSigningKey && hasSigningPassword) {
                signAllPublications()
            }

            // Define coordinates
            coordinates(group.toString(), project.name, version.toString())

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
}

