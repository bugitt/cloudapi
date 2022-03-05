package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.utils.IntOrString
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int

interface CourseResource : Entity<CourseResource>, IEntity {
    companion object : Entity.Factory<CourseResource>()

    var id: Int
    var courseId: Int
    var expId: Int?
    var file: File

    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}

object CourseResources : Table<CourseResource>("course_resource") {
    val id = int("id").primaryKey().bindTo { it.id }
    val courseId = int("course_id").bindTo { it.courseId }
    val expId = int("exp_id").bindTo { it.expId }
    val fileId = int("file_id").references(Files) { it.file }
}

val Database.courseResources get() = this.sequenceOf(CourseResources)