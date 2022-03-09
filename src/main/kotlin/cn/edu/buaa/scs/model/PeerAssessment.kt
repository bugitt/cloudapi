package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

interface PeerAssessment : Entity<PeerAssessment>, IEntity {
    companion object : Entity.Factory<PeerAssessment>()

    var id: Int
    var studentId: String
    var assignmentId: Int
    var experimentId: Int
    var assessorId: String
    var score: Double
    var reason: String
    var createdAt: Long
    var isStandard: Boolean
}

object PeerAssessments : Table<PeerAssessment>("peer_assessment_v2") {
    val id = int("id").primaryKey().bindTo { it.id }
    val studentId = varchar("student_id").bindTo { it.studentId }
    val assignmentId = int("assignment_id").bindTo { it.assignmentId }
    val experimentId = int("experiment_id").bindTo { it.experimentId }
    val assessorId = varchar("assessor_id").bindTo { it.assessorId }
    val score = double("score").bindTo { it.score }
    val reason = text("reason").bindTo { it.reason }
    val createdAt = long("created_at").bindTo { it.createdAt }
    val isStandard = boolean("is_standard").bindTo { it.isStandard }
}

val Database.peerAssessments
    get() = this.sequenceOf(PeerAssessments)