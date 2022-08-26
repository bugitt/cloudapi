package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.project.IProjectManager

object BusinessKubeClient : IProjectManager {
    val client by lazy(kubeClient)

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
            client.namespaces().withName(projectName).create()
        }
    }.map { projectName }

    override suspend fun deleteProject(projectName: String): Result<Unit> = runCatching {
        client.namespaces().withName(projectName).delete()
    }
}