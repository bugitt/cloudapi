package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.Course
import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.model.courseStudents
import cn.edu.buaa.scs.model.courses
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.user
import io.ktor.application.*
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList

val ApplicationCall.course get() = CourseService(this)

class CourseService(val call: ApplicationCall) {
    fun get(id: Int): Course {
        val course = Course.id(id)
        call.user().assertRead(course)
        return course
    }

    fun getAllStudents(courseId: Int): List<User> {
        return mysql.courseStudents.filter { it.courseId eq courseId }.toList().map { it.student }
    }
}

fun Course.Companion.id(id: Int): Course {
    return mysql.courses.find { it.id eq id }
        ?: throw BusinessException("find course($id) from database error")
}