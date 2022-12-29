package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.application
import cn.edu.buaa.scs.kube.crd.v1alpha1.*
import cn.edu.buaa.scs.project.IProjectManager
import cn.edu.buaa.scs.utils.getConfigString
import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newNamespace
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.Resource
import io.ktor.server.plugins.*

object BusinessKubeClient : IProjectManager {
    val client by lazy(businessKubeClientBuilder)

    private val builderClient by lazy {
        client.resources(
            Builder::class.java,
            BuilderList::class.java,
        ) as MixedOperation<Builder, BuilderList, Resource<Builder>>
    }

    private val deployerClient by lazy {
        client.resources(
            Deployer::class.java,
            DeployerList::class.java,
        ) as MixedOperation<Deployer, DeployerList, Resource<Deployer>>
    }

    private val workflowClient by lazy {
        client.resources(
            Workflow::class.java,
            WorkflowList::class.java,
        ) as MixedOperation<Workflow, WorkflowList, Resource<Workflow>>
    }

    val nodeIp by lazy { application.getConfigString("kube.business.nodeIp") }

    fun getBuilder(name: String, namespace: String): Result<Builder> = runCatching {
        builderClient.inNamespace(namespace).withName(name).get()
            ?: throw NotFoundException("Builder $name not found")
    }

    fun getDeployer(name: String, namespace: String): Result<Deployer> = runCatching {
        deployerClient.inNamespace(namespace).withName(name).get()
            ?: throw NotFoundException("Deployer $name not found")
    }

    fun getWorkflow(name: String, namespace: String): Result<Workflow> = runCatching {
        workflowClient.inNamespace(namespace).withName(name).get()
            ?: throw NotFoundException("Workflow $name not found")
    }

    fun deleteResource(namespace: String, resourceName: String) = runCatching {
        // try to delete service
        client.services().inNamespace(namespace).withName(resourceName).tryGetAndDelete()
        // try to delete deployment
        client.apps().deployments().inNamespace(namespace).withName(resourceName).tryGetAndDelete()
        // try to delete job
        client.batch().v1().jobs().inNamespace(namespace).withName(resourceName).tryGetAndDelete()
        // try to delete statefulset
        client.apps().statefulSets().inNamespace(namespace).withName(resourceName).tryGetAndDelete()
        // try to delete daemonset
        client.apps().daemonSets().inNamespace(namespace).withName(resourceName).tryGetAndDelete()
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
        if (!client.namespaces().withName(projectName).isReady) {
            client.namespaces().resource(
                newNamespace {
                    metadata {
                        name = projectName
                    }
                }
            ).create()
        }
    }.map { projectName }

    override suspend fun deleteProject(projectName: String): Result<Unit> = runCatching {
        client.namespaces().withName(projectName).delete()
    }

    override suspend fun addProjectMember(projectName: String, memberID: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun removeProjectMember(projectName: String, memberID: String): Result<Unit> {
        return Result.success(Unit)
    }
}
