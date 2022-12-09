package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.AssignmentReview
import cn.edu.buaa.scs.model.Experiment
import cn.edu.buaa.scs.service.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.experimentRoute() {
    route("/experiments") {
        get {
            val termId = call.request.queryParameters["termId"]?.toInt()
            val submitted = call.request.queryParameters["submitted"]?.toBoolean()
            val courseId = call.request.queryParameters["courseId"]?.toInt()
            call.experiment.getAll(termId, submitted, courseId).let {
                call.respond(it.map { exp -> convertExperimentResponse(call, exp) })
            }
        }

        route("/names") {
            get {
                val courseId = call.request.queryParameters["courseId"]?.toInt()
                    ?: throw BadRequestException("courseId is required")
                call.respond(call.experiment.getNameList(courseId))
            }
        }
    }

    route("/experiment") {

        post {
            val request = call.receive<CreateExperimentRequest>()
            val experiment = call.experiment.create(request)
            call.respond(convertExperimentResponse(call, experiment))
        }

        route("/{experimentId}") {

            fun ApplicationCall.getExpIdFromPath(): Int =
                parameters["experimentId"]?.toInt()
                    ?: throw BadRequestException("experiment id is invalid")

            get {
                val experiment = call.experiment.get(call.getExpIdFromPath())
                call.respond(convertExperimentResponse(call, experiment))
            }

            put {
                val experimentId = call.getExpIdFromPath()
                val request = call.receive<PutExperimentRequest>()
                val experiment = call.experiment.put(experimentId, request)
                call.respond(convertExperimentResponse(call, experiment))
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
                        call.assignment.patch(call.getAssignmentIdFromPath(), req.fileId, req.finalScore).let {
                            call.respond(convertAssignment(it))
                        }
                    }
                }
            }

            route("/selectStandardAssignments") {
                post {
                    call.experiment.selectStandardAssignments(call.getExpIdFromPath()).let { list ->
                        call.respond(list.map { pair -> convertAssignmentWithStandardScore(pair.first, pair.second) })
                    }
                }
            }

            route("/enablePeer") {
                post {
                    call.peer.enable(call.getExpIdFromPath())
                    call.respond("OK")
                }
            }
        }
    }

    route("/assignmentReviews") {
        post {
            val req = call.receive<AssignmentReviewRequest>()
            call.assignmentReview.post(req.assignmentId, req.fileId).let {
                call.respond(convertAssignmentReview(it))
            }
        }

        delete {
            val assignmentId = call.request.queryParameters["assignmentId"]?.toInt()
                ?: throw BadRequestException("assignmentId is required")
            call.assignmentReview.delete(assignmentId)
            call.respond("OK")
        }

        get {
            val assignmentId = call.request.queryParameters["assignmentId"]?.toInt()
                ?: throw BadRequestException("assignmentId is required")
            call.respond(
                call.assignmentReview.get(assignmentId).map { convertAssignmentReview(it) }
            )
        }
    }
}

internal fun convertExperimentResponse(call: ApplicationCall, experiment: Experiment): ExperimentResponse =
    ExperimentResponse(
        id = experiment.id,
        name = experiment.name,
        type = experiment.type,
        detail = experiment.detail,
        resourceFile = experiment.resourceFile?.let { convertFileResponse(it) },
        createTime = experiment.createTime,
        startTime = experiment.startTime,
        endTime = experiment.endTime,
        deadline = experiment.deadline,
        isPeerAssessment = experiment.isPeerAssessment,
        peerAssessmentDeadline = experiment.peerAssessmentDeadline,
        appealDeadline = experiment.appealDeadline,
        peerAssessmentRules = experiment.peerAssessmentRules,
        peerAssessmentStart = experiment.peerAssessmentStart,
        sentEmail = experiment.sentEmail,
        course = call.convertCourseResponse(experiment.course, true),
        vm = experiment.getVmApply()?.let { convertExpVmInfo(it) },
    )

internal fun convertAssignmentList(assignmentList: List<Assignment>): AssignmentListResponse =
    AssignmentListResponse(assignmentList.map { convertAssignment(it) })

internal fun convertAssignment(assignment: Assignment): AssignmentResponse {
    return AssignmentResponse(
        id = assignment.id,
        studentId = assignment.studentId,
        expId = assignment.experimentId,
        courseId = assignment.courseId,
        createdAt = assignment.createdAt,
        updatedAt = assignment.updatedAt,
        file = assignment.file?.let { convertFileResponse(it) },
        assignmentReview = assignment.assignmentReview?.let { convertAssignmentReview(it) },
        finalScore = assignment.finalScore.toDouble(),
        peerScore = assignment.peerScore
    )
}

internal fun convertAssignmentReview(assignmentReview: AssignmentReview): AssignmentReviewResponse =
    AssignmentReviewResponse(
        id = assignmentReview.id,
        assignmentId = assignmentReview.assignmentId,
        fileId = assignmentReview.fileId,
        reviewedAt = assignmentReview.reviewedAt,
        reviewerId = assignmentReview.reviewerId,
        reviewerName = assignmentReview.reviewerName,
    )
