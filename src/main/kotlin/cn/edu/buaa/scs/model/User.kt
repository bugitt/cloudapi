package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.service.id
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.IntOrString
import cn.edu.buaa.scs.utils.exists
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.Entity
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.varchar

enum class UserRole {
    STUDENT,
    TEACHER,
    SYS;

    fun level(): Int {
        return when (this) {
            STUDENT -> 1
            TEACHER -> 2
            SYS -> 4
        }
    }

    companion object {
        fun fromLevel(level: Int): UserRole {
            return when (level) {
                1 -> STUDENT
                2 -> TEACHER
                4 -> SYS
                else -> throw IllegalArgumentException("Invalid user level: $level")
            }
        }
    }
}

interface User : Entity<User>, IEntity {
    companion object : Entity.Factory<User>()

    var id: String
    var name: String
    var nickName: String
    var password: String
    var email: String
    var role: UserRole
    var departmentId: Int
    var isAccepted: Boolean
    var acceptTime: String

    fun isTeacher(): Boolean = this.role.level() >= 2

    fun isAdmin(): Boolean = this.id == "admin"

    fun isCourseStudent(course: Course): Boolean =
        mysql.courseStudents.exists { (it.courseId eq course.id) and (it.studentId eq this.id) }

    fun isCourseStudent(courseId: Int): Boolean =
        mysql.courseStudents.exists { (it.courseId eq courseId) and (it.studentId eq this.id) }

    fun isCourseAssistant(course: Course): Boolean =
        mysql.assistants.find {
            (it.courseId eq course.id.toString()) and (it.studentId eq this.id)
        }?.let { true } ?: false

    fun isCourseAssistant(courseId: Int): Boolean =
        mysql.assistants.find {
            (it.courseId eq courseId.toString()) and (it.studentId eq this.id)
        }?.let { true } ?: false

    fun isCourseTeacher(course: Course): Boolean = course.teacher.id == this.id

    fun isCourseTeacher(courseId: Int): Boolean = Course.id(courseId).teacher.id == this.id

    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}

object Users : Table<User>("user") {
    @Suppress("unused")
    val id = varchar("id").primaryKey().bindTo { it.id }

    @Suppress("unused")
    val name = varchar("name").bindTo { it.name }

    @Suppress("unused")
    val nickName = varchar("nick_name").bindTo { it.nickName }

    @Suppress("unused")
    val password = varchar("passwd").bindTo { it.password }

    @Suppress("unused")
    val email = varchar("email").bindTo { it.email }

    @Suppress("unused")
    val role = int("role").transform({ UserRole.fromLevel(it) }, { it.level() }).bindTo { it.role }

    @Suppress("unused")
    val departmentID = varchar("department_id").transform({ it.toInt() }, { it.toString() }).bindTo { it.departmentId }

    @Suppress("unused")
    val isAccepted = boolean("is_accept").bindTo { it.isAccepted }

    @Suppress("unused")
    val acceptTime = varchar("accept_time").bindTo { it.acceptTime }

    fun getByID(id: String): User? =
        mysql.users.find { Users.id eq id }
}

val Database.users get() = this.sequenceOf(Users)