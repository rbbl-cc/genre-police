package cc.rbbl

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

        val dockerBaseJob = job(".docker") {
            image("docker")
            services("docker:dind")
            beforeScript("docker login -u \$CI_REGISTRY_USER -p \$CI_REGISTRY_PASSWORD \$CI_REGISTRY")
        }

        val dockerBuildJob = job("docker-build") {
            needs(buildJob)
            stage = Stages.Build
            extends(dockerBaseJob)
            script(
                "docker build -t \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA ./app",
                "docker push \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA"
            )
        }

        job("docker-publish-dev") {
            needs(dockerBuildJob)
            stage = Stages.Publish
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
            stage = Stages.Publish
            extends(dockerBaseJob)
            script(
                "docker pull \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA",
                "docker tag \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA rbbl/genre-police:latest",
                "docker login -u \$DOCKERHUB_USER -p \$DOCKERHUB_ACCESS_TOKEN",
                "docker push rbbl/genre-police:latest"
            )
            rules = Rules.master
        }

        job("docker-publish-release-candidate") {
            needs(dockerBuildJob)
            stage = Stages.Publish
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

        job("docker-publish-release") {
            needs(dockerBuildJob)
            stage = Stages.Publish
            extends(dockerBaseJob)
            script(
                "docker pull \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA",
                "docker tag \$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA rbbl/genre-police:\$CI_COMMIT_TAG",
                "docker login -u \$DOCKERHUB_USER -p \$DOCKERHUB_ACCESS_TOKEN",
                "docker push rbbl/genre-police:\$CI_COMMIT_TAG"
            )
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