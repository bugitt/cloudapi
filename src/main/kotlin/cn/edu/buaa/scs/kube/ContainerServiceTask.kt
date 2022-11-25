package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.task.Task
import cn.edu.buaa.scs.utils.convertToMap
import cn.edu.buaa.scs.utils.exists
import cn.edu.buaa.scs.utils.jsonReadValue
import com.fkorotkov.kubernetes.*
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import org.ktorm.entity.update
import io.fabric8.kubernetes.api.model.Container as kubeContainer

suspend fun Container.convertToKubeContainer(): kubeContainer {
    val container = this
    val limitedResource = ResourceUsedRecord.id(container.resourceUsedRecordId).resource
    return newContainer {
        name = container.name
        image = container.image
        container.command?.let { cmd ->
            command = listOf("/bin/sh")
            args = listOf("-c", cmd)
        }
        container.workingDir?.let { workingDir = it }
        env = container.envs?.map { (k, v) ->
            newEnvVar {
                name = k
                value = v
            }
        }
        ports = container.ports?.map { port ->
            newContainerPort {
                name = port.name
                protocol = port.protocol.toString()
                containerPort = port.port
            }
        }
        resources = newResourceRequirements {
            limits = limitedResource.convertToKubeResourceMap()
        }
    }
}

suspend fun Container.releaseResource() = runCatching {
    ResourcePool.id(this.resourcePoolId).release(this.resourceUsedRecordId)
}

suspend fun ContainerService.releaseResource() = runCatching {
    this.containers.forEach { it.releaseResource() }
}

class ContainerServiceTask(taskData: TaskData) : Task(taskData) {
    companion object {
        val client by lazy(businessKubeClientBuilder)
    }

    data class Content(
        val rerun: Boolean = false,
    )

    override suspend fun internalProcess(): Result<Unit> = runCatching {
        val containerService = ContainerService.id(taskData.indexRef)
        val content = jsonReadValue<Content>(taskData.data)
        val project = Project.id(containerService.projectId)
        val containerList = containerService.containers
        val selectorLabels = mapOf("app" to containerService.name)
        val annotations = emptyMap<String, String>() +
                containerList.associate { "resourceUsedRecord-${it.name}" to it.resourceUsedRecordId }
        val containers = containerList.map { it.convertToKubeContainer() }
        val podTemplateSpec = newPodTemplateSpec {
            metadata {
                name = containerService.name
                labels = selectorLabels
                this.annotations = annotations
            }
            spec {
                this.containers = containers
            }
        }
        val labels = convertToMap(containerService) + ("projectName" to project.name)
        val podControllerCreationOption = PodControllerCreationOption(
            name = containerService.name,
            namespace = project.name,
            podTemplateSpec = podTemplateSpec,
            labels = labels,
            selectorLabels = selectorLabels,
            rerun = content.rerun,
        )
        when (containerService.serviceType) {
            ContainerService.Type.SERVICE -> {
                client.createDeploymentSync(podControllerCreationOption, replicas = 1).getOrThrow()
                client.createServiceSync(
                    podControllerCreationOption,
                    containerService.containers.flatMap { it.ports ?: listOf() },
                    export = true,
                ).getOrThrow()
                // update service port
                val servicePorts = client.services().inNamespace(project.name).withName(containerService.name)
                    .get().spec.ports.filter { it.nodePort != 0 }
                containerList.forEach { container ->
                    container.ports = container.ports?.map { port ->
                        val servicePort = servicePorts.find { it.name == port.name }
                        if (servicePort != null) {
                            port.copy(
                                exportIP = BusinessKubeClient.nodeIp,
                                exportPort = servicePort.nodePort,
                            )
                        } else {
                            port
                        }
                    }
                    mysql.containerList.update(container)
                }

            }

            ContainerService.Type.JOB -> {
                client.createJobSync(podControllerCreationOption).getOrThrow()
                // release resource
                containerService.releaseResource().getOrThrow()
            }
        }
    }
}

object ContainerServiceDaemon {

    private val pool by lazy {
        Task.TaskExecutorPool("create-container-service", 10000, 1000000)
            .also { it.start() }
    }

    suspend fun asyncDoOnce() {
        mysql.taskDataList
            .find { (it.type eq Task.Type.ContainerService) and (it.status eq Task.Status.UNDO) }
            ?.let { taskData ->
                pool.send(ContainerServiceTask(taskData))
            }
    }
}

fun ContainerService.getStatus(): ContainerService.Status {
    val client = ContainerServiceTask.client
    val project = Project.id(this.projectId)
    fun existTask(): Boolean =
        mysql.taskDataList.exists { it.type.eq(Task.Type.ContainerService).and(it.indexRef.eq(this.id)) }
    when (this.serviceType) {
        ContainerService.Type.SERVICE -> {
            val deployment = client.apps().deployments().inNamespace(project.name).withName(this.name).get()
                ?: return if (existTask()) ContainerService.Status.FAIL else ContainerService.Status.UNDO
            deployment.status.availableReplicas?.let {
                if (it < (deployment.status.replicas ?: 1)) {
                    return ContainerService.Status.NOT_READY
                } else {
                    return ContainerService.Status.RUNNING
                }
            } ?: return ContainerService.Status.NOT_READY
        }

        ContainerService.Type.JOB -> {
            val job = client.batch().v1().jobs().inNamespace(project.name).withName(this.name).get()
                ?: return if (existTask()) ContainerService.Status.FAIL else ContainerService.Status.UNDO
            val status = job.status
            return when {
                status.active != null && status.active > 0 -> ContainerService.Status.RUNNING
                status.succeeded != null && status.succeeded > 0 -> ContainerService.Status.SUCCESS
                status.failed != null && status.failed > 0 -> ContainerService.Status.FAIL
                else -> ContainerService.Status.NOT_READY
            }
        }
    }
}
