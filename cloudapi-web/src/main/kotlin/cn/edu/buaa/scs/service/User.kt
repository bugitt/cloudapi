package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.controller.models.AssistantModel
import cn.edu.buaa.scs.controller.models.DepartmentModel
import cn.edu.buaa.scs.controller.models.PatchUserRequest
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.userId
import io.ktor.server.application.*
import org.ktorm.dsl.*
import org.ktorm.entity.*

fun User.Companion.id(id: String): User =
    mysql.users.find { it.id eq id } ?: throw BusinessException("find user($id) from mysql error")


fun User.Companion.getUerListByIdList(idList: List<String>): List<User> {
    return if (idList.isEmpty()) listOf()
    else mysql.users.filter { it.id.inList(idList) }.toList()
}

fun User.Companion.createNewUnActiveUser(id: String, name: String?, role: UserRole, departmentId: Int): User {
    val user = User {
        this.id = id
        this.name = name ?: "未激活用户"
        this.role = role
        this.departmentId = departmentId
    }
    mysql.users.add(user)
    return user
}

val ApplicationCall.userService
    get() = UserService.getSvc(this) { UserService(this) }

class UserService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<UserService>()

    fun getStudents(search: String?, limit: Int = 10): List<User> {
        if (call.user().isStudent()) return listOf()

        if (search.isNullOrBlank()) return listOf()

        var query = mysql.users
            .filter {
                (it.id.like("%$search%").or(it.name.like("%$search%")))
                    .and(it.role eq UserRole.STUDENT)
            }
            .sortedBy { it.id }
        if (limit != -1) {
            query = query.take(limit)
        }
        return query.toList()
    }

    fun getTeachersAndStudents(search: String?, limit: Int = 10): List<User> {
        if (call.user().isStudent()) return listOf()

        val ret: List<User>
        if (search.isNullOrBlank()) {
            var query = mysql.users
                .filter {
                    (it.role eq UserRole.STUDENT).or(it.role eq UserRole.TEACHER)
                }
                .sortedBy { it.id }
            if (limit != -1) {
                query = query.take(limit)
            }
            ret = query.toList()
        } else {
            var query = mysql.users
                .filter {
                    (it.id.like("%$search%").or(it.name.like("%$search%")))
                        .and((it.role eq UserRole.STUDENT).or(it.role eq UserRole.TEACHER))
                }
                .sortedBy { it.id }
            if (limit != -1) {
                query = query.take(limit)
            }
            ret = query.toList()
        }
        return ret
    }

    fun patchUser(userId: String, req: PatchUserRequest) {
        if (call.userId().lowercase() != userId.lowercase() && !call.user().isAdmin()) {
            throw AuthorizationException("无修改权限")
        }

        val user = User.id(userId)
        if (req.name != null) {
            user.name = req.name
        }
        if (req.email != null) {
            user.email = req.email
        }
        if (req.nickname != null) {
            user.nickName = req.nickname
        }

        user.flushChanges()
    }

    fun changePassword(userId: String, old: String, new: String) {
        if (call.userId().lowercase() != userId.lowercase() && !call.user().isAdmin()) {
            throw BadRequestException("无修改权限")
        }

        val user = User.id(userId)
        // 如果是管理员修改密码，无需检查旧密码
        if (!call.user().isAdmin() && user.password != old) {
            throw BadRequestException("旧密码错误")
        }
        user.password = new
        user.flushChanges()
    }

    fun myAssistants(): List<AssistantModel> {
        if (!call.user().isTeacher()) {
            throw BadRequestException("only teachers can get their assistants")
        }

        val courseIdList = mysql.courses.filter { it.teacherId.eq(call.userId()) }.map { it.id.toString() }
        if (courseIdList.isEmpty()) return listOf()

        return mysql.assistants.filter { it.courseId.inList(courseIdList) }.toList().groupBy { it.courseId }
            .mapValues { (courseId, assistantList) ->
                val course = Course.id(courseId.toInt())
                assistantList.mapNotNull { assistant ->
                    try {
                        val user = User.id(assistant.studentId)
                        AssistantModel(
                            id = user.id,
                            name = course.name,
                            courseName = course.name,
                            termName = course.term.name,
                            createdTime = assistant.createTime,
                            courseId = course.id,
                            rawId = assistant.id,
                        )
                    } catch (e: BusinessException) {
                        null
                    }
                }
            }
            .values
            .flatten()
            .sortedByDescending { it.rawId }
    }

    fun getAllDepartments(): List<DepartmentModel> {
        return mysql.departments.map { department ->
            DepartmentModel(
                id = department.id,
                name = department.name,
            )
        }
    }
}
