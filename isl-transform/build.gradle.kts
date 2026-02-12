import java.util.zip.ZipFile
import java.util.zip.ZipEntry

plugins {
    kotlin("jvm")
    id("antlr")
    id("jacoco")
    id("org.gradle.test-retry") version "1.6.0"
    id("me.champeau.jmh") version "0.7.2"
}

// Publishing is configured globally for library modules

val kotlinVersion: String = "2.1.10"
val kotlinCoroutinesVersion: String = "1.10.1"
val jacksonVersion: String = "2.18.3"
val jsonpathVersion: String = "2.9.0"
val antlrVersion: String = "4.9.1"
val jacocoVersion: String = "0.8.12"
val jmhVersion: String = "1.37"

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinCoroutinesVersion")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.jayway.jsonpath:json-path:$jsonpathVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")

    // ANTLR
    antlr("org.antlr:antlr4:$antlrVersion")
    implementation("org.antlr:antlr4-runtime:$antlrVersion")

    // Other dependencies
    implementation("org.apache.httpcomponents:httpcore:4.4.1")
    implementation("javax.mail:mail:1.4.7")
    implementation("com.google.guava:guava:33.4.0-jre")
    implementation("com.opencsv:opencsv:5.10")
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    
    // BouncyCastle - marked as optional for compile to prevent transitive leaks, but needed for tests
    compileOnly("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    compileOnly("org.bouncycastle:bcprov-jdk18on:1.78.1")
    
    // But add as testImplementation for tests
    testImplementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    testImplementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    
    implementation("org.slf4j:slf4j-api:2.0.17")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.1")
    testImplementation("io.mockk:mockk-jvm:1.13.17")
    
    // JMH Benchmarking
    jmh("org.openjdk.jmh:jmh-core:$jmhVersion")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:$jmhVersion")
    
    // JOLT for comparison benchmarks
    jmh("com.bazaarvoice.jolt:jolt-core:0.1.8")
    jmh("com.bazaarvoice.jolt:json-utils:0.1.8")
    
    // MVEL for comparison benchmarks
    jmh("org.mvel:mvel2:2.5.2.Final")
    
    // GraalVM for Python comparison benchmarks
    jmh("org.graalvm.polyglot:polyglot:24.1.1")
    jmh("org.graalvm.python:python-language:24.1.1")
    jmh("org.graalvm.python:python-resources:24.1.1")
    jmh("org.graalvm.truffle:truffle-api:24.1.1")
    jmh("org.bouncycastle:bcprov-jdk18on:1.78.1")  // Required by Python
}

// Configure ANTLR
tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf(
        "-visitor",
        "-package", "com.intuit.isl.antlr",
    )
    outputDirectory = file("src/main/java/com/intuit/isl/antlr")
}


// Configure source directories
sourceSets {
    main {
        kotlin {
            srcDirs("src/main/kotlin", "src/main/java")
        }
        java {
            srcDirs("src/main/java")
            // Exclude the old ant4 generated directory, we're using grammar now
            // exclude("com/intuit/isl/ant4/**")
        }
    }
    test {
        kotlin {
            srcDirs("src/test/kotlin", "src/test/java")
        }
        java {
            srcDirs("src/test/java")
        }
    }
}

// Configure the Kotlin compilation to happen after ANTLR generation
tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

tasks.compileJava {
    dependsOn(tasks.generateGrammarSource)
}

tasks.compileTestKotlin {
    dependsOn(tasks.generateTestGrammarSource)
}

tasks.named("compileJmhKotlin") {
    dependsOn(tasks.generateJmhGrammarSource)
}

// Make sure sourcesJar depends on generateGrammarSource
tasks.named<Jar>("sourcesJar") {
    dependsOn(tasks.generateGrammarSource)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Configure JaCoCo
tasks.jacocoTestReport {
    dependsOn(tasks.test, tasks.processResources, tasks.classes)
    
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "com/intuit/isl/grammar/**",
                    "com/intuit/isl/debug/**",
                    "com/intuit/isl/parser/ParserErrorListener*",
                    "com/intuit/isl/commands/builder/IslJsBuilder*",
                    "com/intuit/isl/runtime/LocalTransformer*",
                    "com/intuit/isl/runtime/TransformCompilationException*",
                    "com/intuit/isl/common/LocalOperationContext*",
                    "com/intuit/isl/utils/ObjectRefNode*",
                    "com/intuit/isl/utils/IslSecurityKeyStore*",
                    "com/intuit/isl/utils/InstantNode*",
                    "com/intuit/isl/commands/Noop*",
                    "**/transform/**",
                    "*visit*",
                    "*equals*",
                    "*lint*"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport, tasks.classes)

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.0".toBigDecimal()
            }
        }
    }
    classDirectories.setFrom(tasks.jacocoTestReport.get().classDirectories)
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
    
    // Retry failed tests up to 3 times (similar to Maven surefire rerunFailingTestsCount)
    retry {
        maxRetries.set(3)
        maxFailures.set(10)
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// Configure JAR to include manifest entries (for version detection)
tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Specification-Title" to project.name,
            "Specification-Version" to project.version
        )
    }
}

// Create test JAR (similar to Maven test-jar goal)
val testJar by tasks.registering(Jar::class) {
    archiveClassifier.set("tests")
    from(sourceSets.test.get().output)
}

artifacts {
    archives(testJar)
}

// Test JAR is available locally but not published to Maven Central
// configure<PublishingExtension> {
//     publications {
//         named<MavenPublication>("maven") {
//             artifact(testJar) {
//                 classifier = "tests"
//             }
//         }
//     }
// }

// Configure JMH
jmh {
    iterations.set(3)  // Number of measurement iterations
    warmupIterations.set(2)  // Number of warmup iterations
    fork.set(1)  // Number of forks
    benchmarkMode.set(listOf("avgt"))  // Average time
    timeUnit.set("ms")  // Milliseconds
    resultFormat.set("JSON")  // Output format
    resultsFile.set(project.layout.buildDirectory.file("reports/jmh/results.json").get().asFile)
    profilers.set(listOf("gc"))  // Enable GC profiler for memory allocation tracking
}

// Enable zip64 for large JMH jar (GraalVM dependencies are large)
// And properly merge service files for GraalVM language discovery
tasks.named<Jar>("jmhJar") {
    isZip64 = true
    
    // Use INCLUDE strategy to allow service file processing
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    
    manifest {
        attributes(
            "Multi-Release" to "true"
        )
    }
}

// Post-process the JMH JAR to merge service files
// This is critical for GraalVM to discover the Python language
tasks.register("mergeJmhServices") {
    dependsOn("jmhJar")
    
    doLast {
        val jmhJarFile = tasks.getByName<Jar>("jmhJar").archiveFile.get().asFile
        val tempDir = File(layout.buildDirectory.get().asFile, "jmh-service-merge")
        val servicesDir = File(tempDir, "META-INF/services")
        servicesDir.mkdirs()
        
        // Collect all service files from the JAR
        val serviceProviders = mutableMapOf<String, MutableSet<String>>()
        
        ZipFile(jmhJarFile).use { jar: ZipFile ->
            jar.entries().asSequence()
                .filter { entry: ZipEntry -> 
                    entry.name.startsWith("META-INF/services/") && !entry.isDirectory 
                }
                .forEach { entry: ZipEntry ->
                    val serviceName = entry.name.substringAfter("META-INF/services/")
                    val providers = jar.getInputStream(entry).bufferedReader().readLines()
                        .map { line: String -> line.trim() }
                        .filter { line: String -> line.isNotEmpty() && !line.startsWith("#") }
                    serviceProviders.getOrPut(serviceName) { mutableSetOf() }.addAll(providers)
                }
        }
        
        // Extract the entire JAR
        val extractDir = File(tempDir, "jar-contents")
        extractDir.mkdirs()
        
        println("Extracting JMH JAR...")
        ZipFile(jmhJarFile).use { jar: ZipFile ->
            jar.entries().asSequence().forEach { entry: ZipEntry ->
                val entryFile = File(extractDir, entry.name)
                if (entry.isDirectory) {
                    entryFile.mkdirs()
                } else {
                    entryFile.parentFile.mkdirs()
                    // Skip service files - we'll write the merged ones
                    if (!entry.name.startsWith("META-INF/services/")) {
                        jar.getInputStream(entry).use { input ->
                            entryFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        }
        
        // Write merged service files
        val extractedServicesDir = File(extractDir, "META-INF/services")
        extractedServicesDir.mkdirs()
        serviceProviders.forEach { (serviceName, providers) ->
            val serviceFile = File(extractedServicesDir, serviceName)
            serviceFile.writeText(providers.sorted().joinToString("\n") + "\n")
        }
        
        // Backup and recreate the JAR
        val backupFile = File(jmhJarFile.parentFile, "${jmhJarFile.name}.backup")
        jmhJarFile.renameTo(backupFile)
        
        println("Repacking JMH JAR with merged services...")
        exec {
            workingDir = extractDir
            commandLine = if (System.getProperty("os.name").lowercase().contains("windows")) {
                listOf("cmd", "/c", "jar", "cfm", jmhJarFile.absolutePath, "META-INF/MANIFEST.MF", ".")
            } else {
                listOf("jar", "cfm", jmhJarFile.absolutePath, "META-INF/MANIFEST.MF", ".")
            }
        }
        
        // Clean up
        backupFile.delete()
        tempDir.deleteRecursively()
        
        println("✓ Successfully merged ${serviceProviders.size} service files in JMH JAR")
        serviceProviders.forEach { (service, providers) ->
            println("  - $service: ${providers.size} provider(s)")
        }
    }
}

// Make jmh task depend on service merging
tasks.named("jmh") {
    dependsOn("mergeJmhServices")
}

