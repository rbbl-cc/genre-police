package cc.rbbl

import pcimcioch.gitlabci.dsl.gitlabCi
import pcimcioch.gitlabci.dsl.job.createJob
import pcimcioch.gitlabci.dsl.job.createRules

object Stages {
    const val Build = "build"
    const val Docker = "docker"
    const val Helm = "helm"
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

fun main() {
    gitlabCi(validate = true, "../.gitlab-ci.yml") {
        stages {
            +Stages.Build
            +Stages.Docker
            +Stages.Helm
            +Stages.Release
        }

        val buildJob = createJob("build") {
            stage = Stages.Build
            image("gradle:7.2.0-jdk11")
            script("gradle build")
            artifacts {
                paths("app/build/libs/genre-police.jar")
            }
        }
        +buildJob


        val dockerBaseJob = createJob(".docker") {
            stage = Stages.Docker
            image("docker")
            services("docker:dind")
            beforeScript("docker login -u \$CI_REGISTRY_USER -p \$CI_REGISTRY_PASSWORD \$CI_REGISTRY")
        }
        +dockerBaseJob


        val dockerBuildJob = createJob("docker-build") {
            extends(dockerBaseJob)
            needs(buildJob)
            script(
                "docker build -t \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA ./app",
                "docker push \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA"
            )
        }
        +dockerBuildJob

        job("docker-publish-dev") {
            needs(dockerBuildJob)
            extends(dockerBaseJob)
            script(
                "docker pull \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA",
                "docker tag \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA rbbl/genre-police:dev",
                "docker login -u \$DOCKERHUB_USER -p \$DOCKERHUB_ACCESS_TOKEN",
                "docker push rbbl/genre-police:dev"
            )
            rules = Rules.dev
        }

        job("docker-publish-master") {
            needs(dockerBuildJob)
            extends(dockerBaseJob)
            script(
                "docker pull \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA",
                "docker tag \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA rbbl/genre-police:latest",
                "docker login -u \$DOCKERHUB_USER -p \$DOCKERHUB_ACCESS_TOKEN",
                "docker push rbbl/genre-police:latest"
            )
            rules = Rules.master
        }

        job("docker-publish-release") {
            needs(dockerBuildJob)
            extends(dockerBaseJob)
            script(
                "docker pull \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA",
                "docker tag \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA rbbl/genre-police:\$CI_COMMIT_TAG",
                "docker login -u \$DOCKERHUB_USER -p \$DOCKERHUB_ACCESS_TOKEN",
                "docker push rbbl/genre-police:\$CI_COMMIT_TAG"
            )
            rules = Rules.release
        }

        job("docker-publish-release-candidate") {
            needs(dockerBuildJob)
            extends(dockerBaseJob)
            script(
                "docker pull \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA",
                "docker tag \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA rbbl/genre-police:\$CI_COMMIT_TAG",
                "docker tag \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA \$JFROG_URL/genre-police-docker-local/genre-police:\$CI_COMMIT_TAG",
                "docker login -u \$DOCKERHUB_USER -p \$DOCKERHUB_ACCESS_TOKEN",
                "docker login -u \$JFROG_USERNAME -p \$JFROG_API_KEY \$JFROG_URL",
                "docker push rbbl/genre-police:\$CI_COMMIT_TAG",
                "docker push \$JFROG_URL/genre-police-docker-local/genre-police:\$CI_COMMIT_TAG"
            )
            rules = Rules.releaseCandidate
        }

        val helmBaseJob = createJob(".helm") {
            stage = Stages.Helm
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
        +helmBaseJob

        job("helm-publish-release") {
            extends(helmBaseJob)
            rules = Rules.release
            script("curl --request POST --user gitlab-ci-token:\$CI_JOB_TOKEN --form 'chart=@genre-police.tgz' \${CI_API_V4_URL}/projects/\${CI_PROJECT_ID}/packages/helm/api/stable/charts")
        }

        job("helm-publish-release-candidate") {
            extends(helmBaseJob)
            rules = Rules.releaseCandidate
            script("curl --request POST --user gitlab-ci-token:\$CI_JOB_TOKEN --form 'chart=@genre-police.tgz' \${CI_API_V4_URL}/projects/\${CI_PROJECT_ID}/packages/helm/api/dev/charts")
        }

        job("create-release") {
            stage = Stages.Release
            image("registry.gitlab.com/gitlab-org/release-cli:latest")
            rules = Rules.release
            script("echo 'Running the release job.'")
            release {
                name = "\$CI_COMMIT_TAG"
                tagName = "\$CI_COMMIT_TAG"
                description = "Release \$CI_COMMIT_TAG"
            }
        }
    }
}