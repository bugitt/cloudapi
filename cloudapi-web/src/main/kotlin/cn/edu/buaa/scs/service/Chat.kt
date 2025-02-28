package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.controller.models.ChatHistoryItem
import cn.edu.buaa.scs.controller.models.GetChatRecordsRequest
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.HttpClientWrapper
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import org.ktorm.dsl.*
import org.ktorm.entity.*

val ApplicationCall.chat
    get() = ChatService.getSvc(this) { ChatService(this) }

class ChatService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<ChatService>()

    internal val client by lazy {
        HttpClientWrapper(
            HttpClient(CIO) {
                install(ContentNegotiation) { // 确保 JSON 序列化功能可用
                    jackson()
                }
                defaultRequest {
                    header(HttpHeaders.Authorization, "Bearer fastgpt-sbHq9MGmKbijSJZe3l43BcFWUxNlkcISo1SSBqAHKsqXv8bGQoSIGh28Vw4Dryeu")
                }
            },
            basePath = "http://10.251.254.178:3000/api"
        )
    }

    // 获取指定用户的聊天记录
    fun getChatHistoryByUserId(userId: String): List<ChatHistoryItem> {
        return mysql.chatHistory.filter { it.userId eq userId }
            .map { convertToChatHistoryItem(it) }.toList()
    }

    // 创建新的聊天记录
    fun createChatHistory(userId: String, chatHistoryItem: ChatHistoryItem): Int {
        return mysql.insertAndGenerateKey(ChatHistories) {
            set(ChatHistories.userId, userId)
            set(ChatHistories.chatId, chatHistoryItem.chatId)
            set(ChatHistories.updateTime, chatHistoryItem.updateTime)
            set(ChatHistories.title, chatHistoryItem.title)
            set(ChatHistories.top, chatHistoryItem.top)
        } as Int
    }

    // 获取聊天记录
    suspend fun getChatRecords(request: GetChatRecordsRequest): String {
        val body = GetChatRecordsRequest(
            chatId = request.chatId,
            appId = "679e3b3b5fbd929e37095abc",
            offset = 0,
            pageSize = 30,
            loadCustomFeedbacks = false,
        )
        val response = client.post<String>("/core/chat/getPaginationRecords", body)
        return response.getOrThrow()
    }

    private fun convertToChatHistoryItem(chatHistory: ChatHistory): ChatHistoryItem {
        return ChatHistoryItem(
            id = chatHistory.id,
            chatId = chatHistory.chatId,
            updateTime = chatHistory.updateTime,
            title = chatHistory.title,
            top = chatHistory.top
        )
    }
}
