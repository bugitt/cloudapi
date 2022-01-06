package cn.edu.buaa.scs.experiment

import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.model.FileType
import cn.edu.buaa.scs.model.StoreType
import cn.edu.buaa.scs.storage.assignments
import cn.edu.buaa.scs.storage.files
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.storage.uploadFile
import org.ktorm.entity.add
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
    val assignment = Assignment {
        studentId = userId
        this.expId = expId
    }

    mysql.useTransaction {
        // create assignment
        mysql.assignments.add(assignment)

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
            assignment.flushChanges()
        }
    }

    return assignment
}