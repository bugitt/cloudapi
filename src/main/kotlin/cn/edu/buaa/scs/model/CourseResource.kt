package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int

interface CourseResource : Entity<CourseResource>, IEntity {
    companion object : Entity.Factory<CourseResource>()

    var id: Int
    var course: Course
    var file: File
}

object CourseResources : Table<CourseResource>("course_resource") {
    val id = int("id").primaryKey().bindTo { it.id }
    val courseId = int("course_id").references(Courses) { it.course }
    val fileId = int("file_id").references(Files) { it.file }
    val course get() = courseId.referenceTable as Courses
    val file get() = fileId.referenceTable as Files
}

val Database.courseResources get() = this.sequenceOf(CourseResources)