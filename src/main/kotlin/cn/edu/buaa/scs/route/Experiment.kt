package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.controller.models.AssignmentRequest
import cn.edu.buaa.scs.controller.models.AssignmentResponse
import cn.edu.buaa.scs.controller.models.PatchAssignmentRequest
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.Experiment
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.service.assignment
import cn.edu.buaa.scs.service.id
import cn.edu.buaa.scs.utils.user
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.experimentRoute() {
    route("/experiment/{experimentId}") {

        fun ApplicationCall.getExpIdFromPath(): Int =
            parameters["experimentId"]?.toInt()
                ?: throw BadRequestException("experiment id is invalid")

        route("/assignments/content") {
            get {
                call.user().assertWrite(Experiment.id(call.getExpIdFromPath()))

            }
        }

        route("/assignment") {

            /**
             * 创建作业
             */
            post {
                val req = call.receive<AssignmentRequest>()
                call.assignment.create(call.getExpIdFromPath(), req.studentId).let {
                    call.respond(convertAssignmentResponse(it))
                }
            }

            route("/{assignmentId}") {


                fun ApplicationCall.getAssignmentIdFromPath(): Int =
                    parameters["assignmentId"]?.toInt()
                        ?: throw BadRequestException("assignmentId id is invalid")

                get {
                    call.assignment.get(call.getAssignmentIdFromPath()).let {
                        call.respond(convertAssignmentResponse(it))
                    }
                }

                /**
                 * 修改作业
                 */
                patch {
                    val req = call.receive<PatchAssignmentRequest>()
                    call.assignment.patch(call.getAssignmentIdFromPath(), req.fileId).let {
                        call.respond(convertAssignmentResponse(it))
                    }
                }
            }
        }
    }
}

fun convertAssignmentResponse(assignment: Assignment): AssignmentResponse {
    val file = if (assignment.fileId != 0) {
        File.id(assignment.fileId)
    } else {
        null
    }
        ?.let { convertFileResponse(it) }

    return AssignmentResponse(
        id = assignment.id,
        studentId = assignment.studentId,
        expId = assignment.experiment.id,
        courseId = assignment.course.id,
        createdAt = assignment.createdAt,
        updatedAt = assignment.updatedAt,
        file = file
    )
}
