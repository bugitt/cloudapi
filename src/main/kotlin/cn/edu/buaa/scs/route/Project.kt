package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.kube.getStatus
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.model.ContainerServiceTemplate
import cn.edu.buaa.scs.model.Resource
import cn.edu.buaa.scs.model.ResourceExchangeRecord
import cn.edu.buaa.scs.model.ResourcePool
import cn.edu.buaa.scs.model.ResourceUsedRecord
import cn.edu.buaa.scs.service.id
import cn.edu.buaa.scs.service.project
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.user
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import cn.edu.buaa.scs.controller.models.Project as ProjectResponse
import cn.edu.buaa.scs.controller.models.ProjectMember as ProjectMemberResponse

fun Route.projectRoute() {
    post("/paasUser") {
        // this api will do nothing as it is not used anymore
        call.respond("OK")
    }

    route("/projects") {
        get {
            val expID = call.parameters["expId"]?.toInt()
            call.respond(
                call.project.getProjects(expID).map {
                    call.convertProjectResponse(it)
                }
            )
        }

        post {
            val req = call.receive<PostProjectsRequest>()
            call.respond(
                call.convertProjectResponse(
                    call.project.createProjectForCurrentUser(
                        expID = req.expId,
                        displayName = req.displayName,
                        description = req.description ?: "",
                        isPersonal = req.isPersonal ?: false,
                    )
                )
            )
        }
    }

    route("/project/{projectID}") {
        fun ApplicationCall.getProjectID(): Long {
            return parameters["projectID"]?.toLong()
                ?: throw BadRequestException("invalid projectID")
        }

        get {
            val projectID = call.getProjectID()
            call.respond(
                call.convertProjectResponse(
                    call.project.getProject(projectID)
                )
            )
        }

        route("/members") {
            get {
                val projectID = call.getProjectID()
                call.respond(
                    call.project.getProjectMembers(projectID).map {
                        convertProjectMemberResponse(it)
                    }
                )
            }

            post {
                val projectID = call.getProjectID()
                val req = call.receive<PostProjectProjectIdMembersRequest>()
                call.respond(
                    convertProjectMemberResponse(
                        call.project.addProjectMember(projectID, req.userId, ProjectRole.valueOf(req.role))
                    )
                )
            }

            delete {
                val projectID = call.getProjectID()
                val req = call.receive<DeleteProjectProjectIdMembersRequest>()
                call.respond(
                    convertProjectMemberResponse(
                        call.project.removeProjectMember(projectID, req.userId)
                    )
                )
            }
        }

        route("/images") {
            get {
                call.respond(call.project.getImagesByProject(call.getProjectID()))
            }

            post {
                val (imageMeta, taskData) = call.project.createImageBuildTask(call.getProjectID())
                call.respond(
                    convertImageBuildTaskResponse(imageMeta, taskData)
                )
            }
        }

        route("/imageBuildTasks") {
            get {
                call.respond(
                    call.project.getImageBuildTasksByProject(call.getProjectID())
                        .map { convertImageBuildTaskResponse(it) })
            }
        }

        route("/imageRepos") {
            get {
                call.respond(
                    call.project.getImageReposByProject(call.getProjectID())
                )
            }
        }

        route("/containers") {
            post {
                call.project.createContainerService(call.getProjectID(), call.receive())
                call.respond("OK")
            }

            get {
                call.respond(
                    call.project.getContainerServiceListByProject(call.getProjectID())
                        .map { convertContainerServiceResponse(it) }
                )
            }

            route("/fromTemplate") {
                post {
                    val req = call.receive<PostProjectProjectIdContainersFromTemplateRequest>()
                    call.project.createContainerServiceFromTemplate(
                        call.getProjectID(),
                        req.templateId,
                        req.configs.associate { it.name to it.value },
                        req.resourcePoolId,
                        req.limitedResource
                    )
                    call.respond("OK")
                }
            }

            route("/{containerServiceId}") {
                fun ApplicationCall.getContainerServiceId(): Long {
                    return parameters["containerServiceId"]?.toLong()
                        ?: throw BadRequestException("invalid containerServiceId")
                }
                post("/rerun") {
                    call.project.rerunContainerService(call.getProjectID(), call.getContainerServiceId())
                    call.respond("OK")
                }

                delete {
                    call.project.deleteContainerService(call.getProjectID(), call.getContainerServiceId())
                    call.respond("OK")
                }

                get {
                    call.respond(
                        convertContainerServiceResponse(
                            call.project.getContainerService(call.getProjectID(), call.getContainerServiceId())
                        )
                    )
                }
            }
        }

        route("/repos") {
            get {
                call.respond(
                    call.project.getReposByProject(call.getProjectID())
                )
            }

            post {
                val req = call.receive<PostProjectProjectIdReposRequest>()
                val repo = call.project.createGitRepo(call.getProjectID(), req)
                call.respond(repo)
            }
        }

        route("/resourcePools") {
            get {
                call.respond(
                    call.project.getResourcePoolsByProject(call.getProjectID())
                        .map { convertResourcePoolResponse(it) }
                )
            }
        }
    }

    route("/resourcePools") {
        get {
            call.respond(
                call.project.getResourcePools().map {
                    convertResourcePoolResponse(it)
                }
            )
        }

        post {
            val req = call.receive<PostResourcePoolsRequest>()
            call.respond(
                convertResourcePoolResponse(
                    call.project.createResourcePool(
                        req.ownerId,
                        reConvertResource(req.resource),
                    )
                )
            )
        }
    }

    route("/resourceUsedRecords") {
        route("/{resourceUsedRecordId}") {
            fun ApplicationCall.getResourceUsedRecordID(): String {
                return parameters["resourceUsedRecordId"]
                    ?: throw BadRequestException("invalid resourceUsedRecordID")
            }
            get {
                call.respond(
                    convertResourceUsedRecord(
                        call.project.getResourceUsedRecord(call.getResourceUsedRecordID())
                    )
                )
            }
        }

    }

    route("/resourcePools") {
        route("/{resourcePoolId}") {
            fun ApplicationCall.getResourcePoolID(): String {
                return parameters["resourcePoolId"]
                    ?: throw BadRequestException("invalid resourcePoolID")
            }

            get {
                call.respond(
                    convertResourcePoolResponse(
                        call.project.getResourcePool(call.getResourcePoolID())
                    )
                )
            }
        }
    }

    route("/containerServices") {
        get {
            call.respond(
                call.project.getContainerServiceList().map {
                    convertContainerServiceResponse(it)
                }
            )
        }
    }

    route("/containerServiceTemplates") {
        get {
            call.project
                .getContainerServiceTemplateList()
                .groupBy { it.category }
                .map { (category, list) ->
                    GetContainerServiceTemplates200ResponseInner(
                        category,
                        list.groupBy { it.segment }.map { (segment, list) ->
                            GetContainerServiceTemplates200ResponseInnerSegmentsInner(
                                segment ?: "其他",
                                list.map { convertContainerServiceTemplate(it) }
                            )
                        }
                    )
                }.let {
                    call.respond(it)
                }
        }
    }

    get("/repos/name/exist") {
        val name = call.parameters["name"] ?: throw BadRequestException("请提供要查询的名称")
        call.respond(call.project.checkGitRepoNameExist(name))
    }
}

fun ApplicationCall.convertProjectResponse(project: Project): ProjectResponse {
    return ProjectResponse(
        id = project.id,
        name = project.name,
        token = this.user().paasToken,
        owner = project.owner,
        displayName = project.displayName,
        description = project.description,
        expId = project.expID,
        createdTime = project.createTime,
    )
}

fun convertProjectMemberResponse(projectMember: ProjectMember) =
    ProjectMemberResponse(
        id = projectMember.id,
        userId = projectMember.userId,
        username = User.id(projectMember.userId).name,
        projectId = projectMember.projectId,
        role = projectMember.role.name,
    )

fun convertContainerResponse(container: Container) = ContainerResponse(
    id = container.id,
    name = container.name,
    image = container.image,
    command = container.command,
    workingDir = container.workingDir,
    envs = container.envs?.map { (key, value) -> KvPair(key, value) },
    ports = container.ports?.map {
        ContainerServicePort(
            name = it.name,
            port = it.port,
            protocol = it.protocol.name,
            exportIP = it.exportIP,
            exportPort = it.exportPort,
        )
    },
    resourcePoolId = container.resourcePoolId,
    resourceUsedRecordId = container.resourceUsedRecordId,
)

fun convertContainerServiceResponse(
    containerService: ContainerService,
    getStatus: Boolean = true
): ContainerServiceResponse {
    val containers = mysql.containerList
        .filter { it.serviceId eq containerService.id }
        .toList()
        .map { convertContainerResponse(it) }
    val resp = ContainerServiceResponse(
        id = containerService.id,
        name = containerService.name,
        serviceType = containerService.serviceType.name,
        createdTime = containerService.createTime,
        containers = containers,
        creator = containerService.creator,
        projectId = containerService.projectId,
        projectName = containerService.projectName,
        templateId = containerService.templateId,
    )
    if (getStatus) return resp.copy(status = containerService.getStatus().name)
    return resp
}

fun convertImageBuildTaskResponse(imageMeta: ImageMeta, taskData: TaskData) = ImageBuildTask(
    hostPrefix = ImageMeta.hostPrefix,
    owner = imageMeta.owner,
    repo = imageMeta.repo,
    tag = imageMeta.tag,
    status = taskData.status.toString(),
    createdTime = taskData.createTime,
    endTime = taskData.endTime,
)

fun convertImageBuildTaskResponse(imageWithTaskData: Pair<ImageMeta, TaskData>) =
    convertImageBuildTaskResponse(imageWithTaskData.first, imageWithTaskData.second)

fun convertResource(resource: Resource) = cn.edu.buaa.scs.controller.models.Resource(
    cpu = resource.cpu,
    memory = resource.memory,
)

fun reConvertResource(resource: cn.edu.buaa.scs.controller.models.Resource) = Resource(
    cpu = resource.cpu,
    memory = resource.memory,
)

fun convertResourceExchangeRecord(record: ResourceExchangeRecord) =
    cn.edu.buaa.scs.controller.models.ResourceExchangeRecord(
        id = record._id.toString(),
        sender = record.sender,
        receiver = record.receiver,
        resource = convertResource(record.resource),
        time = record.time,
    )

fun convertResourceUsedRecord(record: ResourceUsedRecord) =
    cn.edu.buaa.scs.controller.models.ResourceUsedRecord(
        id = record._id.toString(),
        resource = convertResource(record.resource),
        projectId = record.project.id,
        containerServiceId = record.containerService.id,
        containerId = record.container.id,
        released = record.released,
        time = record.time,
    )

suspend fun convertResourcePoolResponse(pool: ResourcePool) =
    cn.edu.buaa.scs.controller.models.ResourcePool(
        id = pool._id.toString(),
        name = pool.name,
        ownerId = pool.ownerId,
        capacity = convertResource(pool.capacity),
        used = convertResource(pool.used),
        usedRecordList = pool.usedRecordList.map { convertResourceUsedRecord(ResourceUsedRecord.id(it)) },
        exchangeRecordList = pool.exchangeRecordList.map { convertResourceExchangeRecord(ResourceExchangeRecord.id(it)) },
        time = pool.time,
    )

fun convertContainerServiceTemplate(template: ContainerServiceTemplate) =
    cn.edu.buaa.scs.controller.models.ContainerServiceTemplate(
        id = template._id.toString(),
        name = template.name,
        description = template.description,
        category = template.category,
        segment = template.segment,
        iconUrl = template.iconUrl,
        config = template.configs.map { convertContainerServiceTemplateConfigItem(it) }
    )

fun convertContainerServiceTemplateConfigItem(item: ContainerServiceTemplate.ConfigItem) =
    ContainerServiceTemplateConfigItem(
        name = item.name,
        label = item.label,
        type = item.type.toString().lowercase(),
        required = item.required,
        options = item.options,
        default = item.default,
        description = item.description,
    )
