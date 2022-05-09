plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.10"
    java
    `java-library`
    application
    `maven-publish`
}

group = "id.walt"
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
    implementation("com.nimbusds:nimbus-jose-jwt:9.22")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    // -- WALT.ID SSI KIT --
    implementation("id.walt:waltid-ssi-kit:1.9.1-SNAPSHOT")
    implementation("id.walt.servicematrix:WaltID-ServiceMatrix:1.1.1")
    implementation("id.walt:waltid-ssikit-vclib:1.18.1-SNAPSHOT")

    // Web
    implementation("io.javalin:javalin-bundle:4.5.0")

    // JSON
    implementation("com.beust:klaxon:5.6")

    // Logging
    //implementation("org.slf4j:slf4j-simple:1.8.0-beta4")
    implementation("org.slf4j:slf4j-simple:2.0.0-alpha6")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.21")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")


    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:5.3.0")
    testImplementation("io.kotest:kotest-assertions-core:5.3.0")
    testImplementation("io.kotest:kotest-assertions-json:5.3.0")

    // CLI
    //implementation("com.github.ajalt.clikt:clikt:3.3.0")
    //implementation("com.varabyte.konsole:konsole:0.9.0")
    implementation("org.jline:jline-builtins:3.21.0")
    implementation("org.jline:jline-reader:3.21.0")
    implementation("org.jline:jline-terminal:3.21.0")

    // Http
    implementation("io.ktor:ktor-client-core:2.0.1")
    implementation("io.ktor:ktor-client-content-negotiation:2.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.0.1")
    implementation("io.ktor:ktor-client-cio:2.0.1")
    implementation("io.ktor:ktor-client-logging:2.0.1")
    implementation("io.ktor:ktor-client-auth:2.0.1")


    // Serialization
    implementation("io.ktor:ktor-client-serialization:2.0.1")
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
    mainClass.set(System.getProperty("exec.mainClass") ?: "id.walt.storagekit.main.MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("walt.id Storage Kit")
                description.set("Kotlin/Java library for confidential storage functionality.")
                url.set("https://walt.id")
            }
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://maven.walt.id/repository/waltid-storagekit/")
            val usernameFile = File("secret_maven_username.txt")
            val passwordFile = File("secret_maven_password.txt")
            val secretMavenUsername = System.getenv()["MAVEN_USERNAME"] ?: if (usernameFile.isFile) { usernameFile.readLines()[0] } else { "" }
            val secretMavenPassword = System.getenv()["MAVEN_PASSWORD"] ?: if (passwordFile.isFile) { passwordFile.readLines()[0] } else { "" }

            credentials {
                username = secretMavenUsername
                password = secretMavenPassword
            }
        }
    }
}
