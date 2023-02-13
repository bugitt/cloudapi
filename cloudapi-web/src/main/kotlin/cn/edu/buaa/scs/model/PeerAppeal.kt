package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

interface PeerAppeal : Entity<PeerAppeal>, IEntity {
    companion object : Entity.Factory<PeerAppeal>()

    var id: Int
    var expId: Int
    var studentId: String
    var content: String
    var appealedAt: Long
    var processorId: String?
    var processorName: String?
    var processContent: String?
    var processStatus: Int
    var processedAt: Long?
}

object PeerAppeals : Table<PeerAppeal>("peer_appeal") {
    val id = int("id").primaryKey().bindTo { it.id }
    val expId = int("exp_id").bindTo { it.expId }
    val studentId = varchar("student_id").bindTo { it.studentId }
    val content = text("content").bindTo { it.content }
    val appealedAt = long("appealed_at").bindTo { it.appealedAt }
    val processorId = varchar("processor_id").bindTo { it.processorId }
    val processorName = varchar("processor_name").bindTo { it.processorName }
    val processContent = text("process_content").bindTo { it.processContent }
    val processStatus = int("process_status").bindTo { it.processStatus }
    val processAt = long("processed_at").bindTo { it.processedAt }
}

val Database.peerAppeals
    get() = this.sequenceOf(PeerAppeals)