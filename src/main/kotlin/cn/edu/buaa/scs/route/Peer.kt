package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.PeerStandard
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
        peerInfo = if (peerStandard != null && peerStandard.isCompleted) convertStandardAssessmentInfo(peerStandard) else null
    )
}

internal fun convertStandardAssessmentInfo(peerStandard: PeerStandard): StandardAssessmentInfo {
    return StandardAssessmentInfo(
        assessor = SimpleUser(peerStandard.assessorId!!, peerStandard.assessorName!!),
        score = peerStandard.score!!,
        createdAt = peerStandard.createdAt!!
    )
}

internal fun convertAssessmentInfo(assessmentInfo: PeerService.AssessmentInfo): AssessmentInfoResponse {
    return AssessmentInfoResponse(
        assessor = SimpleUser(assessmentInfo.assessorId, assessmentInfo.assessorName),
        score = assessmentInfo.score,
        assessedTime = assessmentInfo.assessedTime,
        assignmentId = assessmentInfo.assignmentId
    )
}