plugins {
    kotlin("jvm")
    application
}

group = "cc.rbbl"
version = "unspecified"

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        name = "Snapshot"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("com.github.pcimcioch:gitlab-ci-kotlin-dsl:1.3.2")
    implementation("cc.rbbl:gitlab-ci-kotlin-dsl-docker-extension:0.1.8-SNAPSHOT")
}


application {
    mainClass.set("cc.rbbl.GitlabCiKt")
}

tasks.create("renderCi") {
    dependsOn("run")
}