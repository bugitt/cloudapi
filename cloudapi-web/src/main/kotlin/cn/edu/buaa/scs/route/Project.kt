package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.kube.crd.v1alpha1.Builder
import cn.edu.buaa.scs.kube.getStatus
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.model.ContainerServiceTemplate
import cn.edu.buaa.scs.service.id
import cn.edu.buaa.scs.service.project
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.jsonMapper
import cn.edu.buaa.scs.utils.user
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
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
            val name = call.parameters["name"]
            val ownerId = call.parameters["ownerId"]
            call.respond(
                call.project.getProjects(expID, name, ownerId).map {
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
                val builder = call.project.createImageBuilder(call.getProjectID())
                call.respond(
                    convertImageBuilder(builder)
                )
            }
        }

        route("/imageBuildTasks") {
            get {
                // TODO
                call.respond(
                    ""
//                    call.project.getImageBuildTasksByProject(call.getProjectID())
//                        .map { convertImageBuilder(it) }
                )
            }
        }

        route("/imageRepos") {
            get {
                call.respond(
                    call.project.getImageReposByProject(call.getProjectID())
                )
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
                )
            }
        }
    }

    route("/resourcePools") {
        get {
            call.respond(
                call.project.getResourcePools()
            )
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

    get("/containerLog") {
        val podName = call.parameters["podName"] ?: throw BadRequestException("请提供要查询的pod名称")
        val containerName = call.parameters["containerName"]
        val namespace = call.parameters["namespace"] ?: throw BadRequestException("请提供要查询的命名空间")
        call.project.getContainerLog(namespace, podName, containerName)?.let {
            call.respond(it)
        } ?: call.respond(HttpStatusCode.BadRequest)
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

fun convertImageBuilder(builder: Builder): ImageBuilder {
    val specJsonStr = jsonMapper.writeValueAsString(builder.spec)
    val imageBuilderSpec = jsonMapper.readValue<ImageBuilderSpec>(specJsonStr)
    return ImageBuilder(
        apiVersion = builder.apiVersion,
        kind = builder.kind,
        metadata = ImageBuilderMetadata(
            name = builder.metadata.name,
            namespace = builder.metadata.namespace
        ),
        spec = imageBuilderSpec,
        status = ImageBuilderStatus(
            base = ImageBuilderStatusBase(
                currentRound = builder.status.base?.currentRound ?: 0,
                status = builder.status.base?.status ?: "",
                historyList = builder.status.base?.historyList,
                message = builder.status.base?.message,
            )
        )
    )
}

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
