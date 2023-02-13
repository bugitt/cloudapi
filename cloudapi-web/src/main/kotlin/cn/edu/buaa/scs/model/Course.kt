@file:Suppress("unused")

package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.utils.IntOrString
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

interface Course : Entity<Course>, IEntity {
    companion object : Entity.Factory<Course>()

    var id: Int
    var teacher: User
    var name: String
    var term: Term
    var departmentId: String
    var createTime: String
    var resourceFolder: String

    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}

object Courses : Table<Course>("course") {
    val id = int("id").primaryKey().bindTo { it.id }
    val teacherId = varchar("teacher_id").references(Users) { it.teacher }
    val name = varchar("name").bindTo { it.name }
    val termId = int("term_id").references(Terms) { it.term }
    val createTime = varchar("create_time").bindTo { it.createTime }
    val departmentId = varchar("department_id").bindTo { it.departmentId }
    val resourceFolder = varchar("resource_folder").bindTo { it.resourceFolder }
}

val Database.courses
    get() = this.sequenceOf(Courses)