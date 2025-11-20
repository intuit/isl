plugins {
    kotlin("jvm")
    id("antlr")
    id("jacoco")
    id("maven-publish")
    id("org.gradle.test-retry") version "1.6.0"
    id("me.champeau.jmh") version "0.7.2"
}

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
}

// Configure ANTLR
tasks.generateGrammarSource {
    maxHeapSize = "64m"
    val grammarDir = project.file("src/main/ant4")
    arguments = arguments + listOf(
        "-visitor",
        "-package", "com.intuit.isl.ant4",
        "-lib", grammarDir.absolutePath
    )
    outputDirectory = file("src/main/java/com/intuit/isl/ant4")
    
    // Explicitly set the source directory
    // sourceSets.main.antlr4 {
    //     srcDirs("src/main/ant4")
    // }

    println(">>>>> LOGS")
    println(">>> Directories {}" + grammarDir.absolutePath)
    println("Source Sets {}" + grammarDir)
    println(">>> Output {}" + outputDirectory)
    
    setSource(grammarDir.absolutePath)
    include("**/*.g4")
    exclude("**/*.tokens", "**/*.interp")
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

// Make sure sourcesJar depends on generateGrammarSource
tasks.named<Jar>("sourcesJar") {
    dependsOn(tasks.generateGrammarSource)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Configure JaCoCo
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    
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

// Add test JAR to published artifacts
configure<PublishingExtension> {
    publications {
        named<MavenPublication>("maven") {
            artifact(testJar)
        }
    }
}

// Configure JMH
jmh {
    iterations.set(3)  // Number of measurement iterations
    warmupIterations.set(2)  // Number of warmup iterations
    fork.set(1)  // Number of forks
    benchmarkMode.set(listOf("avgt"))  // Average time
    timeUnit.set("ms")  // Milliseconds
    resultFormat.set("JSON")  // Output format
    resultsFile.set(project.layout.buildDirectory.file("reports/jmh/results.json").get().asFile)
}

