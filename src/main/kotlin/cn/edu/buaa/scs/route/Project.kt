package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.DeleteProjectProjectIdMembersRequest
import cn.edu.buaa.scs.controller.models.ImageBuildTask
import cn.edu.buaa.scs.controller.models.PostProjectProjectIdMembersRequest
import cn.edu.buaa.scs.controller.models.PostProjectsRequest
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.service.id
import cn.edu.buaa.scs.service.project
import cn.edu.buaa.scs.utils.user
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
                        req.name,
                        req.expId,
                        req.displayName ?: "",
                        req.description ?: "",
                        req.isPersonal ?: false,
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
                )
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
            }
        }

        route("/repos") {
            get {
                call.respond(
                    call.project.getReposByProject(call.getProjectID())
                )
            }
        }
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