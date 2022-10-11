package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.bugit.GitClient
import cn.edu.buaa.scs.bugit.GitRepo
import cn.edu.buaa.scs.controller.models.Image
import cn.edu.buaa.scs.controller.models.ImageRepo
import cn.edu.buaa.scs.controller.models.PostProjectProjectIdImagesRequest
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.harbor.HarborClient
import cn.edu.buaa.scs.image.ImageBuildTask
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.project.IProjectManager
import cn.edu.buaa.scs.project.managerList
import cn.edu.buaa.scs.sdk.harbor.models.Artifact
import cn.edu.buaa.scs.storage.file.FileManager
import cn.edu.buaa.scs.storage.file.LocalFileManager
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.task.Task
import cn.edu.buaa.scs.utils.*
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.RandomStringUtils
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.inList
import org.ktorm.entity.*
import java.net.URL
import java.util.*

val ApplicationCall.project
    get() = ProjectService.getSvc(this) { ProjectService(this) }

class ProjectService(val call: ApplicationCall) : IService, FileService.IFileManageService {
    companion object : IService.Caller<ProjectService>() {
        const val imageBuildContextLocalDir = "image-build-context"
    }

    suspend fun createUser(userID: String) {
        val user = User.id(userID)
        if (user.paasToken != "") return
        val paasToken = RandomStringUtils.randomAlphanumeric(13)
        managerList.forEach {
            it.createUser(
                userID = user.id,
                realName = user.name,
                email = user.email,
                password = paasToken
            )
        }
        user.paasToken = paasToken
        createProjectForUser(
            user,
            user.personalProjectName(),
            displayName = "${user.id}的个人项目",
            description = "${user.id}的个人项目",
            isPersonal = true,
        )
        user.flushChanges()
    }

    private suspend fun createProjectForUser(
        user: User,
        name: String,
        expID: Int? = null,
        displayName: String = "",
        description: String = "",
        isPersonal: Boolean = false,
    ): Project {
        val experiment = if (expID != null) {
            val experiment = Experiment.id(expID)
            user.assertRead(experiment)
            // 检查之前在该实验下没有创建过Project
            if (mysql.projectMembers.exists { it.expId.eq(expID) and it.userId.eq(user.id) }) {
                throw BadRequestException("You have already created a project in this experiment")
            }
            experiment
        } else {
            null
        }
        if (!name.isValidProjectName()) {
            throw BadRequestException("Project name is invalid")
        }
        if (mysql.projects.exists { it.name.eq(name) }) {
            throw BadRequestException("Project name is duplicated, please use another one")
        }
        return transactionWork(
            { this.createProjectForUser(user.id, name, displayName, description) },
            { this.deleteProject(name) },
        ) {
            mysql.useTransaction {
                val project = Project {
                    this.name = name
                    this.owner = user.id
                    this.expID = expID
                    this.courseID = experiment?.course?.id
                    this.displayName = displayName
                    this.description = description
                    this.isPersonal = isPersonal
                    this.createTime = System.currentTimeMillis()
                }
                mysql.projects.add(project)
                val projectMember = ProjectMember {
                    this.userId = user.id
                    this.username = user.name
                    this.projectId = project.id
                    this.expID = expID
                    this.role = ProjectRole.OWNER
                }
                mysql.projectMembers.add(projectMember)
                project
            }
        }.getOrThrow()
    }

    suspend fun createProjectForCurrentUser(
        name: String,
        expID: Int? = null,
        displayName: String = "",
        description: String = "",
        isPersonal: Boolean = false,
    ) = createProjectForUser(call.user(), name, expID, displayName, description, isPersonal)

    fun getProject(projectID: Long): Project {
        val project = Project.id(projectID)
        call.user().assertRead(project)
        return project
    }

    fun getProjects(expID: Int?): List<Project> {
        if (expID != null) {
            val experiment = Experiment.id(expID)
            return if (call.user().isCourseAdmin(experiment.course)) {
                mysql.projects.filter { it.expID eq expID }.toList()
            } else {
                mysql.projectMembers.find { it.expId.eq(expID) and it.userId.eq(call.userId()) }
                    ?.let { projectMember ->
                        mysql.projects.filter { it.id eq projectMember.projectId }.toList()
                    } ?: listOf()
            }
        }
        if (call.user().isAdmin()) return mysql.projects.toList()
        mysql.projectMembers.filter { it.userId.eq(call.userId()) }.map { it.projectId }.let { projectIds ->
            return if (projectIds.isNotEmpty()) {
                mysql.projects.filter { it.id.inList(projectIds) }.toList()
            } else {
                listOf()
            }
        }
    }

    suspend fun addProjectMember(projectID: Long, memberID: String, role: ProjectRole): ProjectMember {
        val project = Project.id(projectID)
        if (!call.user().isProjectAdmin(project)) {
            throw AuthorizationException("You are not the project admin")
        }
        return transactionWork(
            { this.addProjectMember(project.name, memberID) },
            { this.removeProjectMember(project.name, memberID) },
        ) {
            val projectMember = ProjectMember {
                this.userId = memberID
                this.username = memberID
                this.projectId = project.id
                this.expID = project.expID
                this.role = role
            }
            mysql.projectMembers.add(projectMember)
            projectMember
        }.getOrThrow()
    }

    suspend fun removeProjectMember(projectID: Long, memberID: String): ProjectMember {
        val project = Project.id(projectID)
        if (!call.user().isProjectAdmin(project)) {
            throw AuthorizationException("You are not the project admin")
        }
        val projectMember = mysql.projectMembers.find { it.projectId.eq(projectID) and it.userId.eq(memberID) }
            ?: throw NotFoundException("Project member not found")

        managerList.forEach { it.removeProjectMember(project.name, memberID) }
        projectMember.delete()
        return projectMember
    }

    fun getProjectMembers(projectID: Long): List<ProjectMember> {
        return mysql.projectMembers.filter { it.projectId.eq(projectID) }.toList()
    }

    suspend fun getAllReposForProject(projectName: String): List<GitRepo> {
        return GitClient.getRepoListOfProject(projectName).getOrThrow()
    }

    private suspend fun <T, K, U> transactionWork(
        `do`: suspend IProjectManager.() -> Result<K>,
        undo: suspend IProjectManager.() -> Result<U>,
        dbWork: suspend () -> T
    ): Result<T> {
        for ((i, manager) in managerList.withIndex()) {
            val result = manager.`do`()
            if (result.isFailure) {
                for (j in 0 until i) {
                    managerList[j].undo()
                }
                throw result.exceptionOrNull()!!
            }
        }
        return try {
            Result.success(dbWork())
        } catch (e: Throwable) {
            managerList.forEach { it.undo() }
            Result.failure(e)
        }
    }

    private fun PostProjectProjectIdImagesRequest.fetchGitUrl(): String {
        if (this.gitUrl.isNullOrBlank()) {
            throw BadRequestException("Git url is required")
        }
        var finalGitUrl = this.gitUrl
        if (!finalGitUrl.startsWith("https://") && !finalGitUrl.startsWith("http://")) {
            throw BadRequestException("Git url must start with http:// or https://")
        }
        if (finalGitUrl.startsWith("https://github.com")) {
            finalGitUrl = "https://ghproxy.com/$finalGitUrl"
        }
        if (!finalGitUrl.contains("@scs.buaa.edu.cn")) {
            val protocolPrefix = if (finalGitUrl.startsWith("http://")) "http://" else "https://"
            val gitAuthString = when {
                gitUsername.isNullOrBlank() && gitPassword.isNullOrBlank() -> ""
                gitUsername.isNullOrBlank() -> "$gitPassword@"
                else -> "${gitUsername}:${gitPassword}@"
            }
            finalGitUrl = finalGitUrl.replace(protocolPrefix, "$protocolPrefix$gitAuthString")
        }
        return finalGitUrl
    }

    suspend fun createImageBuildTask(projectID: Long): Pair<ImageMeta, TaskData> {

        val project = Project.id(projectID)

        val req = call.receive<PostProjectProjectIdImagesRequest>()

        val imageMeta = ImageMeta(project.name, req.name, req.tag ?: "latest")

        // handle context

        var contextFileName: String? = null
        var gitUrl: String? = null
        var buildType: ImageBuildTask.BuildType? = null

        val prepareLocalContextTarFile: suspend (suspend (contextTarFilename: String) -> Unit) -> Unit = { prepare ->
            val contextTarFilename = UUID.randomUUID().toString()
            prepare(contextTarFilename)
            contextFileName = contextTarFilename
            buildType = ImageBuildTask.BuildType.LOCAL
        }

        when {
            !req.gitUrl.isNullOrBlank() -> {
                gitUrl = req.fetchGitUrl()
                buildType = ImageBuildTask.BuildType.GIT
            }

            req.contextFileId != null && req.contextFileId > 0 -> prepareLocalContextTarFile {
                val file = File.id(req.contextFileId)
                call.user().assertRead(file)
                file.fileType.manageService(call).manager()
                    .downloadFile(file.storeName, "$imageBuildContextLocalDir/$it")
            }

            !req.contextFileLink.isNullOrBlank() -> prepareLocalContextTarFile {
                withContext(Dispatchers.IO) {
                    URL(req.contextFileLink).openStream().use { input ->
                        val targetFile = java.io.File("$imageBuildContextLocalDir/$it")
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }

            !req.dockerfileContent.isNullOrBlank() -> {
                buildType = ImageBuildTask.BuildType.RawDockerfile
            }
        }

        if (buildType == null) {
            throw BadRequestException("No context provided")
        }

        // handle dockerfile
        val dockerfileConfigmapName = req.dockerfileContent?.let {
            ImageBuildTask.createDockerfileConfigmap(it).getOrThrow()
        }

        val taskContent = ImageBuildTask.Content(
            buildType!!,
            imageMeta,
            contextFileName,
            gitUrl,
            req.gitRef,
            req.dockerfilePath ?: "Dockerfile",
            dockerfileConfigmapName,
            req.workspacePath,
        )

        return mysql.useTransaction {
            val imageBuildTaskIndex = ImageBuildTaskIndex.buildFromImageMeta(projectID, taskContent.imageMeta)
            mysql.imageBuildTaskIndexList.add(imageBuildTaskIndex)
            val taskData = TaskData.create(
                Task.Type.ImageBuild,
                jsonMapper.writeValueAsString(taskContent),
                imageBuildTaskIndex.id,
            )
            mysql.taskDataList.add(taskData)
            imageBuildTaskIndex.taskDataId = taskData.id
            mysql.imageBuildTaskIndexList.update(imageBuildTaskIndex)
            Pair(taskContent.imageMeta, taskData)
        }
    }

    private fun Artifact.toImage(repoName: String?) = Image(
        hostPrefix = ImageMeta.hostPrefix,
        repo = repoName!!,
        digest = this.digest!!,
        tags = this.tags?.map { it.name!! } ?: listOf(),
        imageSize = this.propertySize ?: 0,
        pushTime = this.pushTime?.toInstant()?.toEpochMilli()?.let { if (it < 0) 0 else it },
        pullTime = this.pullTime?.toInstant()?.toEpochMilli()?.let { if (it < 0) 0 else it },
        pullCommand = "docker pull ${ImageMeta.hostPrefix}/${repoName}@${this.digest}",
    )

    fun getImagesByProject(projectID: Long): List<Image> {
        val project = Project.id(projectID)
        call.user().assertRead(project)
        return HarborClient.getImagesByProject(project.name)
            .getOrThrow()
            .flatMap { (repo, artifactList) ->
                artifactList.map { it.toImage(repo.name) }
            }
    }

    fun getImageReposByProject(projectID: Long): List<ImageRepo> {
        val project = Project.id(projectID)
        call.user().assertRead(project)
        return HarborClient.getImagesByProject(project.name)
            .getOrThrow()
            .map { (repo, artifactList) ->
                ImageRepo(
                    name = repo.name!!,
                    artifactsCount = repo.artifactCount ?: 0,
                    downloadCount = repo.pullCount ?: 0,
                    updateTime = repo.updateTime?.toInstant()?.toEpochMilli()?.let { if (it < 0) 0 else it },
                    images = artifactList.map { it.toImage(repo.name) }
                )
            }
    }

    fun getImageBuildTasksByProject(projectID: Long): List<Pair<ImageMeta, TaskData>> {
        val project = Project.id(projectID)
        call.user().assertRead(project)
        val indexIdList = mysql.imageBuildTaskIndexList
            .filter { it.projectId eq projectID }
            .map { it.id }
        if (indexIdList.isEmpty()) return listOf()
        return mysql.taskDataList
            .filter { it.indexRef inList indexIdList }
            .toList()
            .map { taskData ->
                val content = jsonMapper.readValue<ImageBuildTask.Content>(taskData.data)
                Pair(content.imageMeta, taskData)
            }
    }

    override fun manager(): FileManager = LocalFileManager(imageBuildContextLocalDir)

    override fun fixName(originalName: String?, ownerId: String, involvedId: Int): Pair<String, String> {
        return Pair(
            "image-build-context-tar-$involvedId-${originalName ?: ""}",
            "image-build-context-tar-${UUID.randomUUID()}"
        )
    }

    override fun checkPermission(ownerId: String, involvedId: Int): Boolean = true

    override fun storePath(): String {
        return "local-fs:$imageBuildContextLocalDir"
    }
}