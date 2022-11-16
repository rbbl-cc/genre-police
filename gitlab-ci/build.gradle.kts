plugins {
    kotlin("jvm")
    application
}

group = "cc.rbbl"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.pcimcioch:gitlab-ci-kotlin-dsl:1.3.2")
}


application {
    mainClass.set("cc.rbbl.GitlabCiKt")
}

tasks.create("renderCi") {
    dependsOn("run")
}