package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

interface Assignment : Entity<Assignment>, IEntity {

    var id: Int
    var studentId: String

    // TODO: 检查其他的可空字段
    var file: File?
    var assignmentReview: AssignmentReview?

    var experimentId: Int
    var finalScore: Float
    var peerScore: Double
    var peerCompleted: Boolean
    var courseId: Int

    var createdAt: Long
    var updatedAt: Long

    companion object : Entity.Factory<Assignment>()

    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}

object Assignments : Table<Assignment>("assignment_v2") {
    @Suppress("unused")
    val id = int("id").primaryKey().bindTo { it.id }

    @Suppress("unused")
    val studentId = varchar("student_id").bindTo { it.studentId }

    @Suppress("unused")
    val fileId = int("file_id").references(Files) { it.file }

    @Suppress("unused")
    val assignmentReview = int("assignment_review").references(AssignmentReviews) { it.assignmentReview }

    @Suppress("unused")
    val expId = int("exp_id").bindTo { it.experimentId }

    @Suppress("unused")
    val courseId = int("course_id").bindTo { it.courseId }

    @Suppress("unused")
    // 这里名称不统一是故意为之
    val finalScore = float("score").bindTo { it.finalScore }

    @Suppress("unused")
    val peerScore = double("peer_score").bindTo { it.peerScore }

    @Suppress("unused")
    val peerCompleted = boolean("peer_completed").bindTo { it.peerCompleted }

    @Suppress("unused")
    val createdAt = long("created_at").bindTo { it.createdAt }

    @Suppress("unused")
    val updatedAt = long("updated_at").bindTo { it.updatedAt }
}

@Suppress("unused")
val Database.assignments
    get() = this.sequenceOf(Assignments)
