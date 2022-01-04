package cn.edu.buaa.scs.model

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.float
import org.ktorm.schema.int
import org.ktorm.schema.varchar

interface Assignment : Entity<Assignment> {
    companion object : Entity.Factory<Assignment>()

    var id: Int
    var studentId: String
    var fileId: Int
    var expId: Int
    var score: Float
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
    val score = float("score").bindTo { it.score }
}