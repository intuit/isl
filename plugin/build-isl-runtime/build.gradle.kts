/**
 * Standalone Gradle build to download ISL artifacts from Maven Central
 * and package them as a fat JAR for the VS Code plugin.
 *
 * Run: ./gradlew buildIslRuntime -PislVersion=1.1.0
 * Output: plugin/lib/isl-cmd-all.jar
 */
plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

val islVersion: String = findProperty("islVersion")?.toString()
    ?: project.findProperty("version")?.toString()
    ?: "1.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.intuit.isl:isl-cmd:$islVersion")
}

tasks.shadowJar {
    archiveBaseName.set("isl-cmd")
    archiveClassifier.set("all")
    archiveVersion.set("")
    archiveFileName.set("isl-cmd-all.jar")

    manifest {
        attributes(
            "Main-Class" to "com.intuit.isl.cmd.IslCommandLineKt",
            "Implementation-Title" to "ISL Command Line",
            "Implementation-Version" to islVersion,
            "Multi-Release" to "true"
        )
    }

    mergeServiceFiles()
}

val buildIslRuntime by tasks.registering(Copy::class) {
    group = "build"
    description = "Download ISL from Maven Central and package as fat JAR for the plugin"
    dependsOn(tasks.shadowJar)

    from(tasks.shadowJar.get().archiveFile)
    into(project.rootProject.layout.projectDirectory.dir("plugin/lib"))
    rename { "isl-cmd-all.jar" }

    doLast {
        val dest = project.rootProject.file("plugin/lib/isl-cmd-all.jar")
        logger.lifecycle("✓ Built isl-cmd-all.jar (ISL $islVersion) -> ${dest.absolutePath}")
    }
}
