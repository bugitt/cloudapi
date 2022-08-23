package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.PostPaasUserRequest
import cn.edu.buaa.scs.service.project
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.projectRoute() {
    post("/paasUser") {
        val req = call.receive<PostPaasUserRequest>()
        call.project.createUser(req.userId)
        call.respond("OK")
    }
}