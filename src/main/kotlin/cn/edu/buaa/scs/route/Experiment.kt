package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.Experiment
import cn.edu.buaa.scs.service.assignment
import cn.edu.buaa.scs.service.experiment
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.experimentRoute() {
    route("/experiment/{experimentId}") {

        fun ApplicationCall.getExpIdFromPath(): Int =
            parameters["experimentId"]?.toInt()
                ?: throw BadRequestException("experiment id is invalid")

        get {
            val experiment = call.experiment.get(call.getExpIdFromPath())
            call.respond(convertExperiment(experiment))
        }

        route("/assignments") {
            get {
                call.respond(
                    convertAssignmentList(call.assignment.getAll(call.getExpIdFromPath()))
                )
            }
        }

        route("/assignment") {

            /**
             * 创建作业
             */
            post {
                val req = call.receive<AssignmentRequest>()
                call.assignment.create(call.getExpIdFromPath(), req.studentId).let {
                    call.respond(convertAssignment(it))
                }
            }

            route("/{assignmentId}") {


                fun ApplicationCall.getAssignmentIdFromPath(): Int =
                    parameters["assignmentId"]?.toInt()
                        ?: throw BadRequestException("assignmentId id is invalid")

                get {
                    call.assignment.get(call.getAssignmentIdFromPath()).let {
                        call.respond(convertAssignment(it))
                    }
                }

                /**
                 * 修改作业
                 */
                patch {
                    val req = call.receive<PatchAssignmentRequest>()
                    call.assignment.patch(call.getAssignmentIdFromPath(), req.fileId).let {
                        call.respond(convertAssignment(it))
                    }
                }
            }
        }
    }
}

internal fun convertExperiment(experiment: Experiment): ExperimentResponse =
    ExperimentResponse(
        id = experiment.id,
        name = experiment.name,
        type = experiment.type,
        detail = experiment.detail,
        resource = if (experiment.resource.isEmpty() || experiment.resource == "null") null else experiment.resource,
        createTime = experiment.createTime,
        startTime = experiment.startTime,
        endTime = experiment.endTime,
        deadLine = experiment.deadLine,
        isPeerAssessment = experiment.isPeerAssessment,
        peerAssessmentDeadline = experiment.peerAssessmentDeadline,
        appealDeadline = experiment.appealDeadline,
        peerAssessmentRules = experiment.peerAssessmentRules,
        peerAssessmentStart = experiment.peerAssessmentStart,
        sentEmail = experiment.sentEmail,
        course = convertCourse(experiment.course),
    )

internal fun convertAssignmentList(assignmentList: List<Assignment>): AssignmentListResponse =
    AssignmentListResponse(assignmentList.map { convertAssignment(it) })

internal fun convertAssignment(assignment: Assignment): AssignmentResponse {
    return AssignmentResponse(
        id = assignment.id,
        studentId = assignment.studentId,
        expId = assignment.experiment.id,
        courseId = assignment.course.id,
        createdAt = assignment.createdAt,
        updatedAt = assignment.updatedAt,
        file = assignment.file?.let { convertFileResponse(it) }
    )
}
