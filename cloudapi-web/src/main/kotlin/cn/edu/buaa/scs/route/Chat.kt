package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.ChatHistoryItem
import cn.edu.buaa.scs.controller.models.GetChatRecordsRequest
import cn.edu.buaa.scs.service.chat
import cn.edu.buaa.scs.utils.userId
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.chatRoute() {
    route("/chat") {
        route("/history") {
            get {
                call.respond(call.chat.getChatHistoryByUserId(call.userId()))
            }

            post {
                val chatHistoryItem = call.receive<ChatHistoryItem>()
                call.chat.createChatHistory(call.userId(), chatHistoryItem)
                call.respond("OK")
            }
        }

        post("/records") {
            val requestBody = call.receive<GetChatRecordsRequest>()
            val records = call.chat.getChatRecords(requestBody)
            call.respond(records)
        }
    }
}
