package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.bugit.GitRepo
import cn.edu.buaa.scs.controller.models.PostPaasUserRequest
import cn.edu.buaa.scs.controller.models.PostProjectsRequest
import cn.edu.buaa.scs.controller.models.Repository
import cn.edu.buaa.scs.controller.models.SimpleProject
import cn.edu.buaa.scs.model.Project
import cn.edu.buaa.scs.model.ProjectMember
import cn.edu.buaa.scs.model.User
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
        val req = call.receive<PostPaasUserRequest>()
        call.project.createUser(req.userId)
        call.respond("OK")
    }
    route("/projects") {
        get {
            val expID = call.parameters["expId"]?.toInt()
            call.respond(
                call.project.getProjects(expID).map {
                    call.convertSimpleProjectResponse(it)
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
}

suspend fun ApplicationCall.convertProjectResponse(project: Project): ProjectResponse {
    return ProjectResponse(
        id = project.id,
        name = project.name,
        token = this.user().paasToken,
        owner = project.owner,
        repositories = this.project.getAllReposForProject(project.name).map { convertRepositoryResponse(it) },
        members = this.project.getAllMembers(project.id).map { convertProjectMemberResponse(it) },
        displayName = project.displayName,
        description = project.description,
    )
}

fun ApplicationCall.convertSimpleProjectResponse(project: Project): SimpleProject {
    return SimpleProject(
        id = project.id,
        name = project.name,
        token = this.user().paasToken,
        owner = project.owner,
        displayName = project.displayName,
        description = project.description,
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

fun convertRepositoryResponse(repo: GitRepo) =
    Repository(
        name = repo.name,
        url = repo.htmlURL,
    )