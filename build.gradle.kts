val logback_version: String by project

plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "cc.rbbl"
version = "1.2.1"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://gitlab.com/api/v4/projects/28863171/packages/maven")
    maven("https://m2.dv8tion.net/releases")
}

application {
    mainClass.set("cc.rbbl.Main")
}

dependencies {
    implementation("cc.rbbl:program-parameters-jvm:1.0.3")
    implementation("net.dv8tion:JDA:4.2.1_265")
    implementation("se.michaelthelin.spotify:spotify-web-api-java:6.5.1")

    //Percistence
    implementation("org.postgresql:postgresql:42.2.23")
    implementation("org.hibernate:hibernate-core:5.5.7.Final")
    implementation("org.flywaydb:flyway-core:7.15.0")

    //LOGGING
    implementation("ch.qos.logback:logback-classic:1.2.3")

    //Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}


