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
    var student: User

    override fun entityId(): IntOrString {
        return IntOrString(0)
    }
}

object CourseStudents : Table<CourseStudent>("course_student_mapping") {
    @Suppress("unused")
    val courseId = int("course_id").bindTo { it.courseId }

    @Suppress("unused")
    val studentId = varchar("student_id").references(Users) { it.student }
}

val Database.courseStudents get() = this.sequenceOf(CourseStudents)
