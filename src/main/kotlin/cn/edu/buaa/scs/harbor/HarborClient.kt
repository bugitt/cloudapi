package cn.edu.buaa.scs.harbor

import cn.edu.buaa.scs.project.IProjectManager
import cn.edu.buaa.scs.sdk.harbor.apis.*
import cn.edu.buaa.scs.sdk.harbor.infrastructure.ClientException
import cn.edu.buaa.scs.sdk.harbor.models.*
import io.ktor.http.*

object HarborClient : IProjectManager {

    private val projectClient by lazy { ProjectApi() }
    private val userClient by lazy { UserApi() }
    private val memberClient by lazy { MemberApi() }
    private val repoClient by lazy { RepositoryApi() }
    private val artifactClient by lazy { ArtifactApi() }

    fun getImagesByProject(projectName: String): Result<Map<Repository, List<Artifact>>> = runCatching {
        repoClient.listRepositories(
            projectName = projectName,
            pageSize = Int.MAX_VALUE.toLong(),
        ).associateWith {
            artifactClient.listArtifacts(
                projectName = projectName,
                repositoryName = it.name!!,
                pageSize = Int.MAX_VALUE.toLong()
            ).filter { artifact -> artifact.type?.lowercase() == "image" }
        }
    }

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
        if (existProject(projectName)) return Result.success(projectName)

        val projectReq = ProjectReq(projectName = projectName, `public` = true)
        projectClient.createProject(projectReq)

        createProjectMember(projectName, userID)
    }.map { projectName }


    override suspend fun deleteProject(projectName: String): Result<Unit> = runCatching {
        repoClient.listRepositories(
            projectName = projectName,
            pageSize = Int.MAX_VALUE.toLong(),
        ).forEach { it.name?.let { repoName -> repoClient.deleteRepository(projectName, repoName) } }
        projectClient.deleteProject(projectName)
    }

    override suspend fun addProjectMember(projectName: String, memberID: String): Result<Unit> = runCatching {
        createProjectMember(projectName, memberID)
    }

    override suspend fun removeProjectMember(projectName: String, memberID: String): Result<Unit> = runCatching {
        deleteProjectMember(projectName, memberID)
    }

    private fun existProject(projectName: String): Boolean {
        return try {
            projectClient.getProject(projectName)
            true
        } catch (e: ClientException) {
            if (e.statusCode == HttpStatusCode.NotFound.value
                || e.statusCode == HttpStatusCode.Forbidden.value
            ) {
                false
            } else {
                throw e
            }
        }
    }

    private fun createProjectMember(projectName: String, userID: String) {
        val users = userClient.searchUsers(userID)
        if (users.isEmpty()) throw Exception("User $userID not found in harbor")
        val userEntity = UserEntity(
            userId = users[0].userId,
            username = users[0].username,
        )
        val projectMember = ProjectMember(
            roleId = 1,
            memberUser = userEntity,
        )
        memberClient.createProjectMember(projectName, projectMember = projectMember)
    }

    private fun deleteProjectMember(projectName: String, userID: String) {
        memberClient.listProjectMembers(projectName, pageSize = Int.MAX_VALUE.toLong()).find { it.entityName == userID }
            ?.let {
                memberClient.deleteProjectMember(projectName, mid = it.id?.toLong() ?: 0)
            }
    }

}