package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.TermModel
import cn.edu.buaa.scs.model.Term
import cn.edu.buaa.scs.service.term
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.termRoute() {
    route("/terms") {
        get {
            call.respond(call.term.getAllTerms().map { convertTermModel(it) })
        }

        get("/latest") {
            call.respond(convertTermModel(call.term.getLatestTerm()))
        }
    }
}

fun convertTermModel(term: Term): TermModel {
    return TermModel(term.id, term.name)
}
