package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

interface Course : Entity<Course> {
    companion object : Entity.Factory<Course>()

    var id: Int
    var teacherId: String
    var name: String
    var termId: Int
    var departmentId: String
    var createTime: String
    var resourceFolder: String
}

object Courses : Table<Course>("course") {
    @Suppress("unused")
    val id = int("id").primaryKey().bindTo { it.id }

    @Suppress("unused")
    val teacherId = varchar("teacher_id").bindTo { it.teacherId }

    @Suppress("unused")
    val name = varchar("name").bindTo { it.name }

    @Suppress("unused")
    val termId = int("term_id").bindTo { it.termId }

    @Suppress("unused")
    val createTime = varchar("create_time").bindTo { it.createTime }

    @Suppress("unused")
    val departmentId = varchar("department_id").bindTo { it.departmentId }

    @Suppress("unused")
    val resourceFolder = varchar("resource_folder").bindTo { it.resourceFolder }
}

@Suppress("unused")
val Database.courses
    get() = this.sequenceOf(Courses)