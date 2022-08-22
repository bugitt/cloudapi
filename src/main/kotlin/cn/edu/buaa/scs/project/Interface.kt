package cn.edu.buaa.scs.project

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
        projectDisplayName: String,
        projectDescription: String
    ): Result<String>

    suspend fun deleteProject(projectName: String): Result<Unit>
}