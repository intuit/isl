plugins {
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val kotlinVersion: String = "2.1.10"
val kotlinCoroutinesVersion: String = "1.10.1"
val picocliVersion: String = "4.7.6"
val jacksonVersion: String = "2.18.3"

application {
    mainClass.set("com.intuit.isl.cmd.IslCommandLineKt")
}

dependencies {
    // ISL modules
    implementation(project(":isl-transform"))
    implementation(project(":isl-validation"))
    implementation(project(":isl-test"))
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinCoroutinesVersion")
    
    // Jackson for JSON/YAML processing
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    
    // Command line parsing
    implementation("info.picocli:picocli:$picocliVersion")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.16")
    
    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.1")
    testImplementation("io.mockk:mockk-jvm:1.13.17")
}

// Configure Shadow JAR for fat JAR creation
tasks.shadowJar {
    archiveBaseName.set("isl")
    archiveClassifier.set("all")
    archiveVersion.set(project.version.toString())
    
    manifest {
        attributes(
            "Main-Class" to "com.intuit.isl.cmd.IslCommandLineKt",
            "Implementation-Title" to "ISL Command Line",
            "Implementation-Version" to project.version,
            "Multi-Release" to "true"
        )
    }
    
    // Merge service files for proper functionality
    mergeServiceFiles()
}

// Make build depend on shadowJar
tasks.build {
    dependsOn(tasks.shadowJar)
}

// Create a task to run the CLI
tasks.register<JavaExec>("runIsl") {
    group = "application"
    description = "Run ISL CLI with arguments"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.intuit.isl.cmd.IslCommandLineKt")
    workingDir = project.findProperty("runWorkingDir")?.toString()?.takeIf { it.isNotBlank() }?.let { file(it) } ?: rootProject.projectDir
    
    // Allow passing arguments: ./gradlew :isl-cmd:runIsl --args="script.isl"
    if (project.hasProperty("args")) {
        args = (project.property("args") as String).split("\\s+".toRegex())
    }
}

// Application run task: use invocation directory when set (e.g. from isl.bat/isl.sh)
tasks.named<JavaExec>("run").configure {
    workingDir = project.findProperty("runWorkingDir")?.toString()?.takeIf { it.isNotBlank() }?.let { file(it) } ?: rootProject.projectDir
}

// Configure JAR manifest
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.intuit.isl.cmd.IslCommandLineKt",
            "Implementation-Title" to "ISL Command Line",
            "Implementation-Version" to project.version
        )
    }
}

// Publishing is configured automatically by the maven publish plugin

