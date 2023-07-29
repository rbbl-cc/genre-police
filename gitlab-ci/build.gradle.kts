plugins {
    kotlin("jvm")
    application
}

group = "cc.rbbl"
version = "unspecified"

repositories {
    mavenCentral()
    maven {
        name = "Snapshot"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("com.github.pcimcioch:gitlab-ci-kotlin-dsl:1.6.0")
    implementation("cc.rbbl.gitlab-ci-kotlin-dsl-extensions:common-jobs:1.0.1")
    implementation("cc.rbbl.gitlab-ci-kotlin-dsl-extensions:container-common:0.1.0-SNAPSHOT")
    implementation("cc.rbbl.gitlab-ci-kotlin-dsl-extensions:podman:0.1.0-SNAPSHOT")
}


application {
    mainClass.set("cc.rbbl.GitlabCiKt")
}

tasks.create("renderCi") {
    dependsOn("run")
}