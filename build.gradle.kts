plugins {
    kotlin("jvm") version "2.1.10" apply false
    id("org.jetbrains.kotlin.plugin.allopen") version "2.1.10" apply false
}

group = "com.intuit.isl"
version = "2.4.20-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")
    apply(plugin = "maven-publish")

    group = "com.intuit.isl"
    version = rootProject.version

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        withSourcesJar()
    }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
            freeCompilerArgs = listOf("-Xjsr305=strict")
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

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                
                pom {
                    name.set(project.name)
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

        repositories {
            mavenLocal()
            
            // Maven Central via Sonatype OSSRH
            maven {
                name = "OSSRH"
                url = uri(
                    if (version.toString().endsWith("SNAPSHOT"))
                        "https://oss.sonatype.org/content/repositories/snapshots/"
                    else
                        "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                )
                credentials {
                    username = System.getenv("OSSRH_USERNAME") ?: findProperty("ossrhUsername") as String?
                    password = System.getenv("OSSRH_PASSWORD") ?: findProperty("ossrhPassword") as String?
                }
            }
            
            // GitHub Packages
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/intuit/isl")
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: findProperty("githubActor") as String?
                    password = System.getenv("GITHUB_TOKEN") ?: findProperty("githubToken") as String?
                }
            }
        }
    }
    
    // Signing configuration for Maven Central
    if (System.getenv("SIGNING_KEY") != null || project.hasProperty("signing.keyId")) {
        apply(plugin = "signing")
        extensions.configure<org.gradle.plugins.signing.SigningExtension> {
            if (System.getenv("SIGNING_KEY") != null) {
                val signingKey = System.getenv("SIGNING_KEY")
                val signingPassword = System.getenv("SIGNING_PASSWORD") ?: ""
                // Use 2-parameter version which only needs the key and password (no keyId)
                useInMemoryPgpKeys(signingKey, signingPassword)
            }
            sign(extensions.getByType<PublishingExtension>().publications["maven"])
        }
    }
}

