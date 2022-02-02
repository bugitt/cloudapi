package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertAdmin
import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.auth.authAdmin
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.exists
import cn.edu.buaa.scs.utils.getFileExtension
import cn.edu.buaa.scs.utils.user
import io.ktor.application.*
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.ktorm.entity.update
import java.io.InputStream

val ApplicationCall.assignment: AssignmentService
    get() = AssignmentService(this)

class AssignmentService(val call: ApplicationCall) {

    fun create(expId: Int, owner: String): Assignment {
        if (mysql.assignments.exists { (it.studentId eq owner) and (it.expId eq expId) }) {
            // 如果之前已经创建过了，那么直接返回
            return mysql.assignments.find { it.studentId eq owner and (it.expId eq expId) }!!
        }
        val assignment = Assignment {
            this.studentId = owner
            this.expId = expId
            this.courseId = Experiment.id(expId).courseId
            this.createdAt = System.currentTimeMillis()
            this.updatedAt = System.currentTimeMillis()
        }
        call.user().authAdmin(assignment)
        mysql.assignments.add(assignment)
        return assignment
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
        assignment.fileId = fileId
        mysql.assignments.update(assignment)
        return assignment
    }

    fun create(
        expId: Int,
        owner: String,
        originalName: String,
        inputStream: InputStream
    ): Assignment {
        // 检查是或否已经提交过作业了
        if (mysql.assignments.exists { (it.studentId eq owner) and (it.expId eq expId) }) {
            throw BusinessException("student($owner) has already created assignment for exp($expId)")
        }

        val assignment = Assignment {
            studentId = owner
            this.expId = expId
            this.courseId = Experiment.id(expId).courseId
            this.createdAt = System.currentTimeMillis()
            this.updatedAt = System.currentTimeMillis()
        }

        call.user().assertAdmin(assignment)

        val file = call.file.create(owner, buildFilename(expId, owner, originalName), FileType.Assignment, inputStream)
        mysql.useTransaction {
            // create assignment
            mysql.assignments.add(assignment)
            updateAssignmentFile(assignment, file)
        }
        return assignment
    }

    fun update(
        assignment: Assignment,
        originalName: String,
        inputStream: InputStream
    ): Assignment {
        call.user().assertWrite(assignment)
        val file = call.file.create(
            assignment.studentId,
            buildFilename(assignment.expId, assignment.studentId, originalName),
            FileType.Assignment,
            inputStream
        )
        mysql.useTransaction {
            updateAssignmentFile(assignment, file)
        }
        return assignment
    }

    private fun buildFilename(expId: Int, ownerId: String, originalName: String): String {
        val owner = User.id(ownerId)
        return "${owner.name}_${owner.id}_${Experiment.id(expId).name}.${originalName.getFileExtension()}"
    }

    private fun updateAssignmentFile(
        assignment: Assignment,
        file: File
    ) {
        file.involvedId = assignment.id
        mysql.files.add(file)
        assignment.fileId = file.id
        assignment.updatedAt = System.currentTimeMillis()
        mysql.assignments.update(assignment)
    }
}

fun Assignment.Companion.id(id: Int): Assignment {
    return mysql.assignments.find { it.id eq id }
        ?: throw BusinessException("find course($id) from database error")
}

val Assignment.Companion.bucket: String
    get() = "scs-assignment"

fun Assignment.getCourse(): Course = Course.id(this.courseId)

