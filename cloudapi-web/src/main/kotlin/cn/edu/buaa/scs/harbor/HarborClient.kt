package cn.edu.buaa.scs.harbor

import cn.edu.buaa.scs.project.IProjectManager
import cn.edu.buaa.scs.sdk.harbor.apis.*
import cn.edu.buaa.scs.sdk.harbor.infrastructure.ClientException
import cn.edu.buaa.scs.sdk.harbor.models.*
import io.ktor.http.*
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object HarborClient : IProjectManager {

    private val clientBuilder by lazy {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
        )

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslContext.socketFactory, (trustAllCerts[0] as X509TrustManager)!!)
        builder.hostnameVerifier { _: String?, _: SSLSession? -> true }
        builder
    }

    private val projectClient by lazy { ProjectApi(client = clientBuilder.build()) }
    private val userClient by lazy { UserApi(client = clientBuilder.build()) }
    private val memberClient by lazy { MemberApi(client = clientBuilder.build()) }
    private val repoClient by lazy { RepositoryApi(client = clientBuilder.build()) }
    private val artifactClient by lazy { ArtifactApi(client = clientBuilder.build()) }

    fun getImagesByProject(projectName: String): Result<Map<Repository, List<Artifact>>> = runCatching {
        val project = projectClient.getProject(projectName)
        repoClient.listRepositories(
            projectName = projectName,
            pageSize = project.repoCount?.toLong(),
        ).associateWith {
            artifactClient.listArtifacts(
                projectName = projectName,
                repositoryName = it.name!!.split("/").last(),
                pageSize = it.artifactCount,
            ).filter { artifact -> artifact.type?.lowercase() == "image" }
        }
    }

    override suspend fun createUser(userID: String, realName: String, email: String, password: String): Result<String> =
        runCatching {
            if (userClient.searchUsers(userID).isNotEmpty()) return Result.success(userID)
            try {
                val userReq = UserCreationReq(
                    email = email,
                    realname = realName,
                    password = password,
                    username = userID,
                )
                userClient.createUser(userReq)
            } catch (e: ClientException) {
                if (e.statusCode == HttpStatusCode.Conflict.value) {
                    val userReq = UserCreationReq(
                        email = email.replace("@", "+$userID@"),
                        realname = realName,
                        password = password,
                        username = userID,
                    )
                    userClient.createUser(userReq)
                } else {
                    throw e
                }
            }
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
        val project = projectClient.getProject(projectName)
        repoClient.listRepositories(
            projectName = projectName,
            pageSize = project.repoCount?.toLong(),
        ).forEach { it.name?.let { repoName -> repoClient.deleteRepository(projectName, repoName) } }
        projectClient.deleteProject(projectName)
    }

    override suspend fun addProjectMember(projectName: String, memberID: String): Result<Unit> = runCatching {
        createProjectMember(projectName, memberID)
    }

    override suspend fun removeProjectMember(projectName: String, memberID: String): Result<Unit> = runCatching {
        deleteProjectMember(projectName, memberID)
    }

    override suspend fun changePassword(username: String, password: String): Result<Unit> = runCatching {
        val users = userClient.searchUsers(username.lowercase())
        if (users.isEmpty()) throw Exception("User $username not found in harbor")
        userClient.updateUserPassword(users[0].userId!!, PasswordReq(newPassword = password))
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
        val members = memberClient.listProjectMembers(projectName)
        val member = members.find { it.entityName?.lowercase()?.trim() == userID.lowercase().trim() }
        if (member != null) {
            return
        }
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
