package cc.rbbl

import cc.rbbl.gitlab_ci_kotlin_dsl_extensions.common_jobs.ciRenderCheckJob
import cc.rbbl.gitlab_ci_kotlin_dsl_extensions.container_common.OciCredentials
import cc.rbbl.gitlab_ci_kotlin_dsl_extensions.container_common.OciImage
import cc.rbbl.gitlab_ci_kotlin_dsl_extensions.container_common.gitlabDockerCredentials
import cc.rbbl.gitlab_ci_kotlin_dsl_extensions.podman.podmanBuildJob
import cc.rbbl.gitlab_ci_kotlin_dsl_extensions.podman.podmanMoveJob
import pcimcioch.gitlabci.dsl.gitlabCi
import pcimcioch.gitlabci.dsl.job.ImageDsl
import pcimcioch.gitlabci.dsl.job.createRule

object Stages {
    const val Test = "test"
    const val Build = "build"
    const val Publish = "publish"
    const val Release = "release"
}

object Rules {
    val dev = createRule {
        ifCondition = "\$CI_COMMIT_BRANCH =~ /^dev$/"
    }
    val master = createRule {
        ifCondition = "\$CI_COMMIT_BRANCH == \$CI_DEFAULT_BRANCH"
    }
    val releaseCandidate = createRule {
        ifCondition = "\$CI_COMMIT_TAG =~ /^\\d+\\.\\d+\\.\\d+-RC\\d+$/ && \$CI_PIPELINE_SOURCE =~ /^push$/"
    }
    val release = createRule {
        ifCondition = "\$CI_COMMIT_TAG =~ /^\\d+\\.\\d+\\.\\d+$/ && \$CI_PIPELINE_SOURCE =~ /^push$/"
    }
    val onPush = createRule {
        ifCondition = "\$CI_PIPELINE_SOURCE =~ /^push\$/"
    }
}

val gitlabCiSource = OciImage("\$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA", gitlabDockerCredentials)

val dockerHubCredentials = OciCredentials("\$DOCKERHUB_USER", "\$DOCKERHUB_ACCESS_TOKEN")

object Targets {
    val DockerHubDev = OciImage("rbbl/genre-police:dev", dockerHubCredentials)
    val DockerHubTagged = OciImage("rbbl/genre-police:\$CI_COMMIT_TAG", dockerHubCredentials)
}

const val gradleImage = "gradle:8.2.1"
fun main() {
    gitlabCi(validate = true, "../.gitlab-ci-generated.yml") {
        stages {
            +Stages.Test
            +Stages.Build
            +Stages.Publish
            +Stages.Release
        }

        ciRenderCheckJob("checkCiRender", path = ".gitlab-ci-generated.yml", image = gradleImage) {
            rules {
                +Rules.onPush
            }
        }

        job("test") {
            stage = Stages.Test
            image = ImageDsl(gradleImage)
            script {
                +"gradle check"
            }
            rules {
                +Rules.onPush
            }
        }

        val dockerBuildJob = podmanBuildJob(
            "podman-build", gitlabCiSource, dockerFile = "./app/Dockerfile"
        ) {
            stage = Stages.Build
            rules {
                +Rules.dev
                +Rules.master
                +Rules.releaseCandidate
                +Rules.release
            }
        }

        podmanMoveJob(
            "podman-publish-dev", gitlabCiSource, Targets.DockerHubDev
        ) {
            needs(dockerBuildJob)
            stage = Stages.Publish
            rules {
                +Rules.dev
            }
        }

        podmanMoveJob(
            "podman-publish-release-candidate", gitlabCiSource, Targets.DockerHubTagged
        ) {
            needs(dockerBuildJob)
            stage = Stages.Publish
            rules {
                +Rules.releaseCandidate
            }
        }

        val publishReleaseJob = podmanMoveJob("podman-publish-release", gitlabCiSource, Targets.DockerHubTagged) {
            needs(dockerBuildJob)
            stage = Stages.Publish
            rules {
                +Rules.release
            }
        }

        job("create-short-tags") {
            stage = Stages.Publish
            needs(publishReleaseJob)
            image {
                name = "rbbl/semtag:1"
                entrypoint("/bin/bash", "-cl")
            }
            script("semtag -u \$DOCKERHUB_USER -p \$DOCKERHUB_ACCESS_TOKEN rbbl/semtag:\$CI_COMMIT_TAG")
            rules{
                +Rules.release
            }
        }

        val helmBaseJob = job(".helm") {
            stage = Stages.Publish
            image("fedora")
            variables {
                add("VERSION", "v4.33.2")
                add("BINARY", "yq_linux_amd64")
            }
            beforeScript(
                "dnf in -y openssl",
                "curl -fsLO https://github.com/mikefarah/yq/releases/download/\${VERSION}/\${BINARY} && mv \${BINARY} /usr/bin/yq && chmod +x /usr/bin/yq",
                "curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3",
                "chmod 700 get_helm.sh",
                "./get_helm.sh",
                "yq -i \".appVersion = \\\"\$CI_COMMIT_TAG\\\"\" helm/genre-police/Chart.yaml",
                "yq -i \".version = \\\"\$CHART_VERSION\\\"\" helm/genre-police/Chart.yaml",
                "cat helm/genre-police/Chart.yaml",
                "helm repo add bitnami https://charts.bitnami.com/bitnami",
                "helm dependency build helm/genre-police",
                "helm package helm/genre-police"
            )
        }

        job("helm-publish-release") {
            extends(helmBaseJob)
            script(
                "curl -fs --show-error --request POST --user gitlab-ci-token:\$CI_JOB_TOKEN --form \"chart=@genre-police-\$CHART_VERSION.tgz\" \${CI_API_V4_URL}/projects/\${CI_PROJECT_ID}/packages/helm/api/stable/charts"
            )
            rules {
                rule {
                    ifCondition = "\$CI_COMMIT_TAG =~ /^\\d+\\.\\d+\\.\\d+$/ && \$CI_PIPELINE_SOURCE =~ /^web$/"
                }
            }
        }

        job("helm-publish-release-candidate") {
            extends(helmBaseJob)
            script(
                "curl -fs --show-error --request POST --user gitlab-ci-token:\$CI_JOB_TOKEN --form \"chart=@genre-police-\$CHART_VERSION.tgz\" \${CI_API_V4_URL}/projects/\${CI_PROJECT_ID}/packages/helm/api/dev/charts"
            )
            rules {
                rule {
                    ifCondition = "\$CI_COMMIT_TAG =~ /^\\d+\\.\\d+\\.\\d+-RC\\d+$/ && \$CI_PIPELINE_SOURCE =~ /^web$/"
                }
            }
        }

        job("create-release") {
            stage = Stages.Release
            image("registry.gitlab.com/gitlab-org/release-cli:latest")
            script("echo 'Running the release job.'")
            rules {
                +Rules.release
            }
            release {
                name = "\$CI_COMMIT_TAG"
                tagName = "\$CI_COMMIT_TAG"
                description = "Release \$CI_COMMIT_TAG"
            }
        }
    }
}