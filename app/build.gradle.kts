import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
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
compileKotlin.kotlinOptions.jvmTarget = "11"

tasks.withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

val exposedVersion = "0.36.1"
val ktorVersion = "1.6.8"

dependencies {
    implementation("cc.rbbl:program-parameters-jvm:1.0.3")
    implementation("net.dv8tion:JDA:4.4.0_350")
    implementation("com.adamratzman:spotify-api-kotlin-core:3.8.6")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("com.github.zensum:ktor-health-check:011a5a8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    //Persistence
    implementation("org.postgresql:postgresql:42.3.6")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.flywaydb:flyway-core:8.2.3")

    //LOGGING
    implementation("ch.qos.logback:logback-classic:1.2.9")

    //Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.getByName<ShadowJar>("shadowJar") {
    archiveFileName.set("genre-police.jar")
}


