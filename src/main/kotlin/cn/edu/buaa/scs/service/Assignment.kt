package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.auth.authAdmin
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.S3
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.exists
import cn.edu.buaa.scs.utils.getFileExtension
import cn.edu.buaa.scs.utils.user
import io.ktor.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.ktorm.dsl.*
import org.ktorm.entity.*
import java.util.*

val ApplicationCall.assignment: AssignmentService
    get() = AssignmentService(this)

class AssignmentService(val call: ApplicationCall) : FileService.IFileManageService {

    companion object {
        private const val bucketName = "scs-assignment"
    }

    private val s3: S3 by lazy { S3(bucketName) }

    fun create(expId: Int, owner: String): Assignment {
        if (mysql.assignments.exists { (it.studentId eq owner) and (it.expId eq expId) }) {
            // 如果之前已经创建过了，那么直接返回
            return mysql.assignments.find { it.studentId eq owner and (it.expId eq expId) }!!
        }
        val experiment = Experiment.id(expId)
        val assignment = Assignment {
            this.studentId = owner
            this.experiment = experiment
            this.course = experiment.course
            this.createdAt = System.currentTimeMillis()
            this.updatedAt = System.currentTimeMillis()
        }
        call.user().authAdmin(assignment)
        mysql.assignments.add(assignment)
        return assignment
    }

    fun getAll(expId: Int): List<Assignment> {
        val experiment = Experiment.id(expId)
        val baseQuery = mysql.assignments.filter {
            (it.expId eq expId) and
                    (it.fileId.isNotNull()) and
                    (it.fileId.notEq(0))
        }
        return if (call.user().isCourseTeacher(experiment.course) || call.user()
                .isCourseAssistant(experiment.course) || call.user().isAdmin()
        ) {
            baseQuery.toList()
        } else {
            baseQuery.filter { it.studentId eq call.user().id }.toList()
        }
    }


    fun get(assignmentId: Int): Assignment {
        val assignment = mysql.assignments.find { it.id eq assignmentId }
            ?: throw NotFoundException("assignment($assignmentId) not found")
        call.user().assertRead(assignment)
        return assignment
    }

    fun patch(assignmentId: Int, fileId: Int): Assignment {
        val assignment = mysql.assignments.find { it.id eq assignmentId }
            ?: throw NotFoundException("assignment($assignmentId) not found")
        call.user().assertWrite(assignment)
        val file = File.id(fileId)
        if (file.owner != assignment.studentId) {
            throw AuthorizationException("file owner(${file.owner} is conflict with assignment student(${assignment.studentId}")
        }
        assignment.file = file
        assignment.updatedAt = System.currentTimeMillis()
        mysql.assignments.update(assignment)
        return assignment
    }

    override fun manager(): S3 = s3

    override fun fixName(originalName: String?, ownerId: String, involvedId: Int): Pair<String, String> {
        val owner = User.id(ownerId)
        val assignment = Assignment.id(involvedId)
        val expName = assignment.experiment.name.filterNot { it.isWhitespace() }
        val fileExtension = (originalName ?: "").getFileExtension()

        val name = "${owner.name}_${owner.id}_$expName.$fileExtension"
        val storeName = "exp-${assignment.experiment.id}/${owner.name}_${owner.id}_${UUID.randomUUID()}.$fileExtension"
        return Pair(name, storeName)
    }

    override fun checkPermission(ownerId: String, involvedId: Int): Boolean {
        return Assignment.id(involvedId).studentId == ownerId
    }

    override fun storePath(): String {
        return bucketName
    }

    override fun callback(involvedEntity: IEntity, file: File) {
        // do noting
    }

    override suspend fun packageFiles(involvedId: Int): FileService.PackageResult =
        withContext(Dispatchers.Default) {
            val experiment = Experiment.id(involvedId)

            val validAssignments = mysql.assignments
                .filter { it.expId eq experiment.id }
                .filterNot { (it.fileId eq 0) or (it.fileId.isNull()) }
                .toList()

            val files = validAssignments.mapNotNull { it.file }

            val readme = async {
                val validStudentIds = validAssignments.map { it.studentId }
                val invalidStudents = mysql.from(Users)
                    .leftJoin(CourseStudents, on = Users.id eq CourseStudents.studentId)
                    .select()
                    .where {
                        // 选了这个课的
                        var condition = (CourseStudents.courseId eq experiment.course.id)
                        // 并且不是交了作业的
                        if (validStudentIds.isNotEmpty()) {
                            condition = condition.and(Users.id notInList validStudentIds)
                        }
                        condition
                    }
                    .map { row -> Users.createEntity(row) }

                val invalidStudentListInfo =
                    if (invalidStudents.isNotEmpty()) {
                        "未提交作业名单:${invalidStudents.joinToString { "${it.id}\t${it.name}\n" }}"
                    } else {
                        ""
                    }

                "已提交作业: ${validStudentIds.size} 人\n未提交作业: ${invalidStudents.size} 人\n$invalidStudentListInfo"
            }

            val filename = "全部作业_${experiment.name.filterNot { it.isWhitespace() }}.zip"

            FileService.PackageResult(files, readme.await(), filename)
        }

}

fun Assignment.Companion.id(id: Int): Assignment {
    return mysql.assignments.find { it.id eq id }
        ?: throw BusinessException("find course($id) from database error")
}

