package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

interface AssignmentReview : Entity<AssignmentReview> {
    companion object : Entity.Factory<AssignmentReview>()

    var id: Int
    var assignmentId: Int
    var fileId: Int
    var reviewedAt: Long
    var reviewerId: String
    var reviewerName: String
}

object AssignmentReviews : Table<AssignmentReview>("assignment_review") {
    val id = int("id").primaryKey().bindTo { it.id }
    val assignmentId = int("assignment_id").bindTo { it.assignmentId }
    val fileId = int("file_id").bindTo { it.fileId }
    val reviewAt = long("reviewed_at").bindTo { it.reviewedAt }
    val reviewerId = varchar("reviewer_id").bindTo { it.reviewerId }
    val reviewerName = varchar("reviewer_name").bindTo { it.reviewerName }
}

val Database.assignmentReviews
    get() = this.sequenceOf(AssignmentReviews)