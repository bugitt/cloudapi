package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.CreateUserRequest
import cn.edu.buaa.scs.controller.models.TermModel
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.Term
import cn.edu.buaa.scs.model.UserRole
import cn.edu.buaa.scs.service.admin
import cn.edu.buaa.scs.service.term
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.termRoute() {
    route("/terms") {
        get {
            call.respond(call.term.getAllTerms())
        }

        get("/latest") {
            call.respond(convertTermModel(call.term.getLatestTerm()))
        }

        post {
            val req = call.receive<TermModel>()
            call.term.createTerm(req.name ?: "")
            call.respond("OK")
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: throw BadRequestException("学期 ID 不得为空")
            call.term.deleteTerm(id.toInt())
            call.respond("OK")
        }
    }
}

fun convertTermModel(term: Term): TermModel {
    return TermModel(term.id, term.name)
}
