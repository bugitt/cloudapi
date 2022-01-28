package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

interface CourseStudent : Entity<CourseStudent>, IEntity {
    companion object : Entity.Factory<CourseStudent>()

    var courseId: Int
    var studentId: String
}

object CourseStudents : Table<CourseStudent>("course_student_mapping") {
    @Suppress("unused")
    val courseId = int("course_id").bindTo { it.courseId }

    @Suppress("unused")
    val studentId = varchar("student_id").bindTo { it.studentId }
}

val Database.courseStudents get() = this.sequenceOf(CourseStudents)