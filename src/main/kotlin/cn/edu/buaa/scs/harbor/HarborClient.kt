package cn.edu.buaa.scs.harbor

import cn.edu.buaa.scs.project.IProjectManager
import cn.edu.buaa.scs.sdk.harbor.apis.MemberApi
import cn.edu.buaa.scs.sdk.harbor.apis.ProjectApi
import cn.edu.buaa.scs.sdk.harbor.apis.RepositoryApi
import cn.edu.buaa.scs.sdk.harbor.apis.UserApi
import cn.edu.buaa.scs.sdk.harbor.models.ProjectMember
import cn.edu.buaa.scs.sdk.harbor.models.ProjectReq
import cn.edu.buaa.scs.sdk.harbor.models.UserCreationReq
import cn.edu.buaa.scs.sdk.harbor.models.UserEntity

object HarborClient : IProjectManager {

    private val projectClient by lazy { ProjectApi() }
    private val userClient by lazy { UserApi() }
    private val memberClient by lazy { MemberApi() }
    private val repoClient by lazy { RepositoryApi() }

    override suspend fun createUser(userID: String, realName: String, email: String, password: String): Result<String> =
        runCatching {
            if (userClient.searchUsers(userID).isNotEmpty()) return Result.success(userID)
            val userReq = UserCreationReq(
                email = email,
                realname = realName,
                password = password,
                username = userID,
            )
            userClient.createUser(userReq)
        }.map { userID }


    override suspend fun createProjectForUser(
        userID: String,
        projectName: String,
        projectDisplayName: String,
        projectDescription: String
    ): Result<String> = runCatching {
        val users = userClient.searchUsers(userID)
        if (users.isEmpty()) return Result.failure(Exception("User $userID not found in harbor"))
        val userEntity = UserEntity(
            userId = users[0].userId,
            username = users[0].username,
        )
        val projectReq = ProjectReq(projectName = projectName, `public` = true)
        projectClient.createProject(projectReq)
        val projectMember = ProjectMember(
            roleId = 1,
            memberUser = userEntity,
        )
        memberClient.createProjectMember(projectName, projectMember = projectMember)
    }.map { projectName }


    override suspend fun deleteProject(projectName: String): Result<Unit> = runCatching {
        repoClient.listRepositories(
            projectName = projectName,
            pageSize = Int.MAX_VALUE.toLong(),
        ).forEach { it.name?.let { repoName -> repoClient.deleteRepository(projectName, repoName) } }
        projectClient.deleteProject(projectName)
    }

}