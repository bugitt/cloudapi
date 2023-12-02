package cn.edu.buaa.scs.project

import cn.edu.buaa.scs.harbor.HarborClient
import cn.edu.buaa.scs.kube.BusinessKubeClient

interface IProjectManager {
    suspend fun createUser(
        userID: String,
        realName: String,
        email: String,
        password: String,
    ): Result<String>

    suspend fun createProjectForUser(
        userID: String,
        projectName: String,
        projectDisplayName: String = "",
        projectDescription: String = "",
    ): Result<String>

    suspend fun deleteProject(projectName: String): Result<Unit>

    suspend fun addProjectMember(projectName: String, memberID: String): Result<Unit>

    suspend fun removeProjectMember(projectName: String, memberID: String): Result<Unit>

    suspend fun changePassword(username: String, password: String): Result<Unit>
}

val managerList = listOf<IProjectManager>(
//    GitClient,
    HarborClient,
    BusinessKubeClient,
)
