package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.auth.authAdmin
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.file.FileManager
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.exists
import cn.edu.buaa.scs.utils.getFileExtension
import cn.edu.buaa.scs.utils.toTimestamp
import cn.edu.buaa.scs.utils.user
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.ktorm.dsl.*
import org.ktorm.entity.*
import java.util.*

val ApplicationCall.assignment: AssignmentService
    get() = AssignmentService.getSvc(this) { AssignmentService(this) }

class AssignmentService(val call: ApplicationCall) : IService, FileService.FileDecorator {

    companion object : IService.Caller<AssignmentService>() {
        private const val bucketName = "scs-assignment"
    }

    private val fileManager: FileManager by lazy { FileManager.buildFileManager("local", bucketName) }

    fun create(expId: Int, owner: String): Assignment {
        if (mysql.assignments.exists { (it.studentId eq owner) and (it.expId eq expId) }) {
            // 如果之前已经创建过了，那么直接返回
            return mysql.assignments.find { it.studentId eq owner and (it.expId eq expId) }!!
        }
        val experiment = Experiment.id(expId)
        val assignment = Assignment {
            this.studentId = owner
            this.experimentId = experiment.id
            this.courseId = experiment.course.id
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

    fun patch(assignmentId: Int, fileId: Int?, finalScore: Double?): Assignment {
        val assignment = mysql.assignments.find { it.id eq assignmentId }
            ?: throw NotFoundException("assignment($assignmentId) not found")
        call.user().assertWrite(assignment)
        fileId?.let { id ->
            val file = File.id(id)
            if (file.owner != assignment.studentId) {
                throw AuthorizationException("file owner(${file.owner} is conflict with assignment student(${assignment.studentId}")
            }
            assignment.file = file
        }
        finalScore?.let { score ->
            assignment.finalScore = score.toFloat()
        }
        assignment.updatedAt = System.currentTimeMillis()
        mysql.useTransaction {
            mysql.assignments.update(assignment)
            if (fileId != null) {
                // 说明是新提交的作业，清除之前可能存在的评阅记录
                mysql.delete(AssignmentReviews) {
                    it.assignmentId eq assignmentId
                }
            }
        }
        return assignment
    }

    override fun manager(): FileManager = fileManager

    override fun fixName(originalName: String?, ownerId: String, involvedId: Int): Pair<String, String> {
        val owner = User.id(ownerId)
        val assignment = Assignment.id(involvedId)
        val expName = Experiment.id(assignment.experimentId).name.filterNot { it.isWhitespace() }
        val fileExtension = (originalName ?: "").getFileExtension()

        val name = "${owner.id}_${owner.name}_$expName.$fileExtension"
        val storeName = "exp-${assignment.experimentId}/${owner.name}_${owner.id}_${UUID.randomUUID()}.$fileExtension"
        return Pair(name, storeName)
    }

    override fun checkPermission(ownerId: String, involvedId: Int): Boolean {
        return Assignment.id(involvedId).studentId == ownerId
    }

    override fun storePath(): String {
        return bucketName
    }

    override fun beforeUploadFile(involvedEntity: IEntity, filePart: FileService.FilePart) {
        val assignment = involvedEntity as Assignment
        if (System.currentTimeMillis() > Experiment.id(assignment.experimentId).deadline.toTimestamp()) {
            throw BadRequestException("已过作业提交截止时间")
        }
    }

    /**
     * 作业打包时默认全部打包, 不关心 fileIdList
     */
    override suspend fun packageFiles(involvedId: Int, fileIdList: List<Int>?): FileService.PackageResult =
        withContext(Dispatchers.Default) {
            val experiment = Experiment.id(involvedId)

            val validAssignments = mysql.assignments
                .filter { it.expId eq experiment.id }
                .filterNot { (it.fileId eq 0) or (it.fileId.isNull()) }
                .toList()

            val files = validAssignments.mapNotNull { it.file }
            files.forEach {
                if (it.name.first().code >= 256) {
                    // 如果作业的文件名是 姓名_学号_xxx 这种形式的，要改成 学号_姓名_xxx 这种
                    val strList = it.name.split("_")
                    it.name =
                        "${strList[1]}_${strList[0]}_" + strList.subList(2, strList.size).joinToString(separator = "_")
                }
            }

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

val ApplicationCall.assignmentReview: AssignmentReviewService
    get() = AssignmentReviewService.getSvc(this) { AssignmentReviewService(this) }

class AssignmentReviewService(val call: ApplicationCall) : IService, FileService.FileDecorator {

    companion object : IService.Caller<AssignmentReviewService>() {
        private const val bucketName = "scs-reviewed-assignments"
    }

    private fun assertAdminPermission(assignmentId: Int): Assignment {
        val assignment = Assignment.id(assignmentId)
        call.user().assertWrite(Experiment.id(assignment.experimentId))
        return assignment
    }

    fun post(assignmentId: Int, fileId: Int): AssignmentReview {
        val assignment = assertAdminPermission(assignmentId)
        val assignmentReview = AssignmentReview {
            this.assignmentId = assignmentId
            this.fileId = fileId
            this.reviewedAt = System.currentTimeMillis()
            this.reviewerId = call.user().id
            this.reviewerName = call.user().name
        }
        mysql.useTransaction {
            mysql.assignmentReviews.add(assignmentReview)
            assignment.assignmentReview = assignmentReview
            mysql.assignments.update(assignment)
        }
        return assignmentReview
    }

    fun delete(assignmentId: Int) {
        val assignment = assertAdminPermission(assignmentId)
        mysql.useTransaction {
            mysql.delete(AssignmentReviews) { it.assignmentId eq assignmentId }
            assignment.assignmentReview = null
            mysql.assignments.update(assignment)
        }
    }

    fun get(assignmentId: Int): List<AssignmentReview> {
        val assignment = Assignment.id(assignmentId)
        call.user().assertRead(assignment)
        return mysql.assignmentReviews.filter { it.assignmentId eq assignmentId }.toList()
    }

    private val fileManager = FileManager.buildFileManager("local", bucketName)

    override fun manager(): FileManager {
        return fileManager
    }

    override fun fixName(originalName: String?, ownerId: String, involvedId: Int): Pair<String, String> {
        val subStoreName = "${originalName ?: ""}-${UUID.randomUUID()}.${originalName?.getFileExtension() ?: ""}"
        val storeName = "assignment-${involvedId}/$subStoreName"
        return Pair(originalName!!, storeName)
    }

    override fun checkPermission(ownerId: String, involvedId: Int): Boolean {
        return true
    }

    override fun storePath(): String {
        return bucketName
    }
}

fun Assignment.Companion.id(id: Int): Assignment {
    return mysql.assignments.find { it.id eq id }
        ?: throw BusinessException("find course($id) from database error")
}

fun Assignment.Companion.isPeerTarget(assignmentId: Int, userId: String): Boolean {
    return mysql.peerTasks.exists { it.assignmentId.eq(assignmentId) and it.assessorId.eq(userId) }
}

