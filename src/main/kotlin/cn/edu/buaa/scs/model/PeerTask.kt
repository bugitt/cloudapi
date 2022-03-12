package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.service.id
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

interface PeerTask : Entity<PeerTask>, IEntity {
    companion object : Entity.Factory<PeerTask>()

    var id: Int
    var studentId: String
    var assignmentId: Int
    var expId: Int
    var assessorId: String
    var assessorName: String
    var originalScore: Double?
    var adjustedScore: Double?   // 与标准分对比调整后的分数
    var reason: String?
    var createdAt: Long?
    var isStandard: Boolean

    /**
    0: 还没有打分
    1: 打分了，但还没调整分数
    2: 分数调整完毕，打分任务完成
     */
    var status: Int

    fun experiment(): Experiment = Experiment.id(expId)
    fun assignment(): Assignment = Assignment.id(assignmentId)
}

object PeerTasks : Table<PeerTask>("peer_task") {
    val id = int("id").primaryKey().bindTo { it.id }
    val studentId = varchar("student_id").bindTo { it.studentId }
    val assignmentId = int("assignment_id").bindTo { it.assignmentId }
    val expId = int("exp_id").bindTo { it.expId }
    val assessorId = varchar("assessor_id").bindTo { it.assessorId }
    val assessorName = varchar("assessor_name").bindTo { it.assessorName }
    val originalScore = double("original_score").bindTo { it.originalScore }
    val adjustedScore = double("adjusted_score").bindTo { it.adjustedScore }
    val reason = text("reason").bindTo { it.reason }
    val createdAt = long("created_at").bindTo { it.createdAt }
    val isStandard = boolean("is_standard").bindTo { it.isStandard }
    val status = int("status").bindTo { it.status }
}

val Database.peerTasks
    get() = this.sequenceOf(PeerTasks)