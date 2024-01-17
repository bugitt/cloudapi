package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.model.Authentication
import cn.edu.buaa.scs.service.auth
import cn.edu.buaa.scs.service.project
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

    route("/tokenInfo") {
        post {
            val req = call.receiveParameters()
            val token = req["token"] ?: throw BadRequestException("token is required")
            val service = req["service"] ?: throw BadRequestException("service is required")
            call.respond(
                call.auth.getTokenInfo(token, service)
            )
        }
    }

    route("/checkPermission") {
        get {
            val entityType = call.parameters["entityType"] ?: throw BadRequestException("entityType is required")
            val entityId = call.parameters["entityId"] ?: throw BadRequestException("entityId is required")
            val action = call.parameters["action"] ?: throw BadRequestException("action is required")
            call.respond(call.auth.checkPermission(entityType, entityId, action))
        }
    }

    route("/paasToken") {
        put {
            val password = call.receive<PutPaasTokenRequest>().paasToken
            call.project.changePassword(password)
            call.respond("OK")
        }
    }

    route("/auth/sendActiveEmail") {
        post {
            val req = call.receive<SendActiveEmailRequest>()
            call.auth.sendActiveEmail(req.id, req.name, req.email)
            call.respond("OK")
        }
    }

    route("/auth/sendResetPasswordEmail") {
        post {
            val req = call.receive<SendResetPasswordEmailRequest>()
            call.auth.sendResetPasswordEmail(req.id, req.email)
            call.respond("OK")
        }
    }

    route("/activeUser") {
        post {
            val req = call.receive<ActiveUserRequest>()
            call.respond(call.auth.activeUser(req.token, req.password))
        }
    }

    route("/resetPassword") {
        post {
            val req = call.receive<ResetPasswordrRequest>()
            call.auth.resetPassword(req.token, req.password)
            call.respond("OK")
        }
    }
}
