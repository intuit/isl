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
        // NEW PUBLISHING CONFIGURATION
        mavenPublishing {
            // This tells the plugin to use the NEW Central Portal API
            publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)

            // Automatically signs artifacts (assumes valid GPG secrets are present)
            signAllPublications()

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

