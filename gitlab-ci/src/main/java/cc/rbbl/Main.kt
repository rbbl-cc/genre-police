package cc.rbbl

import pcimcioch.gitlabci.dsl.Duration
import pcimcioch.gitlabci.dsl.gitlabCi
import pcimcioch.gitlabci.dsl.job.WhenUploadType
import java.io.FileWriter

object Stages {
    val Build = "build"
    val Docker = "docker"
    val Helm = "helm"
    val Release = "release"
}

fun main() {
    val writer = FileWriter("../.gitlab-ci.yml")
    gitlabCi(validate = true, writer = writer) {
        stages {
            +Stages.Build
            +Stages.Docker
            +Stages.Helm
            +Stages.Release
        }

        job("build") {
            stage = Stages.Build
            image("gradle:7.2.0-jdk11")
            script("gradle build")
            artifacts {
                paths("app/build/libs/genre-police.jar")
            }
        }

        job("release") {
            stage = "release"
            script("./gradlew publishToMavenLocal")
            only {
                master()
            }
            artifacts {
                whenUpload = WhenUploadType.ON_SUCCESS
                paths("build/libs")
                expireIn = Duration(days = 7)
            }
        }
    }
    writer.close()
}