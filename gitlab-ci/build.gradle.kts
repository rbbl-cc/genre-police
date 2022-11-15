plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "cc.rbbl"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.pcimcioch:gitlab-ci-kotlin-dsl:1.3.1")
}


application {
    mainClass.set("cc.rbbl.MainKt")
}

tasks.create("createCi") {
    dependsOn("run")
}