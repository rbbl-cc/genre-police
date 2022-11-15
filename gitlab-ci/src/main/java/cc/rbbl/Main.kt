package cc.rbbl

import pcimcioch.gitlabci.dsl.Duration
import pcimcioch.gitlabci.dsl.gitlabCi
import pcimcioch.gitlabci.dsl.job.WhenUploadType
import java.io.FileWriter

fun main() {
    val test = FileWriter("../.gitlab-ci.yml")
    gitlabCi(validate = true, writer = test) {
        default {
            image("openjdk:8u162")

            cache("./gradle/wrapper", "./gradle/caches")

            beforeScript("export GRADLE_USER_HOME=\$(pwd)/.gradle")
        }

        stages {
            +"test"
            +"release"
        }

        job("build") {
            stage = "test"
            script("./gradlew clean build")
            artifacts {
                whenUpload = WhenUploadType.ALWAYS
                paths("build/test-results", "build/reports")
                expireIn = Duration(days = 7)
                reports {
                    junit("build/test-results/test/TEST-*.xml")
                }
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
    test.close()
}