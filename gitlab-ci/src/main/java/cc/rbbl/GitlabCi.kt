package cc.rbbl

import cc.rbbl.gitlab_ci_kotlin_dsl_extensions.docker.*
import pcimcioch.gitlabci.dsl.gitlabCi
import pcimcioch.gitlabci.dsl.job.createRules

object Stages {
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

const val gitlabCiImage = "\$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA"

val gitlabCiSource = DockerSource(gitlabCiImage, gitlabDockerCredentials)

object Credentials {
    val DockerHub = DockerCredentials("\$DOCKERHUB_USER", "\$DOCKERHUB_ACCESS_TOKEN")
    val Jfrog = DockerCredentials("\$JFROG_USERNAME", "\$JFROG_API_KEY", "\$JFROG_URL")
}

object Targets {
    val DockerHubDev = DockerTarget("rbbl/genre-police:dev", Credentials.DockerHub)
    val DockerHubLatest = DockerTarget("rbbl/genre-police:latest", Credentials.DockerHub)
    val DockerHubTagged = DockerTarget("rbbl/genre-police:\$CI_COMMIT_TAG", Credentials.DockerHub)
    val JfrogTagged =
        DockerTarget("\$JFROG_URL/genre-police-docker-local/genre-police:\$CI_COMMIT_TAG", Credentials.Jfrog)
}

fun main() {
    gitlabCi(validate = true, "../.gitlab-ci.yml") {
        stages {
            +Stages.Build
            +Stages.Publish
            +Stages.Release
        }

        val buildJob = job("build") {
            stage = Stages.Build
            image("gradle:7.2.0-jdk11")
            script("gradle build")
            artifacts {
                paths("app/build/libs/genre-police.jar")
            }
        }

        val dockerBuildJob = dockerBuildJob(
            "docker-build",
            DockerTarget(gitlabCiImage, gitlabDockerCredentials),
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
            listOf(Targets.DockerHubTagged, Targets.JfrogTagged)
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
                "dnf in -y openssl",
                "curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3",
                "chmod 700 get_helm.sh",
                "./get_helm.sh",
                "helm repo add bitnami https://charts.bitnami.com/bitnami",
                "helm dependency build helm/genre-police",
                "helm package helm/genre-police",
                "mv genre-police-*.tgz genre-police.tgz"
            )
        }

        job("helm-publish-release") {
            extends(helmBaseJob)
            script("curl --request POST --user gitlab-ci-token:\$CI_JOB_TOKEN --form 'chart=@genre-police.tgz' \${CI_API_V4_URL}/projects/\${CI_PROJECT_ID}/packages/helm/api/stable/charts")
            rules = Rules.release
        }

        job("helm-publish-release-candidate") {
            extends(helmBaseJob)
            script("curl --request POST --user gitlab-ci-token:\$CI_JOB_TOKEN --form 'chart=@genre-police.tgz' \${CI_API_V4_URL}/projects/\${CI_PROJECT_ID}/packages/helm/api/dev/charts")
            rules = Rules.releaseCandidate
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