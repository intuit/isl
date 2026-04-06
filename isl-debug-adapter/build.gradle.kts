plugins {
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val kotlinVersion: String = "2.1.10"
val kotlinCoroutinesVersion: String = "1.10.1"
val jacksonVersion: String = "2.18.3"

application {
    mainClass.set("com.intuit.isl.dap.MainKt")
}

dependencies {
    implementation(project(":isl-transform"))
    implementation(project(":isl-cmd"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.16")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.1")
}

tasks.shadowJar {
    archiveBaseName.set("isl-debug-adapter")
    archiveClassifier.set("all")
    archiveVersion.set(project.version.toString())

    manifest {
        attributes(
            "Main-Class" to "com.intuit.isl.dap.MainKt",
            "Implementation-Title" to "ISL Debug Adapter",
            "Implementation-Version" to project.version
        )
    }

    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.intuit.isl.dap.MainKt",
            "Implementation-Title" to "ISL Debug Adapter",
            "Implementation-Version" to project.version
        )
    }
}
