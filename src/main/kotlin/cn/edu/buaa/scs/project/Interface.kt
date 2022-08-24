package cn.edu.buaa.scs.project

import cn.edu.buaa.scs.bugit.GitClient
import cn.edu.buaa.scs.harbor.HarborClient

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
}

val managerList = listOf(
    GitClient,
    HarborClient,
)