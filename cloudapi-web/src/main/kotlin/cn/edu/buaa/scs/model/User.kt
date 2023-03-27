@file:Suppress("unused")

package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.service.id
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.exists
import cn.edu.buaa.scs.utils.tryToInt
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.inList
import org.ktorm.dsl.or
import org.ktorm.entity.*
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
                0, 1 -> STUDENT
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
    var paasToken: String

    fun isTeacher(): Boolean = this.role.level() == 2

    fun isStudent(): Boolean = this.role.level() == 1

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

    fun isAssistant(): Boolean =
        mysql.assistants.exists { it.studentId eq this.id }

    fun isAssistantForStudent(studentId: String): Boolean {
        val student = User.id(studentId)
        if (!student.isStudent()) return false
        val courseIdList =
            mysql.courseStudents.filter { it.studentId eq studentId }.map { it.courseId }.toList().map { it.toString() }
        if (courseIdList.isEmpty()) return false
        return mysql.assistants.exists { (it.studentId eq this.id).and(it.courseId.inList(courseIdList)) }
    }

    fun getAdminCourses(): List<Pair<Int, String>> {
        return when (this.role) {
            UserRole.STUDENT -> {
                mysql.assistants
                    .filter { it.studentId.eq(this.id) }.map { it.courseId }
                    .toList()
                    .map { it.toInt() }
                    .let { courseIdList ->
                        if (courseIdList.isEmpty()) emptyList()
                        else mysql.courses.filter { it.id.inList(courseIdList) }.map { it.id to it.name }.toList()
                    }
            }

            UserRole.SYS -> emptyList()
            UserRole.TEACHER -> {
                mysql.courses.filter { it.teacherId.eq(this.id) }.map { it.id to it.name }.toList()
            }
        }
    }

    fun getAssistantCourseIdList(): List<Int> =
        mysql.assistants.filter { it.studentId eq this.id }.map { it.courseId.tryToInt() }.filterNotNull().distinct()

    fun isCourseTeacher(course: Course): Boolean = course.teacher.id == this.id

    fun isCourseTeacher(courseId: Int): Boolean = Course.id(courseId).teacher.id == this.id

    fun isCourseAdmin(course: Course): Boolean = isCourseAssistant(course) || isCourseTeacher(course) || isAdmin()

    fun isProjectAdmin(projectID: Long): Boolean = isProjectAdmin(Project.id(projectID))

    fun isProjectAdmin(project: Project): Boolean =
        project.expID != null && isCourseAdmin(Experiment.id(project.expID!!).course) ||
                mysql.projectMembers.exists {
                    it.projectId.eq(project.id) and it.userId.eq(this.id)
                        .and(it.role.eq(ProjectRole.ADMIN) or it.role.eq(ProjectRole.OWNER))
                } ||
                isAdmin()

    fun personalProjectName() = "personal-${this.id.lowercase()}"

    fun getAllExperimentIdListAsStudent(): List<Int> {
        val courseIdList = mysql.courseStudents.filter { it.studentId.eq(this.id) }.map { it.courseId }
        return if (courseIdList.isEmpty()) listOf()
        else mysql.experiments.filter { it.courseId.inList(courseIdList) }.map { it.id }.distinct()
    }

    fun getAllManagedExperimentIdList(): List<Int> {
        val courseIdList = if (isAdmin()) {
            mysql.courses.map { it.id }
        } else if (isTeacher()) {
            mysql.courses.filter { it.teacherId.eq(this.id) }.map { it.id }
        } else {
            mysql.assistants.filter { it.studentId.eq(this.id) }.map { it.courseId }.map { it.toInt() }
        }.distinct()
        return if (courseIdList.isEmpty()) listOf()
        else mysql.experiments.filter { it.courseId.inList(courseIdList) }.map { it.id }.distinct()
    }

    fun getAllAssistantIdList(): List<String> {
        if (!isTeacher()) return listOf()
        val courseIdList = mysql.courses.filter { it.teacherId.eq(this.id) }.map { it.id.toString() }
        if (courseIdList.isEmpty()) return listOf()
        return mysql.assistants.filter { it.courseId.inList(courseIdList) }.map { it.studentId }
    }

    fun assistantGetAllTeacherIdList(): List<String> {
        val courseIdList = mysql.assistants.filter { it.studentId.eq(it.studentId) }.map { it.courseId.toInt() }
        return mysql.courses.filter { it.id.inList(courseIdList) }.map { it.teacher.id }
    }

    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}

open class Users(alias: String?) : Table<User>("user", alias) {
    companion object : Users(null)

    override fun aliased(alias: String) = Users(alias)

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

    @Suppress("unused")
    val paasToken = varchar("paas_token").bindTo { it.paasToken }

    fun getByID(id: String): User? =
        mysql.users.find { Users.id eq id }
}

val Database.users get() = this.sequenceOf(Users)
