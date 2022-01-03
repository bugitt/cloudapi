package cn.edu.buaa.scs.auth

import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.model.assistants
import cn.edu.buaa.scs.model.courses
import cn.edu.buaa.scs.model.experiments
import cn.edu.buaa.scs.storage.mysql
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.find

fun isAdmin(userId: String): Boolean =
    userId == "admin"

@Suppress("unused")
fun assertAdmin(userId: String) {
    if (!isAdmin(userId)) {
        throw AuthorizationException("user is not admin")
    }
}

/**
 * 检查用户是否有对该课程的所有资源的管理权限
 */
fun authCourse(userId: String, courseId: Int): Boolean {
    if (isAdmin(userId)) {
        return true
    }
    val checkTeacher = fun() = mysql.courses.find { it.id eq courseId }?.let { it.teacherId == userId } ?: false

    val checkAssistant = fun() = mysql.assistants.find {
        (it.courseId eq courseId.toString()) and (it.studentId eq userId)
    }?.let { true } ?: false

    return checkTeacher() || checkAssistant()
}

@Suppress("unused")
fun assertCourse(userId: String, courseId: Int) {
    if (!authCourse(userId, courseId)) {
        throw AuthorizationException("用户对该课程没有管理权限")
    }
}

/**
 * 检查用户是否有对该实验(作业)的所有资源的管理权限
 */
@Suppress("unused")
fun authExperiment(userId: String, experimentId: Int): Boolean {
    if (isAdmin(userId)) {
        return true
    }
    val courseId = mysql.experiments.find { it.id eq experimentId }?.courseId ?: return false
    return authCourse(userId, courseId)
}

@Suppress("unused")
fun assertExperiment(userId: String, experimentId: Int) {
    if (!authCourse(userId, experimentId)) {
        throw AuthorizationException("用户对该实验(作业)没有管理权限")
    }
}