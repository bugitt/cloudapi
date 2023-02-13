@file:Suppress("unused")

package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

interface Assistant : Entity<Assistant>, IEntity {
    companion object : Entity.Factory<Assistant>()

    var id: Int
    var studentId: String
    var courseId: String
    var createTime: String

    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}

object Assistants : Table<Assistant>("assistant") {
    val id = int("id").primaryKey().bindTo { it.id }
    val studentId = varchar("student_id").bindTo { it.studentId }
    val courseId = varchar("course_id").bindTo { it.courseId }
    val createTime = varchar("create_time").bindTo { it.createTime }
}

val Database.assistants
    get() = this.sequenceOf(Assistants)
