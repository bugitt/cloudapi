package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.PeerStandard
import cn.edu.buaa.scs.model.PeerTask
import cn.edu.buaa.scs.service.PeerService
import cn.edu.buaa.scs.service.peer
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

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

internal fun convertAssessmentInfo(assessmentInfo: PeerTask): AssessmentInfoResponse? {
    val score = assessmentInfo.originalScore ?: return null
    val createdAt = assessmentInfo.createdAt ?: return null
    return AssessmentInfoResponse(
        assessor = SimpleUser(assessmentInfo.assessorId, assessmentInfo.assessorName),
        score = score,
        assessedTime = createdAt,
        assignmentId = assessmentInfo.assignmentId,
        reason = assessmentInfo.reason ?: ""
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