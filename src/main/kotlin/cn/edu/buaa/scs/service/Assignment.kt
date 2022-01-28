package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertAdmin
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.error.BusinessException
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

fun Assignment.getExperiment(): Experiment = Experiment.id(this.expId)

