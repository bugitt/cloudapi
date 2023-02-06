package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.GetCaptcha200Response
import cn.edu.buaa.scs.controller.models.PostLoginRequest
import cn.edu.buaa.scs.model.Authentication
import cn.edu.buaa.scs.service.auth
import cn.edu.buaa.scs.utils.token
import cn.edu.buaa.scs.utils.user
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoute() {
    route("/authentications") {
        get {
            call.respond(listOf(Authentication(call.user().id.lowercase(), call.token())))
        }
    }

    route("/captcha") {
        get {
            val (token, image) = call.auth.createCaptcha()
            call.respond(
                GetCaptcha200Response(token, image)
            )
        }
    }

    route("/login") {
        post {
            val req = call.receive<PostLoginRequest>()
            call.respond(
                call.auth.login(req.userId, req.password, req.captchaToken, req.captchaText)
            )
        }
    }

    route("buaaSSOLogin") {
        post {
            val ssoToken = call.request.queryParameters["ssoToken"] ?: throw BadRequestException("ssoToken is required")
            call.respond(
                call.auth.buaaSSOLogin(ssoToken)
            )
        }
    }

    route("/whoami") {
        get {
            call.respond(call.auth.whoami(call.parameters["listProjects"].toBoolean()))
        }
    }

    route("/checkPermission") {
        get {
            val entityType = call.parameters["entityType"] ?: throw BadRequestException("entityType is required")
            val entityId = call.parameters["entityId"]?.toLong() ?: throw BadRequestException("entityId is required")
            val action = call.parameters["action"] ?: throw BadRequestException("action is required")
            call.respond(call.auth.checkPermission(entityType, entityId, action))
        }
    }
}
