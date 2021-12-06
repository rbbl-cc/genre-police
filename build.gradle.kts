plugins {
    java
    kotlin("jvm") version "1.5.31"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "cc.rbbl"
version = "1.4.1"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://gitlab.com/api/v4/projects/28863171/packages/maven")
    maven("https://m2.dv8tion.net/releases")
}

application {
    mainClass.set("cc.rbbl.MainKt")
}

sourceSets.main {
    java.srcDirs("src/main/java")
}

val exposedVersion = "0.34.1"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    implementation("cc.rbbl:program-parameters-jvm:1.0.3")
    implementation("net.dv8tion:JDA:4.2.1_265")
    implementation("com.adamratzman:spotify-api-kotlin-core:3.8.4")

    //Persistence
    implementation("org.postgresql:postgresql:42.3.1")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.flywaydb:flyway-core:8.1.0")

    //LOGGING
    implementation("ch.qos.logback:logback-classic:1.2.7")

    //Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}


