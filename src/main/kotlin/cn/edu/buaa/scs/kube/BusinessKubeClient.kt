package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.application
import cn.edu.buaa.scs.project.IProjectManager
import cn.edu.buaa.scs.utils.getConfigString
import com.fkorotkov.kubernetes.metadata
import com.fkorotkov.kubernetes.newNamespace

object BusinessKubeClient : IProjectManager {
    val client by lazy(businessKubeClientBuilder)

    val nodeIp by lazy { application.getConfigString("kube.business.nodeIp") }

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