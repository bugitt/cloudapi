package cn.edu.buaa.scs.experiment

import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.model.FileType
import cn.edu.buaa.scs.model.StoreType
import cn.edu.buaa.scs.storage.assignments
import cn.edu.buaa.scs.storage.files
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.exists
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.ktorm.entity.update
import java.io.InputStream
import java.util.*
import cn.edu.buaa.scs.storage.uploadFile as s3UploadFile

const val bucket = "scs-assignment"

private fun uploadFile(
    owner: String,
    uploader: String,
    originalName: String,
    inputStream: InputStream
): File {
    val storeFileName = UUID.randomUUID().toString()
    val fileResp = s3UploadFile(bucket, storeFileName, inputStream)
    return File {
        name = originalName
        storeType = StoreType.S3
        storeName = storeFileName
        storePath = "$bucket/$storeFileName"
        uploadTime = fileResp.lastModified().toInstant().toEpochMilli()
        fileType = FileType.Assignment
        size = fileResp.size()
        this.uploader = uploader
        this.owner = owner
        this.createdAt = System.currentTimeMillis()
        this.updatedAt = System.currentTimeMillis()
    }
}

fun createAssignment(
    expId: Int,
    owner: String,
    uploader: String,
    originalName: String,
    inputStream: InputStream
): Assignment {
    // 检查是或否已经提交过作业了
    if (mysql.assignments.exists { (it.studentId eq owner) and (it.expId eq expId) }) {
        throw BusinessException("student($owner) has already created assignment for exp($expId)")
    }

    val file = uploadFile(owner, uploader, originalName, inputStream)
    val assignment = Assignment {
        studentId = owner
        this.expId = expId
        this.createdAt = System.currentTimeMillis()
        this.updatedAt = System.currentTimeMillis()
    }
    mysql.useTransaction {
        // create assignment
        mysql.assignments.add(assignment)
        updateAssigmentFile(assignment, file)
    }
    return assignment
}

fun updateAssigmentFile(
    assignmentId: Int,
    owner: String,
    uploader: String,
    originalName: String,
    inputStream: InputStream
): Assignment {
    val file = uploadFile(owner, uploader, originalName, inputStream)
    return mysql.useTransaction {
        val assignment = mysql.assignments
            .find { it.id eq assignmentId } ?: throw BusinessException("experiment with expId($assignmentId) not found")
        updateAssigmentFile(assignment, file)
        assignment
    }
}

private fun updateAssigmentFile(
    assignment: Assignment,
    file: File
) {
    mysql.files.add(file)
    assignment.fileId = file.id
    assignment.updatedAt = System.currentTimeMillis()
    mysql.assignments.update(assignment)
}