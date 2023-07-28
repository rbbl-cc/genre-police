package cc.rbbl

import cc.rbbl.gitlab_ci_kotlin_dsl_extensions.common_jobs.ciRenderCheckJob
import cc.rbbl.gitlab_ci_kotlin_dsl_extensions.docker.*
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

val gitlabCiSource = DockerImage("\$CI_REGISTRY_IMAGE:\$CI_COMMIT_SHORT_SHA", gitlabDockerCredentials)

val dockerHubCredentials = DockerCredentials("\$DOCKERHUB_USER", "\$DOCKERHUB_ACCESS_TOKEN")

object Targets {
    val DockerHubDev = DockerImage("rbbl/genre-police:dev", dockerHubCredentials)
    val DockerHubLatest = DockerImage("rbbl/genre-police:latest", dockerHubCredentials)
    val DockerHubTagged = DockerImage("rbbl/genre-police:\$CI_COMMIT_TAG", dockerHubCredentials)
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

        val dockerBuildJob = dockerBuildJob(
            "docker-build",
            gitlabCiSource,
            dockerFile = "./app/Dockerfile",
            executable = "podman"
        ) {
            stage = Stages.Build
            image = ImageDsl("quay.io/podman/stable")
            services = services()
            rules {
                +Rules.dev
                +Rules.master
                +Rules.releaseCandidate
                +Rules.release
            }
        }

        dockerMoveJob(
            "docker-publish-dev",
            gitlabCiSource,
            Targets.DockerHubDev,
            executable = "podman"
        ) {
            needs(dockerBuildJob)
            image = ImageDsl("quay.io/podman/stable")
            services = services()
            stage = Stages.Publish
            rules {
                +Rules.dev
            }
        }

        dockerMoveJob(
            "docker-publish-master",
            gitlabCiSource,
            Targets.DockerHubLatest,
            executable = "podman"
        ) {
            needs(dockerBuildJob)
            image = ImageDsl("quay.io/podman/stable")
            services = services()
            stage = Stages.Publish
            rules {
                +Rules.master
            }
        }

        dockerMoveJob(
            "docker-publish-release-candidate",
            gitlabCiSource,
            Targets.DockerHubTagged,
            executable = "podman"
        ) {
            needs(dockerBuildJob)
            image = ImageDsl("quay.io/podman/stable")
            services = services()
            stage = Stages.Publish
            rules {
                +Rules.releaseCandidate
            }
        }

        dockerMoveJob("docker-publish-release", gitlabCiSource, Targets.DockerHubTagged, executable = "podman") {
            needs(dockerBuildJob)
            stage = Stages.Publish
            image = ImageDsl("quay.io/podman/stable")
            services = services()
            rules {
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