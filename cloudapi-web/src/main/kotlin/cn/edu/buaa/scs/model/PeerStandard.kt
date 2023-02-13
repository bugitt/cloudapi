package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

interface PeerStandard : Entity<PeerStandard> {
    companion object : Entity.Factory<PeerStandard>()

    var id: Int
    var assignmentId: Int
    var expId: Int
    var assessorId: String?
    var assessorName: String?
    var score: Double?
    var createdAt: Long?
    var isCompleted: Boolean
}

object PeerStandards : Table<PeerStandard>("peer_standard") {
    val id = int("id").primaryKey().bindTo { it.id }
    val assignmentId = int("assignment_id").bindTo { it.assignmentId }
    val expId = int("exp_id").bindTo { it.expId }
    val assessorId = varchar("assessor_id").bindTo { it.assessorId }
    val assessorName = varchar("assessor_name").bindTo { it.assessorName }
    val score = double("score").bindTo { it.score }
    val isCompleted = boolean("is_completed").bindTo { it.isCompleted }
    val createdAt = long("created_at").bindTo { it.createdAt }
}

val Database.peerStands get() = this.sequenceOf(PeerStandards)