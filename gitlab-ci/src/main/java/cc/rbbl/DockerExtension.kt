package cc.rbbl

import pcimcioch.gitlabci.dsl.GitlabCiDsl
import pcimcioch.gitlabci.dsl.job.JobDsl
import pcimcioch.gitlabci.dsl.job.createJob

data class DockerCredentials(val username: String, val password: String, val registry: String = "docker.io") {
    fun toLoginCommand(): String = "docker login -u $username -p $password $registry"
}

enum class DockerDirection(val instruction: String) {
    PUSH("push"), PULL("pull")
}

abstract class DockerSourceTarget(val imageTag: String, val dockerCredentials: DockerCredentials) {
    abstract val direction: DockerDirection

    fun toShellCommand(): String = "docker ${direction.instruction} $imageTag"
}

class DockerSource(source: String, dockerCredentials: DockerCredentials) :
    DockerSourceTarget(source, dockerCredentials) {
    override val direction: DockerDirection
        get() = DockerDirection.PULL
}

class DockerTarget(targetImage: String, dockerCredentials: DockerCredentials) :
    DockerSourceTarget(targetImage, dockerCredentials) {
    override val direction: DockerDirection
        get() = DockerDirection.PUSH
}

fun createDockerBuildJob(
    name: String,
    targets: List<DockerTarget>,
    context: String = ".",
    dockerVersion: String? = null,
    block: JobDsl.() -> Unit = {}
): JobDsl {
    return createJob(name) {
        image(
            if (dockerVersion == null) {
                "docker"
            } else {
                "docker:$dockerVersion"
            }
        )
        services(
            if (dockerVersion == null) {
                "docker:dind"
            } else {
                "docker:$dockerVersion-dind"
            }
        )
        val scripts = ArrayList<String>()
        scripts.add(targets.fold("docker build") { buildCommand: String, dockerTarget: DockerTarget -> buildCommand + " -t ${dockerTarget.imageTag}" } + " $context")
        targets.forEach {
            scripts.add(it.dockerCredentials.toLoginCommand())
            scripts.add(it.toShellCommand())
        }
        script(scripts)
    }.apply(block)
}

fun createDockerBuildJob(
    name: String,
    target: DockerTarget,
    context: String = ".",
    dockerVersion: String? = null,
    block: JobDsl.() -> Unit = {}
) = createDockerBuildJob(name, listOf(target), context, dockerVersion, block)

fun GitlabCiDsl.dockerBuildJob(
    name: String,
    targets: List<DockerTarget>,
    context: String = ".",
    dockerVersion: String? = null,
    block: JobDsl.() -> Unit = {}
): JobDsl {
    val job = createDockerBuildJob(name, targets, context, dockerVersion, block)
    +job
    return job
}

fun GitlabCiDsl.dockerBuildJob(
    name: String,
    target: DockerTarget,
    context: String = ".",
    dockerVersion: String? = null,
    block: JobDsl.() -> Unit = {}
): JobDsl {
    val job = createDockerBuildJob(name, target, context, dockerVersion, block)
    +job
    return job
}

val gitlabDockerCredentials = DockerCredentials("\$CI_REGISTRY_USER", "\$CI_REGISTRY_PASSWORD", "\$CI_REGISTRY")