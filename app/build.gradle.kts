import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "cc.rbbl"
version = "1.6.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://gitlab.com/api/v4/projects/28863171/packages/maven")
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
}

application {
    mainClass.set("cc.rbbl.MainKt")
}

sourceSets.main {
    java.srcDirs("src/main/java")
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "17"

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

val exposedVersion = "0.36.1"
val ktorVersion = "2.3.2"
val coroutinesVersion = "1.7.1"

dependencies {
    implementation("cc.rbbl:program-parameters-jvm:1.0.3")
    implementation("net.dv8tion:JDA:4.4.1_353")
    implementation("com.adamratzman:spotify-api-kotlin-core:4.0.0")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("cc.rbbl:ktor-health-check:2.0.0")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-java:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    //Persistence
    implementation("org.postgresql:postgresql:42.3.8")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.flywaydb:flyway-core:8.2.3")

    //LOGGING
    implementation("ch.qos.logback:logback-classic:1.2.9")

    //Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.getByName<ShadowJar>("shadowJar") {
    archiveFileName.set("genre-police.jar")
}


