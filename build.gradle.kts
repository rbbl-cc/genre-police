plugins {
    java
    kotlin("jvm") version "1.5.31"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "cc.rbbl"
version = "1.4.0-RC4"

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

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    implementation("cc.rbbl:program-parameters-jvm:1.0.3")
    implementation("net.dv8tion:JDA:4.2.1_265")
    implementation("com.adamratzman:spotify-api-kotlin-core:3.8.3")

    //Persistence
    implementation("org.postgresql:postgresql:42.2.24")
    implementation("org.hibernate:hibernate-core:5.5.7.Final")
    implementation("org.apache.commons:commons-dbcp2:2.9.0")
    implementation("org.flywaydb:flyway-core:7.15.0")

    //LOGGING
    implementation("ch.qos.logback:logback-classic:1.2.6")

    //Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}


