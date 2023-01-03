package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.bugit.GitClient
import cn.edu.buaa.scs.bugit.GitRepo
import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.harbor.HarborClient
import cn.edu.buaa.scs.image.ImageBuildTask
import cn.edu.buaa.scs.kube.BusinessKubeClient
import cn.edu.buaa.scs.kube.ContainerServiceTask
import cn.edu.buaa.scs.kube.releaseResource
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.model.ContainerServiceTemplate
import cn.edu.buaa.scs.model.Project
import cn.edu.buaa.scs.model.ProjectMember
import cn.edu.buaa.scs.model.Resource
import cn.edu.buaa.scs.model.ResourcePool
import cn.edu.buaa.scs.model.ResourceUsedRecord
import cn.edu.buaa.scs.project.IProjectManager
import cn.edu.buaa.scs.project.managerList
import cn.edu.buaa.scs.sdk.harbor.models.Artifact
import cn.edu.buaa.scs.storage.bugitDB
import cn.edu.buaa.scs.storage.file.FileManager
import cn.edu.buaa.scs.storage.mongo
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
import org.ktorm.dsl.delete
import org.ktorm.dsl.eq
import org.ktorm.dsl.inList
import org.ktorm.entity.*
import org.litote.kmongo.eq
import java.net.URL
import java.util.*

val ApplicationCall.project
    get() = ProjectService.getSvc(this) { ProjectService(this) }

class ProjectService(val call: ApplicationCall) : IService, FileService.FileDecorator {
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
        isPersonal
    )

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
                file.fileType.decorator(call).manager()
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
            .filter { it.type.eq(Task.Type.ImageBuild).and(it.indexRef inList indexIdList) }
            .toList()
            .map { taskData ->
                val content = jsonMapper.readValue<ImageBuildTask.Content>(taskData.data)
                Pair(content.imageMeta, taskData)
            }
    }

    suspend fun createContainerServiceFromTemplate(
        projectID: Long,
        templateId: String,
        configs: Map<String, String>,
        resourcePoolId: String,
        limitedResource: cn.edu.buaa.scs.controller.models.Resource
    ) {
        val template = ContainerServiceTemplate.id(templateId)
        var image = template.baseImage
        val envs = mutableListOf<KvPair>()
        template.configs.forEach {
            when (it.target) {
                ContainerServiceTemplate.ConfigItem.Target.TAG -> {
                    image += ":" + (configs[it.name] ?: it.default)
                }

                ContainerServiceTemplate.ConfigItem.Target.ENV -> {
                    val key = it.name
                    val value = configs[it.name]
                        ?: if (it.required) throw BadRequestException("Missing config: $key") else it.default
                    if (value != null) {
                        envs.add(KvPair(key, value))
                    }
                }
            }
        }
        val containerRequest = ContainerRequest(
            name = "container-main",
            image = image,
            resourcePoolId = resourcePoolId,
            limitedResource = limitedResource,
            envs = envs,
            ports = template.portList.map {
                ContainerServicePort(
                    name = "${it.protocol}-${it.port}",
                    port = it.port,
                    protocol = it.protocol.name,
                )
            },
        )
        val containerServiceRequest = ContainerServiceRequest(
            name = "${template.name}-${RandomStringUtils.randomNumeric(3)}",
            serviceType = "SERVICE",
            containers = listOf(containerRequest),
        )
        createContainerService(projectID, containerServiceRequest)
    }

    suspend fun createContainerService(projectID: Long, req: ContainerServiceRequest) {
        val project = Project.id(projectID)
        call.user().assertWrite(project)
        // check name valid
        if (!req.name.isValidProjectName()) {
            throw BadRequestException("Service name invalid")
        }
        // check name conflict
        if (mysql.containerServiceList.exists { it.projectId.eq(projectID).and(it.name.eq(req.name)) }) {
            throw BadRequestException("Service name conflict")
        }
        // check permission to use the resourcePool
        req.containers.forEach { container ->
            call.user().assertWrite(ResourcePool.id(container.resourcePoolId))
        }
        mysql.useTransaction {
            val containerService = ContainerService {
                this.name = req.name
                this.creator = call.userId()
                this.projectId = projectID
                this.projectName = project.name
                this.serviceType = ContainerService.Type.valueOf(req.serviceType)
                this.createTime = System.currentTimeMillis()
                this.templateId = null
            }
            mysql.containerServiceList.add(containerService)
            req.containers.forEach { containerReq ->
                val container = Container {
                    this.name = containerReq.name
                    this.image = containerReq.image
                    this.command = containerReq.command
                    this.workingDir = containerReq.workingDir
                    this.envs = containerReq.envs?.associate { it.name to it.value }
                    this.ports = containerReq.ports?.map {
                        ContainerService.Port(
                            it.name,
                            it.port,
                            IPProtocol.valueOf(it.protocol)
                        )
                    }
                    this.serviceId = containerService.id
                    this.resourcePoolId = containerReq.resourcePoolId
                    this.resourceUsedRecordId = ""
                }
                mysql.containerList.add(container)
                val reqResource = containerReq.limitedResource
                val (_, resourceUsedRecord) =
                    ResourcePool.id(container.resourcePoolId)
                        .use(Resource(reqResource.cpu, reqResource.memory), project, container, containerService)
                container.resourceUsedRecordId = resourceUsedRecord._id.toString()
                mysql.containerList.update(container)
            }
            val taskData = TaskData.create(
                Task.Type.ContainerService,
                ContainerServiceTask.Content(
                    rerun = false,
                ),
                containerService.id,
            )
            mysql.taskDataList.add(taskData)
        }
    }

    fun getContainerServiceList(): List<ContainerService> {
        return mysql.projectMembers
            .filter { it.userId.eq(call.userId()) }
            .map { it.projectId }
            .distinct()
            .flatMap { projectId -> mysql.containerServiceList.filter { it.projectId eq projectId }.toList() }
    }

    suspend fun deleteContainerService(projectID: Long, serviceID: Long) {
        val project = Project.id(projectID)
        call.user().assertWrite(project)
        mysql.useTransaction {
            val containerService = ContainerService.id(serviceID)
            if (containerService.projectId != projectID) {
                throw NotFoundException("Service not found")
            }
            BusinessKubeClient.deleteResource(project.name, containerService.name).getOrThrow()
            containerService.releaseResource().getOrThrow()
            containerService.delete()
            mysql.delete(ContainerList) { it.serviceId eq serviceID }
        }
    }

    fun getContainerService(projectID: Long, serviceID: Long): ContainerService {
        val project = Project.id(projectID)
        call.user().assertWrite(project)
        return ContainerService.id(serviceID)
    }

    fun rerunContainerService(projectID: Long, serviceID: Long) {
        val project = Project.id(projectID)
        call.user().assertWrite(project)
        val containerService = ContainerService.id(serviceID)
        if (containerService.projectId != projectID) {
            throw NotFoundException("Service not found")
        }
        mysql.useTransaction {
            val taskData = TaskData.create(
                Task.Type.ContainerService,
                ContainerServiceTask.Content(rerun = true),
                containerService.id,
            )
            mysql.taskDataList.add(taskData)
            containerService.createTime = System.currentTimeMillis()
            mysql.containerServiceList.update(containerService)
        }
    }

    fun getContainerServiceListByProject(projectID: Long): List<ContainerService> {
        if (projectID <= 0) return getContainerServiceList()
        val project = Project.id(projectID)
        call.user().assertRead(project)
        return mysql.containerServiceList
            .filter { it.projectId eq projectID }
            .toList()
    }

    suspend fun createResourcePool(userID: String, resource: Resource): ResourcePool {
        if (!call.user().isAdmin()) throw AuthorizationException("Permission denied")

        val resourcePool = ResourcePool(
            name = "$userID-${RandomStringUtils.randomNumeric(5)}",
            ownerId = userID,
            capacity = resource,
        )
        mongo.resourcePool.insertOne(resourcePool)
        return resourcePool
    }

    private suspend fun getResourcePoolsByUser(userID: String): List<ResourcePool> {
        return mongo.resourcePool.find(ResourcePool::ownerId eq userID).toList()
    }

    suspend fun getResourcePools(): List<ResourcePool> {
        return getResourcePoolsByUser(call.userId())
    }

    suspend fun getResourceUsedRecord(id: String): ResourceUsedRecord {
        return ResourceUsedRecord.id(id)
    }

    suspend fun getResourcePool(id: String): ResourcePool {
        return ResourcePool.id(id)
    }

    suspend fun getResourcePoolUsedStat(resourcePoolId: String): GetStatResourcePoolsResourcePoolIdUsed200Response {
        val resourcePool = ResourcePool.id(resourcePoolId)

        // TODO 权限控制

        val usedRecordList = resourcePool.usedRecordList
            .map { ResourceUsedRecord.id(it) }
            .filter { !it.released }

        val cpuItemList = mutableListOf<ResourceUsedStatItem>()
        val memoryItemList = mutableListOf<ResourceUsedStatItem>()

        usedRecordList.forEach {
            val name = "${it.project.name} / ${it.containerService.name}"
            cpuItemList.add(ResourceUsedStatItem(name, it.resource.cpu))
            memoryItemList.add(ResourceUsedStatItem(name, it.resource.memory))
        }

        (resourcePool.capacity.cpu - cpuItemList.sumOf { it.value }).let {
            if (it > 0) {
                cpuItemList.add(ResourceUsedStatItem("空闲", it))
            }
        }

        (resourcePool.capacity.memory - memoryItemList.sumOf { it.value }).let {
            if (it > 0) {
                memoryItemList.add(ResourceUsedStatItem("空闲", it))
            }
        }

        return GetStatResourcePoolsResourcePoolIdUsed200Response(cpuItemList, memoryItemList)
    }

    suspend fun getResourcePoolsByProject(projectID: Long): List<ResourcePool> {
        val project = Project.id(projectID)
        call.user().assertRead(project)
        return getProjectMembers(projectID).map { it.userId }.distinct().flatMap { userID ->
            getResourcePoolsByUser(userID)
        }
    }

    suspend fun getReposByProject(projectID: Long): List<Repository> {
        val project = Project.id(projectID)
        call.user().assertRead(project)
        return GitClient.getRepoListOfProject(project.name).getOrThrow().map { gitRepo ->
            Repository(
                name = "${project.name}/${gitRepo.name}",
                url = "${GitClient.gitRepoUrlPrefix}/${project.name}/${gitRepo.name}",
                username = call.userId(),
                token = call.user().paasToken,
            )
        }
    }

    fun checkGitRepoNameExist(name: String): Boolean {
        return bugitDB.gitRepoList.exists { it.lowerName eq name.lowercase() }
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
