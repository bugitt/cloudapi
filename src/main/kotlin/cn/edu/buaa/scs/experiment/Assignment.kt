package cn.edu.buaa.scs.experiment

import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.model.FileType
import cn.edu.buaa.scs.model.StoreType
import cn.edu.buaa.scs.storage.assignments
import cn.edu.buaa.scs.storage.files
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.storage.uploadFile
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.ktorm.entity.update
import java.io.InputStream
import java.util.*

const val bucket = "scs-assignment"

fun createAssignment(
    expId: Int,
    userId: String,
    fileUploader: String,
    originalName: String,
    inputStream: InputStream
): Assignment {
    return Assignment {
        studentId = userId
        this.expId = expId
    }.also { assignment ->
        mysql.useTransaction {
            // create assignment
            mysql.assignments.add(assignment)
            updateAssigmentFile(assignment, fileUploader, originalName, inputStream)
        }
    }
}

fun updateAssigmentFile(
    assignmentId: Int,
    fileUploader: String,
    originalName: String,
    inputStream: InputStream
): Assignment {
    return mysql.useTransaction {
        mysql.assignments
            .find { it.id eq assignmentId }
            ?.also {
                updateAssigmentFile(it, fileUploader, originalName, inputStream)
            } ?: throw BusinessException("experiment with expId($assignmentId) not found")
    }
}

private fun updateAssigmentFile(
    assignment: Assignment,
    fileUploader: String,
    originalName: String,
    inputStream: InputStream
) {
    // upload file
    val storeFileName = UUID.randomUUID().toString()
    val fileResp = uploadFile(bucket, storeFileName, inputStream)
    File {
        name = originalName
        storeType = StoreType.S3
        storeName = storeFileName
        storePath = "$bucket/$storeFileName"
        uploadTime = fileResp.lastModified().toLocalDateTime()
        fileType = FileType.Assignment
        size = fileResp.size()
        uploader = fileUploader
        involvedId = assignment.id
    }.let {
        // update database
        mysql.files.add(it)
        assignment.fileId = it.id
        mysql.assignments.update(assignment)
    }
}