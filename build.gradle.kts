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

// ---- Publishing (Maven Central + signing) ----
// Maven Central: plugin build service needs mavenCentralUsername/Password. Set via:
//   - Env: MAVEN_CENTRAL_USERNAME, MAVEN_CENTRAL_PASSWORD (or OSSRH_*); CI should also set ORG_GRADLE_PROJECT_mavenCentralUsername/Password.
//   - Root gradle.properties: mavenCentralUsername, mavenCentralPassword (we set project.ext from these).
// Signing: env SIGNING_KEY (or SIGNING_KEY_FILE) and SIGNING_PASSWORD (or SIGNING_PASSWORD_FILE)

val publishModules = listOf("isl-transform", "isl-validation", "isl-cmd")

configure(subprojects.filter { it.name in publishModules }) {
    afterEvaluate {
        val mavenUser = System.getenv("MAVEN_CENTRAL_USERNAME") ?: System.getenv("OSSRH_USERNAME") ?: rootProject.findProperty("mavenCentralUsername")?.toString()
        val mavenPass = System.getenv("MAVEN_CENTRAL_PASSWORD") ?: System.getenv("OSSRH_PASSWORD") ?: rootProject.findProperty("mavenCentralPassword")?.toString()
        val hasMavenCreds = !mavenUser.isNullOrBlank() && !mavenPass.isNullOrBlank()
        if (hasMavenCreds) {
            project.ext.set("mavenCentralUsername", mavenUser)
            project.ext.set("mavenCentralPassword", mavenPass)
        }

        val signKey = System.getenv("SIGNING_KEY") ?: System.getenv("SIGNING_KEY_FILE")?.let { file(it).readText(Charsets.UTF_8).trim() }
        val signPass = System.getenv("SIGNING_PASSWORD") ?: System.getenv("SIGNING_PASSWORD_FILE")?.let { file(it).readText(Charsets.UTF_8).trim() } ?: ""
        val hasSigning = !signKey.isNullOrBlank()

        if (hasMavenCreds && hasSigning) {
            apply(plugin = "signing")
            extensions.configure<org.gradle.plugins.signing.SigningExtension> {
                val keyContent = signKey!!.trim()
                val key = if (keyContent.startsWith("-----BEGIN PGP")) keyContent
                else String(java.util.Base64.getDecoder().decode(keyContent.replace(Regex("\\s+"), ""))).trim()
                useInMemoryPgpKeys(key.replace("\r\n", "\n").replace("\r", "\n").trim().let { if (it.endsWith("\n")) it else "$it\n" }, signPass)
            }
        }

        extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            coordinates(group.toString(), project.name, version.toString())
            pom {
                name.set("ISL")
                description.set("ISL - JSON transformation scripting language")
                url.set("https://github.com/intuit/isl")
                licenses { license { name.set("Apache License 2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0") } }
                developers { developer { id.set("intuit"); name.set("Intuit Inc."); email.set("opensource@intuit.com") } }
                scm {
                    url.set("https://github.com/intuit/isl")
                    connection.set("scm:git:https://github.com/intuit/isl.git")
                    developerConnection.set("scm:git:https://github.com/intuit/isl.git")
                }
            }
            if (hasMavenCreds) {
                publishToMavenCentral()
                if (hasSigning) signAllPublications()
            }
        }
    }
}

tasks.register<Copy>("buildIslRuntimeLocal") {
    group = "build"
    description = "Build isl-cmd shadow JAR from local source and copy to plugin/lib/isl-cmd-all.jar"
    dependsOn(":isl-cmd:shadowJar")

    val shadowJar = project(":isl-cmd").tasks.named<Jar>("shadowJar").get()
    from(shadowJar.archiveFile)
    into(layout.projectDirectory.dir("plugin/lib"))
    rename { "isl-cmd-all.jar" }

    doLast {
        logger.lifecycle("✓ Built isl-cmd-all.jar from local source -> plugin/lib/isl-cmd-all.jar")
    }
}

tasks.register("publishToMavenCentral") {
    group = "publishing"
    description = "Publish all modules to Maven Central"
    publishModules.forEach { name ->
        project(name).tasks.findByName("publishToMavenCentral")?.let { dependsOn(it) }
    }
    doFirst {
        if (taskDependencies.getDependencies(this).isEmpty()) {
            throw GradleException(
                "Maven Central credentials required: set MAVEN_CENTRAL_USERNAME and MAVEN_CENTRAL_PASSWORD (or OSSRH_*), or mavenCentralUsername/Password in root gradle.properties. For local only: gradlew publishToMavenLocal"
            )
        }
    }
}

