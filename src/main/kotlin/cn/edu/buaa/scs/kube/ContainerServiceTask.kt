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

class ContainerServiceTask(taskData: TaskData) : Task(taskData) {
    companion object {
        val client by lazy(businessKubeClientBuilder)
    }

    data class Content(
        val rerun: Boolean = false,
    )

    override suspend fun internalProcess(): Result<Unit> = runCatching {

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
