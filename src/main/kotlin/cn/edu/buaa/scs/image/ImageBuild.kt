package cn.edu.buaa.scs.image

import cn.edu.buaa.scs.application
import cn.edu.buaa.scs.kube.createConfigMapSync
import cn.edu.buaa.scs.kube.createJobSync
import cn.edu.buaa.scs.kube.kubeClient
import cn.edu.buaa.scs.model.ImageMeta
import cn.edu.buaa.scs.model.TaskData
import cn.edu.buaa.scs.model.taskDataList
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.task.Routine
import cn.edu.buaa.scs.task.RoutineTask
import cn.edu.buaa.scs.task.Task
import cn.edu.buaa.scs.utils.getConfigString
import cn.edu.buaa.scs.utils.jsonMapper
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import com.fkorotkov.kubernetes.*
import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.VolumeMount
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import java.util.*

class ImageBuildTask(taskData: TaskData) : Task(taskData) {

    enum class BuildType {
        LOCAL, GIT, RawDockerfile
    }

    companion object {
        const val buildNamespace = "iobs"
        const val dockerfileConfigmapDataKey = "dockerfile"

        suspend fun createDockerfileConfigmap(dockerfile: String): Result<String> {
            val name = "dockerfile-${UUID.randomUUID()}"
            return kubeClient.createConfigMapSync(
                name,
                buildNamespace,
                mapOf(dockerfileConfigmapDataKey to dockerfile)
            ).map { name }
        }
    }

    data class Content(
        @JsonProperty("type") val type: BuildType,
        @JsonProperty("dockerfile_path") val dockerfilePath: String,
        @JsonProperty("image_meta") val imageMeta: ImageMeta,
        @JsonProperty("context_file_name") val contextFileName: String? = null,
        @JsonProperty("git_url") val gitUrl: String? = null,
        @JsonProperty("git_ref") val gitRef: String? = null,
        @JsonProperty("extra_dockerfile_configmap") val extraDockerfileConfigmap: String? = null,
    )


    override suspend fun internalProcess(): Result<Unit> = runCatching {
        val content = jsonMapper.readValue<Content>(taskData.data)

        val buildContextVolumeName = "build-context-tar"
        val dockerfileVolumeName = "dockerfile"

        val workspaceMount = newVolumeMount {
            this.name = "workspace"
            this.mountPath = "/workspace"
        }

        val httpProxy = application.getConfigString("image.httpProxy")
        val httpProxyEnvs = if (httpProxy.isNotBlank()) {
            listOf(
                newEnvVar {
                    this.name = "HTTP_PROXY"
                    this.value = httpProxy
                },
                newEnvVar {
                    this.name = "HTTPS_PROXY"
                    this.value = httpProxy
                },
            )
        } else {
            listOf()
        }

        val buildInitContainer: (String, List<String>, List<VolumeMount>) -> Container =
            { image, command, volumeMounts ->
                newContainer {
                    this.name = "prepare-workspace"
                    this.image = image
                    this.env = httpProxyEnvs
                    this.command = command
                    this.volumeMounts = volumeMounts
                }
            }

        val initContainers: List<Container> = when (content.type) {
            BuildType.RawDockerfile -> listOf()

            BuildType.LOCAL -> listOf(
                buildInitContainer(
                    "loheagn/go-unarr:0.1.6",
                    listOf("/bin/sh", "-c", "unarr /source /workspace"),
                    listOf(
                        workspaceMount,
                        newVolumeMount {
                            this.mountPath = "/source"
                            this.subPath = content.contextFileName!!
                            this.name = buildContextVolumeName
                        }
                    ),
                ),
            )

            BuildType.GIT -> listOf(
                buildInitContainer(
                    "bitnami/git:2",
                    listOf(
                        "/bin/sh",
                        "-c",
                        "git clone ${content.gitUrl} /workspace" +
                                if (content.gitRef.isNullOrBlank()) "" else " && cd /workspace && git checkout ${content.gitRef}"
                    ),
                    listOf(workspaceMount)
                ),
            )
        }

        val mainContainer = newContainer {
            this.name = "builder"
            this.image = "scs.buaa.edu.cn:8081/iobs/kaniko-executor"
            this.args = listOf(
                "--dockerfile=${content.dockerfilePath}",
                "--context=dir:///workspace",
                "--destination=${content.imageMeta.uri()}",
            )
            this.volumeMounts = listOf(
                workspaceMount,
                newVolumeMount {
                    this.name = "push-secret"
                    this.mountPath = "/kaniko/.docker/"
                }
            ).let { baseVolumeMounts ->
                if (!content.extraDockerfileConfigmap.isNullOrBlank()) {
                    baseVolumeMounts + newVolumeMount {
                        this.name = dockerfileVolumeName
                        this.mountPath = "/workspace/Dockerfile"
                        this.subPath = dockerfileConfigmapDataKey
                    }
                } else {
                    baseVolumeMounts
                }
            }
        }

        var volumes = listOf(
            newVolume {
                this.name = "workspace"
                this.emptyDir = newEmptyDirVolumeSource { }
            },
            newVolume {
                this.name = "push-secret"
                secret = newSecretVolumeSource {
                    this.secretName = "push-secret"
                    this.items = listOf(
                        newKeyToPath {
                            this.key = ".dockerconfigjson"
                            this.path = "config.json"
                        }
                    )
                }
            },
        )

        if (content.type == BuildType.LOCAL) {
            volumes = volumes + newVolume {
                this.name = buildContextVolumeName
                this.persistentVolumeClaim = newPersistentVolumeClaimVolumeSource {
                    this.claimName = "image-build-context"
                }
            }
        }

        if (!content.extraDockerfileConfigmap.isNullOrBlank()) {
            volumes = volumes + newVolume {
                this.name = dockerfileVolumeName
                this.configMap = newConfigMapVolumeSource {
                    this.name = content.extraDockerfileConfigmap
                }
            }
        }

        val podTemplate = newPodTemplateSpec {
            this.spec = newPodSpec {
                this.volumes = volumes
                this.initContainers = initContainers
                this.containers = listOf(mainContainer)
                this.restartPolicy = "Never"
            }
        }

        kubeClient.createJobSync("image-build-task-${taskData.id}", buildNamespace, podTemplate).getOrThrow()
    }
}

object ImageBuildRoutine : Routine {

    private val buildImage = run {
        val buildImageExecutorPool = Task.TaskExecutorPool("build-image")
        Routine.alwaysDo(
            "build-image",
            preAction = { buildImageExecutorPool.start() }
        ) {
            mysql.taskDataList
                .filter { it.type.eq(Task.Type.ImageBuild) and it.status.eq(Task.Status.UNDO) }
                .toList()
                .map { ImageBuildTask(it) }
                .forEach {
                    buildImageExecutorPool.send(it)
                }
        }
    }

    override val routineList: List<RoutineTask> = listOf(
        buildImage
    )

}