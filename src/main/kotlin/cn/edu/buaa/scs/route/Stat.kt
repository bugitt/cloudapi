package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.StatExpAssignmentResponse
import cn.edu.buaa.scs.controller.models.StatExpAssignmentResponseAssignments
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.service.stat
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.statRoute() {
    route("/stat") {
        route("/exp") {
            route("/{expId}") {
                fun ApplicationCall.getExpIdFromPath() =
                    parameters["expId"]?.toIntOrNull() ?: throw BadRequestException("expId is not an integer")

                route("/assignments") {
                    get {
                        call.respond(
                            convertStatExpAssignmentResponse(
                                call.stat.expAssignments(call.getExpIdFromPath())
                            )
                        )
                    }
                }
            }
        }
    }
}

internal fun convertStatExpAssignmentResponse(assignments: Map<User, Assignment?>): StatExpAssignmentResponse {
    val itemArray = assignments.map { (user, assignment) ->
        StatExpAssignmentResponseAssignments(user.id, user.name, assignment?.let { convertAssignment(it) })
    }
    return StatExpAssignmentResponse(itemArray)
}