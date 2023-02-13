package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.PeerAppeal
import cn.edu.buaa.scs.model.PeerStandard
import cn.edu.buaa.scs.model.PeerTask
import cn.edu.buaa.scs.service.PeerService
import cn.edu.buaa.scs.service.peer
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * 互评相关的路由
 */
fun Route.peerRoute() {
    route("/peerAssessment") {
        post {
            val req = call.receive<CreatePeerAssessmentRequest>()
            call.peer.createOrUpdate(req.assignmentId, req.score, req.reason).let {
                call.respond(convertAssessmentInfo(it))
            }
        }

        /**
         * 获取一份作业的所有评分结果
         */
        get {
            val assignmentId = call.request.queryParameters["assignmentId"]?.toInt()
                ?: throw BadRequestException("invalid assignmentId")
            call.respond(
                convertAssignmentPeerAssessmentResult(call.peer.getResult(assignmentId))
            )
        }
    }
    route("/peerTasks") {
        get {
            val expId = call.request.queryParameters["expId"]?.toInt()
                ?: throw BadRequestException("invalid expId")
            call.respond(
                call.peer.getTasks(expId).map { convertStudentPeerTask(it) }
            )
        }
    }
    route("/peerAppeal") {
        post {
            val req = call.receive<CreatePeerAppealRequest>()
            call.peer.createAppeal(req.expId, req.content).let {
                call.respond(convertPeerAppeal(it))
            }
        }

        route("/{peerAppealId}") {
            fun ApplicationCall.getPeerAppealIdFromPath(): Int {
                return parameters["peerAppealId"]?.toInt()
                    ?: throw BadRequestException("invalid peerAppealId")
            }
            get {
                call.respond(
                    convertPeerAppeal(
                        call.peer.getAppeal(
                            call.getPeerAppealIdFromPath()
                        )
                    )
                )
            }

            patch {
                val req = call.receive<PatchPeerAppealRequest>()
                call.peer.updateAppeal(
                    call.getPeerAppealIdFromPath(),
                    req
                ).let {
                    call.respond(convertPeerAppeal(it))
                }
            }

            delete {
                call.peer.deleteAppeal(call.getPeerAppealIdFromPath())
                call.respond("OK")
            }
        }
    }
    route("/peerAppeals") {
        get {
            val expId = call.request.queryParameters["expId"]?.toInt()
                ?: throw BadRequestException("invalid expId")
            val studentId = call.request.queryParameters["studentId"]
            call.respond(
                call.peer.getAllAppeal(expId, studentId).map { convertPeerAppeal(it) }
            )
        }
    }
}

internal fun convertAssignmentWithStandardScore(
    assignment: Assignment,
    peerStandard: PeerStandard?
): AssignmentWithStandardScoreResponse {
    return AssignmentWithStandardScoreResponse(
        id = assignment.id,
        studentId = assignment.studentId,
        expId = assignment.experimentId,
        courseId = assignment.courseId,
        createdAt = assignment.createdAt,
        updatedAt = assignment.updatedAt,
        file = assignment.file?.let { convertFileResponse(it) },
        peerInfo = if (peerStandard != null && peerStandard.isCompleted) convertAssessmentInfo(peerStandard) else null
    )
}

internal fun convertAssessmentInfo(standard: PeerStandard): AssessmentInfoResponse {
    return AssessmentInfoResponse(
        assessor = SimpleUser(standard.assessorId!!, standard.assessorName!!),
        score = standard.score!!,
        assessedTime = standard.createdAt!!,
        assignmentId = standard.assignmentId,
        reason = ""
    )
}

internal fun convertAssessmentInfo(task: PeerTask, isAdmin: Boolean = true): AssessmentInfoResponse {
    return AssessmentInfoResponse(
        assessor = if (isAdmin) SimpleUser(task.assessorId, task.assessorName) else null,
        score = task.originalScore,
        adjustedScore = task.adjustedScore,
        assessedTime = task.createdAt,
        assignmentId = task.assignmentId,
        reason = task.reason ?: ""
    )
}

internal fun convertAssessmentInfo(assessmentInfo: PeerService.AssessmentInfo): AssessmentInfoResponse {
    return AssessmentInfoResponse(
        assessor = SimpleUser(assessmentInfo.assessorId, assessmentInfo.assessorName),
        score = assessmentInfo.score,
        assessedTime = assessmentInfo.assessedTime,
        assignmentId = assessmentInfo.assignmentId,
        reason = assessmentInfo.reason
    )
}

internal fun convertStudentPeerTask(taskWithFile: PeerService.PeerTaskWithFile): StudentPeerTaskResponse {
    return StudentPeerTaskResponse(
        taskWithFile.assignmentId,
        convertFileResponse(taskWithFile.file),
        convertAssessmentInfo(taskWithFile.peerTask)
    )
}

internal fun convertAssignmentPeerAssessmentResult(result: PeerService.PeerAssessmentResult): AssignmentPeerAssessmentResultResponse {
    return AssignmentPeerAssessmentResultResponse(
        result.assignmentId,
        peerScore = result.peerScore,
        finalScore = result.finalScore,
        peerInfoList = result.peerInfoList.map { convertAssessmentInfo(it) }
    )
}

internal fun convertPeerAppeal(appeal: PeerAppeal): PeerAppealResponse {
    return PeerAppealResponse(
        id = appeal.id,
        studentId = appeal.studentId,
        expId = appeal.expId,
        content = appeal.content,
        appealedAt = appeal.appealedAt,
        processor = appeal.processorId?.let { SimpleUser(it, appeal.processorName!!) },
        processStatus = appeal.processStatus,
        processContent = appeal.processContent,
        processedAt = appeal.processedAt,
    )
}