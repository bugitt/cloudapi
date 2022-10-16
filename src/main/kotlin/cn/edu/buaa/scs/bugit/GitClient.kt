package cn.edu.buaa.scs.bugit

import cn.edu.buaa.scs.application
import cn.edu.buaa.scs.error.RemoteServiceException
import cn.edu.buaa.scs.project.IProjectManager
import cn.edu.buaa.scs.utils.getConfigString
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import org.ktorm.jackson.KtormModule

object GitClient : IProjectManager {

    const val gitRepoUrlPrefix = "https://scs.buaa.edu.cn/git/"

    internal val client by lazy {
        HttpClient(CIO) {
            val adminToken = application.getConfigString("bugit.adminToken")
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = application.getConfigString("bugit.host")
                    path(application.getConfigString("bugit.pathPrefix"))

                }
                header(HttpHeaders.Authorization, "token $adminToken")
            }
            install(ContentNegotiation) {
                jackson {
                    registerModule(KtormModule())
                }
            }
        }
    }

    private suspend inline fun <reified T> handleResponse(response: HttpResponse): Result<T> =
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            Result.failure(RemoteServiceException(response.status.value, response.body()))
        }


    internal suspend inline fun <reified T> get(path: String): Result<T> {
        val response = client.get(path)
        return handleResponse(response)
    }

    private suspend inline fun <reified T> post(
        path: String,
        body: Any? = null,
        contentType: ContentType = ContentType.Application.Json,
    ): Result<T> {
        val response = client.post(path) {
            contentType(contentType)
            setBody(body)
        }
        return handleResponse(response)
    }

    private suspend inline fun <reified T> delete(
        path: String,
        body: Any? = null,
        contentType: ContentType = ContentType.Application.Json,
    ): Result<T> {
        val response = client.delete(path) {
            contentType(contentType)
            setBody(body)
        }
        return handleResponse(response)
    }

    private suspend fun deleteRepo(username: String, repoName: String): Result<Unit> {
        return delete<String>("repos/$username/$repoName").map { }
    }

    private suspend fun getProject(projectName: String): Result<GitProject> {
        return get("orgs/$projectName")
    }

    override suspend fun createUser(userID: String, realName: String, email: String, password: String): Result<String> {
        val exception = get<GitUser>("users/$userID").exceptionOrNull()
        return if (exception == null) Result.success(userID)
        else if (exception is RemoteServiceException && exception.status == HttpStatusCode.NotFound.value) {
            post<String>(
                "admin/users", CreateUserReq(
                    username = userID,
                    fullName = realName,
                    email = email,
                    loginName = userID,
                    password = password,
                )
            ).map { userID }
        } else Result.failure(exception)
    }

    override suspend fun createProjectForUser(
        userID: String,
        projectName: String,
        projectDisplayName: String,
        projectDescription: String,
    ): Result<String> {
        val request = CreateOrgRequest(
            projectName,
            projectDisplayName,
            projectDescription,
            "",
            "",
        )
        if (getProject(projectName).isSuccess) return Result.success(projectName)
        return post<GitProject>("admin/users/$userID/orgs", request).map { it.username }
    }

    override suspend fun deleteProject(projectName: String): Result<Unit> {
        // check if the project exists
        val exception = getProject(projectName).exceptionOrNull()
        if (exception != null) {
            return if (exception is RemoteServiceException && exception.status == HttpStatusCode.NotFound.value) {
                Result.success(Unit)
            } else {
                Result.failure(exception)
            }
        }
        // delete all the repos in the project first
        return getRepoListOfProject(projectName).mapCatching { repos ->
            repos.forEach { repo ->
                deleteRepo(projectName, repo.name).getOrThrow()
            }
            delete<String>("admin/users/$projectName").getOrThrow()
        }
    }

    override suspend fun addProjectMember(projectName: String, memberID: String): Result<Unit> {
        return post("/orgs/$projectName/memberships/$memberID")
    }

    override suspend fun removeProjectMember(projectName: String, memberID: String): Result<Unit> {
        return delete("/orgs/$projectName/memberships/$memberID")
    }

    suspend fun getRepoListOfProject(projectName: String): Result<List<GitRepo>> {
        return get("orgs/$projectName/repos")
    }

}