package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

interface Assistant : Entity<Assistant> {
    companion object : Entity.Factory<Assistant>()

    var id: Int
    var studentId: String
    var courseId: String
    var createTime: String
}

object Assistants : Table<Assistant>("assistant") {
    @Suppress("unused")
    val id = int("id").primaryKey().bindTo { it.id }

    @Suppress("unused")
    val studentId = varchar("student_id").bindTo { it.studentId }

    @Suppress("unused")
    val courseId = varchar("course_id").bindTo { it.courseId }

    @Suppress("unused")
    val createTime = varchar("create_time").bindTo { it.createTime }
}

@Suppress("unused")
val Database.assistants
    get() = this.sequenceOf(Assistants)

