package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.application
import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.kube.crd.v1alpha1.*
import cn.edu.buaa.scs.project.IProjectManager
import cn.edu.buaa.scs.service.project
import cn.edu.buaa.scs.utils.WsSessionHolder
import cn.edu.buaa.scs.utils.getConfigString
import cn.edu.buaa.scs.utils.user
import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newNamespace
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.Resource
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import io.ktor.websocket.*
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*

object BusinessKubeClient : IProjectManager {

    private val builderClient by lazy {
        businessKubeClientBuilder().resources(
            Builder::class.java,
            BuilderList::class.java,
        ) as MixedOperation<Builder, BuilderList, Resource<Builder>>
    }

    private val deployerClient by lazy {
        businessKubeClientBuilder().resources(
            Deployer::class.java,
            DeployerList::class.java,
        ) as MixedOperation<Deployer, DeployerList, Resource<Deployer>>
    }

    private val workflowClient by lazy {
        businessKubeClientBuilder().resources(
            Workflow::class.java,
            WorkflowList::class.java,
        ) as MixedOperation<Workflow, WorkflowList, Resource<Workflow>>
    }

    val nodeIp by lazy { application.getConfigString("kube.business.nodeIp") }

    fun getBuilder(name: String, namespace: String): Result<Builder> = runCatching {
        builderClient.inNamespace(namespace).withName(name).get()
            ?: throw NotFoundException("Builder $name not found")
    }

    fun createBuilder(builder: Builder): Result<Builder> = runCatching {
        builderClient.inNamespace(builder.metadata.namespace).resource(builder).createOrReplace()
    }

    fun getDeployer(name: String, namespace: String): Result<Deployer> = runCatching {
        deployerClient.inNamespace(namespace).withName(name).get()
            ?: throw NotFoundException("Deployer $name not found")
    }

    fun getWorkflow(name: String, namespace: String): Result<Workflow> = runCatching {
        workflowClient.inNamespace(namespace).withName(name).get()
            ?: throw NotFoundException("Workflow $name not found")
    }

    fun createOrUpdateSecret(secret: Secret): Result<Secret> = runCatching {
        businessKubeClientBuilder().secrets().inNamespace(secret.metadata.namespace).resource(secret).createOrReplace()
    }

    fun createResourcePool(name: String, cpu: Int, memory: Int, wfConfigId: Long) = runCatching {
        val resourcePoolYamlStr = """
apiVersion: cloudapi.scs.buaa.edu.cn/v1alpha1
kind: ResourcePool
metadata:
  name: $name
  labels:
    wfConfigId: $wfConfigId
spec:
  capacity: 
    cpu: $cpu
    memory: $memory
"""
        businessKubeClientBuilder().load(resourcePoolYamlStr.byteInputStream()).createOrReplace()
    }

    fun deleteResourcePool(name: String) = runCatching {
        val resourcePoolYamlStr = """
apiVersion: cloudapi.scs.buaa.edu.cn/v1alpha1
kind: ResourcePool
metadata:
  name: $name
"""
        businessKubeClientBuilder().load(resourcePoolYamlStr.byteInputStream()).delete()
    }

    fun deleteResource(namespace: String, resourceName: String) = runCatching {
        // try to delete service
        businessKubeClientBuilder().services().inNamespace(namespace).withName(resourceName).tryGetAndDelete()
        // try to delete deployment
        businessKubeClientBuilder().apps().deployments().inNamespace(namespace).withName(resourceName).tryGetAndDelete()
        // try to delete job
        businessKubeClientBuilder().batch().v1().jobs().inNamespace(namespace).withName(resourceName).tryGetAndDelete()
        // try to delete statefulset
        businessKubeClientBuilder().apps().statefulSets().inNamespace(namespace).withName(resourceName)
            .tryGetAndDelete()
        // try to delete daemonset
        businessKubeClientBuilder().apps().daemonSets().inNamespace(namespace).withName(resourceName).tryGetAndDelete()
    }

    override suspend fun createUser(userID: String, realName: String, email: String, password: String): Result<String> {
        return Result.success(userID)
    }

    override suspend fun createProjectForUser(
        userID: String,
        projectName: String,
        projectDisplayName: String,
        projectDescription: String
    ): Result<String> = runCatching {
        if (!businessKubeClientBuilder().namespaces().withName(projectName).isReady) {
            businessKubeClientBuilder().namespaces().resource(
                newNamespace {
                    metadata {
                        name = projectName
                    }
                }
            ).create()
        }
    }.map { projectName }

    fun getLog(namespace: String, podName: String, containerName: String? = null): Result<String> = runCatching {
        val podOp = businessKubeClientBuilder().pods().inNamespace(namespace).withName(podName)
        val pod = podOp.get() ?: throw NotFoundException("pod $podName not found")
        val containerName = containerName ?: pod.spec.containers.first().name
        podOp.inContainer(containerName).tailingLines(1000).getLog(true)
    }

    override suspend fun deleteProject(projectName: String): Result<Unit> = runCatching {
        businessKubeClientBuilder().namespaces().withName(projectName).delete()
    }

    override suspend fun addProjectMember(projectName: String, memberID: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun removeProjectMember(projectName: String, memberID: String): Result<Unit> {
        return Result.success(Unit)
    }
}

fun Route.podLogWsRoute() {
    val connections = Collections.synchronizedSet<WsSessionHolder>(LinkedHashSet())

    webSocket("/ws/kubeLog/{token}") {
        val thisConnection = WsSessionHolder(this)
        connections += thisConnection

        val namespace =
            call.request.queryParameters["namespace"] ?: throw BadRequestException("namespace is required")
        val project = call.project.getProjects(name = namespace).firstOrNull()
            ?: throw NotFoundException("namespace $namespace not found")
        call.user().assertRead(project)

        val podName = call.request.queryParameters["podName"] ?: throw BadRequestException("podName is required")
        val podOp = businessKubeClientBuilder().pods().inNamespace(namespace).withName(podName)
        val pod = podOp.get() ?: throw NotFoundException("pod $podName not found")
        val containerName = call.request.queryParameters["containerName"] ?: pod.spec.containers.first().name
        val loggable = podOp.inContainer(containerName).tailingLines(1000).withPrettyOutput()
        val channel = ByteChannel()
        val logWatch = loggable.watchLog(channel.toOutputStream())
        var hasRead = false

        try {
            while (true) {
                if (!hasRead) {
                    hasRead = true
                    val message = withTimeoutOrNull(3000) {
                        channel.readUTF8Line()
                    }
                    if (message == null) {
                        loggable.getLog(true)
                            .split("\n")
                            .forEach {
                                send(it)
                            }
                        continue
                    } else {
                        send(message)
                        continue
                    }
                }
                val message = channel.readUTF8Line() ?: continue
                send(message)
            }
        } catch (_: Exception) {
        } finally {
            connections -= thisConnection
            logWatch.close()
        }
    }
}
