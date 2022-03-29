plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    java
    `java-library`
    application

}

group = "confidentialstorage"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.walt.id/repository/waltid/")
    maven("https://maven.walt.id/repository/waltid-ssi-kit/")
    maven("https://maven.walt.id/repository/danubetech")
    maven("https://repo.danubetech.com/repository/maven-public/")
    //maven("https://us-central1-maven.pkg.dev/varabyte-repos/public")
    maven("https://jitpack.io")
}

java.sourceSets["main"].java {
    srcDir("src/main/kotlin")
}

dependencies {
    // Crypto
    // implementation("com.cossacklabs.com:themis:0.13.1")
    // implementation("com.cossacklabs.com:java-themis:0.13.1")
    implementation("com.nimbusds:nimbus-jose-jwt:9.21")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    // -- WALT.ID SSI KIT --
    implementation("id.walt:waltid-ssi-kit:1.7-SNAPSHOT")
    implementation("id.walt.servicematrix:WaltID-ServiceMatrix:1.1.0")
    implementation("id.walt:waltid-ssikit-vclib:1.15.0")

    // Web
    implementation("io.javalin:javalin-bundle:4.3.0")

    // JSON
    implementation("com.beust:klaxon:5.5")

    // Logging
    //implementation("org.slf4j:slf4j-simple:1.8.0-beta4")
    implementation("org.slf4j:slf4j-simple:2.0.0-alpha6")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")


    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:5.1.0")
    testImplementation("io.kotest:kotest-assertions-core:5.1.0")
    testImplementation("io.kotest:kotest-assertions-json:5.1.0")

    // CLI
    //implementation("com.github.ajalt.clikt:clikt:3.3.0")
    //implementation("com.varabyte.konsole:konsole:0.9.0")
    implementation("org.jline:jline-builtins:3.21.0")
    implementation("org.jline:jline-reader:3.21.0")
    implementation("org.jline:jline-terminal:3.21.0")

    // Http
    implementation("io.ktor:ktor-client-core:1.6.7")
    implementation("io.ktor:ktor-client-cio:1.6.7")
    implementation("io.ktor:ktor-client-auth:1.6.7")
    implementation("io.ktor:ktor-client-logging:1.6.7")


    // Serialization
    implementation("io.ktor:ktor-client-serialization:1.6.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
val compileJava: JavaCompile by tasks
compileJava.destinationDirectory.set(compileKotlin.destinationDirectory)

tasks {
    "compileJava" {
        dependsOn(":compileKotlin")
        if (JavaVersion.current() >= JavaVersion.VERSION_1_9) {
            doFirst {
                compileJava.get().options.compilerArgs = listOf(
                    // include Gradle dependencies as modules
                    "--module-path", java.sourceSets["main"].compileClasspath.asPath,
                )
                java.sourceSets["main"].compileClasspath = files()
            }
        }
    }
}

application {
    mainClass.set(System.getProperty("exec.mainClass") ?: "confidentialstorage.server.WebserverKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
