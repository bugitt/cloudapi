package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.application
import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.bugit.CreateRepoRequest
import cn.edu.buaa.scs.bugit.GitClient
import cn.edu.buaa.scs.bugit.GitRepo
import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.harbor.HarborClient
import cn.edu.buaa.scs.kube.BusinessKubeClient
import cn.edu.buaa.scs.kube.crd.v1alpha1.*
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.model.ContainerServiceTemplate
import cn.edu.buaa.scs.model.Project
import cn.edu.buaa.scs.model.ProjectMember
import cn.edu.buaa.scs.project.IProjectManager
import cn.edu.buaa.scs.project.managerList
import cn.edu.buaa.scs.sdk.harbor.models.Artifact
import cn.edu.buaa.scs.storage.bugitDB
import cn.edu.buaa.scs.storage.file.FileManager
import cn.edu.buaa.scs.storage.mongo
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.*
import com.fkorotkov.kubernetes.newObjectMeta
import com.fkorotkov.kubernetes.newSecret
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import org.apache.commons.lang3.RandomStringUtils
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.inList
import org.ktorm.entity.*
import java.util.*

val ApplicationCall.project
    get() = ProjectService.getSvc(this) { ProjectService(this) }

class ProjectService(val call: ApplicationCall) : IService, FileService.FileDecorator {
    companion object : IService.Caller<ProjectService>() {
        const val imageBuildContextLocalDir = "image-build-context"
        const val imagePushSecretName = "push-secret"
        val dockerConfigJsonBase64String by lazy {
            application.getConfigString("image.dockerConfigJsonBase64String")
        }

        object BuilderS3 {
            val endpoint by lazy {
                application.getConfigString("s3.builder.endpoint")
            }
            val bucket by lazy {
                application.getConfigString("s3.builder.bucket")
            }
            val accessKeyID by lazy {
                application.getConfigString("s3.builder.accessKeyID")
            }
            val accessSecretKey by lazy {
                application.getConfigString("s3.builder.accessSecretKey")
            }
            val region by lazy {
                application.getConfigString("s3.builder.region")
            }
        }

    }

    suspend fun createUser(userID: String) {
        createUser(User.id(userID))
    }

    suspend fun createUser(user: User) {
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
            alreadyHasUser = true,
        )

        val resourcePoolName = "personal-${user.id}"
        BusinessKubeClient.createResourcePoolForUser(user.id, 2000, 4096)
        val resourcePool = ResourcePool {
            this.name = resourcePoolName
            this.ownerId = user.id
        }
        if (!mysql.resourcePools.exists { it.name.eq(resourcePoolName) }) {
            mysql.resourcePools.add(resourcePool)
        }

        user.flushChanges()
    }

    internal suspend fun createProjectForUser(
        user: User,
        name: String,
        expID: Int? = null,
        displayName: String = "",
        description: String = "",
        isPersonal: Boolean = false,
        alreadyHasUser: Boolean = false,
    ): Project {
        if (mysql.projects.exists { it.name.eq(name) }) {
            return mysql.projects.find { it.name eq name }!!
        }
        if (!alreadyHasUser) {
            createUser(user)
        }

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
        name: String? = null,
        expID: Int? = null,
        displayName: String = "",
        description: String = "",
        isPersonal: Boolean = false,
    ) = createProjectForUser(
        call.user(),
        if (name.isNullOrBlank()) "p-${call.userId()}-${
            RandomStringUtils.randomAlphanumeric(10).lowercase()
        }" else name,
        expID,
        displayName,
        description,
        isPersonal,
        alreadyHasUser = true,
    )

    fun getProject(projectID: Long): Project {
        val project = Project.id(projectID)
        call.user().assertRead(project)
        return project
    }

    fun getProjects(expID: Int? = null, name: String? = null, ownerId: String? = null): List<Project> {
        if (name != null) {
            val project = mysql.projects.find { it.name eq name } ?: throw NotFoundException("Project not found")
            call.user().assertRead(project)
            return listOf(project)
        }

        if (expID != null) {
            val experiment = Experiment.id(expID)
            return if (call.user().isCourseAdmin(experiment.course)) {
                if (ownerId == null) mysql.projects.filter { it.expID eq expID }.toList()
                else mysql.projects.filter { it.expID eq expID and it.owner.eq(ownerId) }.toList()
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

    suspend fun changePassword(password: String) {
        val user = call.user()
        managerList.forEach {
            it.changePassword(user.id, password).getOrThrow()
        }
        user.paasToken = password
        user.flushChanges()
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

    @Suppress("unused")
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

    suspend fun createImageBuilder(projectID: Long): Builder {
        val project = Project.id(projectID)
        val req = call.receive<PostProjectProjectIdImagesRequest>()
        val builderName = "b-$projectID-${RandomStringUtils.randomAlphanumeric(10).lowercase()}"
        val imageMeta = ImageMeta(project.name, req.name, req.tag ?: "latest")

        // ensure the docker push secret exist
        val secret = newSecret {
            metadata = newObjectMeta {
                name = imagePushSecretName
                namespace = project.name
            }
            type = "kubernetes.io/dockerconfigjson"
            data = mapOf(
                ".dockerconfigjson" to dockerConfigJsonBase64String
            )
        }
        BusinessKubeClient.createOrUpdateSecret(secret).getOrThrow()

        val builder = Builder()
        builder.metadata = newObjectMeta {
            name = builderName
            namespace = project.name
            labels = mapOf(
                "project" to project.name,
                "image.owner" to imageMeta.owner,
                "image.repo" to imageMeta.repo,
                "image.tag" to imageMeta.tag,
                "image.uri" to imageMeta.uri(),
            )
        }

        val builderSpec = BuilderSpec().also {
            it.destination = imageMeta.uri()
            it.dockerfilePath = req.dockerfilePath ?: "./Dockerfile"
            it.pushSecretName = imagePushSecretName
            it.round = -1
        }
        val context = BuilderContext()

        when {
            !req.gitUrl.isNullOrBlank() -> {
                // use the git context
                val gitContext = GitContext().also {
                    it.endpoint = req.gitUrl
                    it.ref = req.gitRef
                    it.scheme = if (req.gitUrl.startsWith("https")) GitContext.Scheme.HTTPS else GitContext.Scheme.HTTP
                    it.userPassword = req.gitPassword
                    it.username = req.gitUsername
                }
                context.git = gitContext
            }

            !req.contextS3ObjectKey.isNullOrBlank() -> {
                val s3Context = S3Context().also {
                    it.accessKeyID = BuilderS3.accessKeyID
                    it.accessSecretKey = BuilderS3.accessSecretKey
                    it.bucket = BuilderS3.bucket
                    it.endpoint = BuilderS3.endpoint
                    it.region = BuilderS3.region
                    it.objectKey = req.contextS3ObjectKey
                }
                context.s3 = s3Context
            }

            !req.dockerfileContent.isNullOrBlank() -> {
                context.raw = req.dockerfileContent
            }

            else -> throw BadRequestException("No context provided")
        }

        builderSpec.context = context

        return BusinessKubeClient.createBuilder(builder).getOrThrow()
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

    fun getContainerServiceList(): List<ContainerService> {
        return mysql.projectMembers
            .filter { it.userId.eq(call.userId()) }
            .map { it.projectId }
            .distinct()
            .flatMap { projectId -> mysql.containerServiceList.filter { it.projectId eq projectId }.toList() }
    }

    fun getContainerLog(namespace: String, podName: String, containerName: String? = null): String? {
        val project = call.project.getProjects(name = namespace).firstOrNull()
            ?: throw NotFoundException("namespace $namespace not found")
        call.user().assertRead(project)
        return BusinessKubeClient.getLog(namespace, podName, containerName).getOrNull()
    }

    fun getResourcePools(): List<String> {
        return mysql.resourcePools.filter { it.ownerId eq call.userId() }.map { it.name }.toList()
    }

    fun getResourcePoolsByProject(projectID: Long): List<String> {
        val project = Project.id(projectID)
        call.user().assertRead(project)
        return getProjectMembers(projectID).map { it.userId }.distinct().flatMap { userID ->
            mysql.resourcePools.filter { it.ownerId eq userID }.map { it.name }.toList()
        }
    }

    suspend fun getReposByProject(projectID: Long): List<Repository> {
        val project = Project.id(projectID)
        call.user().assertRead(project)
        return GitClient.getRepoListOfProject(project.name).getOrThrow().map { gitRepo ->
            Repository(
                name = "${project.name}/${gitRepo.name}",
                repoName = gitRepo.name,
                owner = project.name,
                url = "${GitClient.gitRepoUrlPrefix}/${project.name}/${gitRepo.name}",
                username = call.userId(),
                token = call.user().paasToken,
            )
        }
    }

    fun checkGitRepoNameExist(name: String): Boolean {
        return bugitDB.gitRepoList.exists { it.lowerName eq name.lowercase() }
    }

    suspend fun createGitRepo(projectID: Long, req: PostProjectProjectIdReposRequest): Repository {
        val project = Project.id(projectID)
        call.user().assertWrite(project)
        val gitRepo = GitClient.createRepo(
            project.name, CreateRepoRequest(
                name = req.name,
                description = req.description ?: "",
                private = req.private,
                autoInit = true,
                gitignores = req.gitignores ?: "",
                license = req.license ?: "",
            )
        ).getOrThrow()
        return Repository(
            name = "${project.name}/${gitRepo.name}",
            repoName = gitRepo.name,
            owner = project.name,
            url = "${GitClient.gitRepoUrlPrefix}/${project.name}/${gitRepo.name}",
            username = call.userId(),
            token = call.user().paasToken,
        )
    }

    suspend fun getContainerServiceTemplateList(): List<ContainerServiceTemplate> {
        return mongo.containerServiceTemplateList.find().toList()
    }

    override fun manager(): FileManager = FileManager.buildFileManager("local", imageBuildContextLocalDir)

    override fun fixName(originalName: String?, ownerId: String, involvedId: Int): Pair<String, String> {
        return Pair(
            "image-build-context-tar-$involvedId-${originalName ?: ""}",
            "image-build-context-tar-${UUID.randomUUID()}"
        )
    }

    override fun checkPermission(ownerId: String, involvedId: Int): Boolean = true

    override fun storePath(): String {
        return imageBuildContextLocalDir
    }
}
