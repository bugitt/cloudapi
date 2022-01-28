package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

interface Assignment : Entity<Assignment>, IEntity {

    var id: Int
    var studentId: String
    var fileId: Int
    var expId: Int
    var score: Float
    var courseId: Int

    var createdAt: Long
    var updatedAt: Long

    companion object : Entity.Factory<Assignment>()
}

object Assignments : Table<Assignment>("assignment_v2") {
    @Suppress("unused")
    val id = int("id").primaryKey().bindTo { it.id }

    @Suppress("unused")
    val studentId = varchar("student_id").bindTo { it.studentId }

    @Suppress("unused")
    val fileId = int("file_id").bindTo { it.fileId }

    @Suppress("unused")
    val expId = int("exp_id").bindTo { it.expId }

    @Suppress("unused")
    val courseId = int("course_id").bindTo { it.courseId }

    @Suppress("unused")
    val score = float("score").bindTo { it.score }

    @Suppress("unused")
    val createdAt = long("created_at").bindTo { it.createdAt }

    @Suppress("unused")
    val updatedAt = long("updated_at").bindTo { it.updatedAt }
}

@Suppress("unused")
val Database.assignments
    get() = this.sequenceOf(Assignments)