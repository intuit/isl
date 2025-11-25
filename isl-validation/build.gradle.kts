plugins {
    kotlin("jvm")
    id("jacoco")
    id("maven-publish")
    id("org.gradle.test-retry") version "1.6.0"
}

val kotlinVersion: String = "2.1.10"
val kotlinCoroutinesVersion: String = "1.10.1"
val jacksonVersion: String = "2.18.3"

dependencies {
    // Project dependency
    implementation(project(":isl-transform"))

    // Kotlin coroutines (needed for runBlocking)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinCoroutinesVersion")
    
    // Jackson modules (needed for tests)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")

    // JSON Schema Validator
    implementation("com.networknt:json-schema-validator:1.5.5") {
        exclude(group = "org.apache.commons", module = "commons-lang3")
    }

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.1")
    testImplementation("io.mockk:mockk-jvm:1.13.17")
}

// Configure source directories
sourceSets {
    main {
        kotlin {
            srcDirs("src/main/kotlin")
        }
    }
    test {
        kotlin {
            srcDirs("src/test/kotlin")
        }
    }
}

// Configure JaCoCo
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude("com/intuit/isl/**")
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
    
    // Retry failed tests up to 3 times
    retry {
        maxRetries.set(3)
        maxFailures.set(10)
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// Configure JAR to include manifest entries
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

// Create test JAR
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

