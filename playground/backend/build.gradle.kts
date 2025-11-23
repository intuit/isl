plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.intuit.isl"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_17

// Force Jackson version to match ISL requirements
ext["jackson.version"] = "2.18.3"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // ISL - using the built JAR from the parent project
    implementation(files("../../isl-transform/build/libs/isl-transform-2.4.20-SNAPSHOT.jar"))
    
    // ISL Dependencies (required when using JAR file directly)
    implementation("org.antlr:antlr4-runtime:4.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.1")
    implementation("com.jayway.jsonpath:json-path:2.9.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.18.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.18.3")
    implementation("org.apache.httpcomponents:httpcore:4.4.1")
    implementation("javax.mail:mail:1.4.7")
    implementation("com.google.guava:guava:33.4.0-jre")
    implementation("com.opencsv:opencsv:5.10")
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation("org.slf4j:slf4j-api:2.0.17")
    
    // CORS support
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

