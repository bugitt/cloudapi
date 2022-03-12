package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.CreatePeerAssessmentRequest
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
            call.peer.createOrUpdate(req.assignmentId, req.score, req.reason)
            call.respond("OK")
        }
    }
}