package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

interface ChatHistory : Entity<ChatHistory>, IEntity {
    var id: Int
    var userId: String
    var chatId: String
    var updateTime: Long
    var title: String
    var top: Boolean

    companion object : Entity.Factory<ChatHistory>()

    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}

object ChatHistories : Table<ChatHistory>("chat_history") {
    val id = int("id").primaryKey().bindTo { it.id }
    val userId = varchar("userId").bindTo { it.userId }
    val chatId = varchar("chatId").bindTo { it.chatId }
    val updateTime = long("updateTime").bindTo { it.updateTime }
    val title = varchar("title").bindTo { it.title }
    val top = boolean("top").bindTo { it.top }
}

@Suppress("unused")
val Database.chatHistory
    get() = this.sequenceOf(ChatHistories)
