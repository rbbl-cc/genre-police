package cc.rbbl

import cc.rbbl.gitlab_ci_kotlin_dsl_extensions.common_jobs.ciRenderCheckJob
import cc.rbbl.gitlab_ci_kotlin_dsl_extensions.docker.*
import pcimcioch.gitlabci.dsl.gitlabCi
import pcimcioch.gitlabci.dsl.job.createRules

object Stages {
    const val Test = "test"
    const val Build = "build"
    const val Publish = "publish"
    const val Release = "release"
}

object Rules {
    val dev = createRules {
        rule {
            ifCondition = "\$CI_COMMIT_BRANCH =~ /^dev$/"
        }
    }
    val master = createRules {
        rule {
            ifCondition = "\$CI_COMMIT_BRANCH =~ /^master$/"
        }
    }
    val releaseCandidate = createRules {
        rule {
            ifCondition = "\$CI_COMMIT_TAG =~ /^\\d+\\.\\d+\\.\\d+-RC\\d+$/"
        }
    }
    val release = createRules {
        rule {
            ifCondition = "\$CI_COMMIT_TAG =~ /^\\d+\\.\\d+\\.\\d+$/"
        }
    }
}

const val gradleImage = "gradle:7.2.0-jdk11"

val gitlabCiSource = DockerImage("\$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA", gitlabDockerCredentials)

val dockerHubCredentials = DockerCredentials("\$DOCKERHUB_USER", "\$DOCKERHUB_ACCESS_TOKEN")


object Targets {
    val DockerHubDev = DockerImage("rbbl/genre-police:dev", dockerHubCredentials)
    val DockerHubLatest = DockerImage("rbbl/genre-police:latest", dockerHubCredentials)
    val DockerHubTagged = DockerImage("rbbl/genre-police:\$CI_COMMIT_TAG", dockerHubCredentials)
}

fun main() {
    gitlabCi(validate = true, "../.gitlab-ci.yml") {
        stages {
            +Stages.Test
            +Stages.Build
            +Stages.Publish
            +Stages.Release
        }

        ciRenderCheckJob("checkCiRender")

        val buildJob = job("build") {
            stage = Stages.Build
            image(gradleImage)
            script("gradle build")
            artifacts {
                paths("app/build/libs/genre-police.jar")
            }
        }

        val dockerBuildJob = dockerBuildJob(
            "docker-build",
            gitlabCiSource,
            "./app"
        ) {
            needs(buildJob)
            stage = Stages.Build
        }

        dockerMoveJob(
            "docker-publish-dev",
            gitlabCiSource,
            Targets.DockerHubDev
        ) {
            needs(dockerBuildJob)
            stage = Stages.Publish
            rules = Rules.dev
        }

        dockerMoveJob(
            "docker-publish-master",
            gitlabCiSource,
            Targets.DockerHubLatest
        ) {
            needs(dockerBuildJob)
            stage = Stages.Publish
            rules = Rules.master
        }

        dockerMoveJob(
            "docker-publish-release-candidate",
            gitlabCiSource,
            Targets.DockerHubTagged
        ) {
            needs(dockerBuildJob)
            stage = Stages.Publish
            rules = Rules.releaseCandidate
        }

        dockerMoveJob("docker-publish-release", gitlabCiSource, Targets.DockerHubTagged) {
            needs(dockerBuildJob)
            stage = Stages.Publish
            rules = Rules.release
        }

        val helmBaseJob = job(".helm") {
            stage = Stages.Publish
            image("fedora")
            beforeScript(
                "export HELM_VERSION=$(echo \$CI_COMMIT_TAG | grep -o -P '\\d+\\.\\d+\\.\\d+(-RC\\d+)?')",
                "dnf in -y openssl",
                "curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3",
                "chmod 700 get_helm.sh",
                "./get_helm.sh",
                "perl -pi -e s/(?<=appVersion: \\\")[^\\\"]*/\$APP_VERSION/g helm/genre-police/Chart.yaml",
                "perl -pi -e s/(?<=version: \\\")[^\\\"]*/\$HELM_VERSION/g helm/genre-police/Chart.yaml",
                "helm repo add bitnami https://charts.bitnami.com/bitnami",
                "helm dependency build helm/genre-police",
                "helm package helm/genre-police",
                "mv genre-police-*.tgz genre-police.tgz"
            )
        }

        job("helm-publish-release") {
            extends(helmBaseJob)
            script("curl --request POST --user gitlab-ci-token:\$CI_JOB_TOKEN --form 'chart=@genre-police.tgz' \${CI_API_V4_URL}/projects/\${CI_PROJECT_ID}/packages/helm/api/stable/charts")
            rules {
                rule {
                    ifCondition = "\$CI_COMMIT_TAG =~ /^chart\\/\\d+\\.\\d+\\.\\d+$/"
                }
            }
        }

        job("helm-publish-release-candidate") {
            extends(helmBaseJob)
            script("curl --request POST --user gitlab-ci-token:\$CI_JOB_TOKEN --form 'chart=@genre-police.tgz' \${CI_API_V4_URL}/projects/\${CI_PROJECT_ID}/packages/helm/api/dev/charts")
            rules {
                rule {
                    ifCondition = "\$CI_COMMIT_TAG =~ /^chart\\/\\d+\\.\\d+\\.\\d+-RC\\d+$/"
                }
            }
        }

        job("create-release") {
            stage = Stages.Release
            image("registry.gitlab.com/gitlab-org/release-cli:latest")
            script("echo 'Running the release job.'")
            rules = Rules.release
            release {
                name = "\$CI_COMMIT_TAG"
                tagName = "\$CI_COMMIT_TAG"
                description = "Release \$CI_COMMIT_TAG"
            }
        }
    }
}