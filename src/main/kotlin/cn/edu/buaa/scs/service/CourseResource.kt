package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.S3
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.warn
import io.ktor.application.*
import io.ktor.features.*
import org.ktorm.dsl.*
import org.ktorm.entity.*
import java.util.*

val ApplicationCall.courseResource: CourseResourceService get() = CourseResourceService(this)

class CourseResourceService(private val call: ApplicationCall) : FileService.IFileManageService {
    companion object {
        const val bucketName = "scs-course-resource"
    }

    fun getAll(courseId: Int): List<CourseResource> {
        call.user().assertRead(Course.id(courseId))

        return mysql.courseResources.filter {
            (it.courseId eq courseId) and
                    (it.fileId.isNotNull() and it.fileId.notEq(0))
        }.toList()
    }

    suspend fun delete(courseId: Int, resourceId: Int): suspend () -> Unit {
        call.user().assertWrite(Course.id(courseId))
        val resource = CourseResource.id(resourceId)
        if (resource.course.id != courseId) {
            throw BadRequestException("course_resource(${resource.id} conflicts with course(${resource.course.id}")
        }
        mysql.useTransaction {
            resource.file.delete()
            resource.delete()
        }
        return {
            try {
                call.file.deleteFileFromStorage(resource.file)
            } catch (e: Exception) {
                call.warn("delete file(${resource.file.id} from storage error:")
                call.warn(e.stackTraceToString())
            }
        }
    }

    suspend fun deleteBatch(courseId: Int, idList: List<Int>): suspend () -> Unit {
        call.user().assertWrite(Course.id(courseId))

        if (idList.isEmpty()) return {}

        val resources = mysql.courseResources.filter { it.id.inList(idList) }.toList()
        resources.forEach {
            if (it.course.id != courseId) {
                throw BadRequestException("course_resource(${it.id} conflicts with course(${it.course.id}")
            }
        }
        val fileIds = resources.map { it.file.id }
        val resourceIds = resources.map { it.id }
        mysql.useTransaction {
            mysql.files.removeIf { it.id.inList(fileIds) }
            mysql.courseResources.removeIf { it.id.inList(resourceIds) }
        }
        return {
            try {
                resources.forEach { call.file.deleteFileFromStorage(it.file) }
            } catch (e: Exception) {
                call.warn("delete file from storage error:")
                call.warn(e.stackTraceToString())
            }
        }

    }

    private val s3 = S3(bucketName)

    override fun manager(): S3 {
        return s3
    }

    override fun fixName(originalName: String?, ownerId: String, involvedId: Int): Pair<String, String> {
        val name = originalName
            ?: "${Course.id(involvedId).name}-课程资源-${UUID.randomUUID().toString().substring(0..4)}"
        val storeName = "course-$involvedId/${UUID.randomUUID()}-$name"
        return Pair(name, storeName)
    }

    override fun checkPermission(ownerId: String, involvedId: Int): Boolean {
        return ownerId == Course.id(involvedId).teacherId
    }

    override fun storePath(): String {
        return bucketName
    }

    override fun callback(involvedEntity: IEntity, file: File) {
        val course = involvedEntity as Course
        mysql.courseResources.add(CourseResource {
            this.course = course
            this.file = file
        })
    }

    override suspend fun packageFiles(involvedId: Int): FileService.PackageResult {
        val course = Course.id(involvedId)
        val files =
            mysql.courseResources
                .filter { (it.courseId eq involvedId) and (it.fileId.isNotNull() and (it.fileId notEq 0)) }
                .toList()
                .map { it.file }
        val readme = """
            共下载 ${files.size} 个文件:
            
            ${files.joinToString(separator = "\n") { it.name }}
        """.trimIndent()
        val zipName = "${course.name}-资源下载.zip"
        return FileService.PackageResult(files, readme, zipName)
    }

}

fun CourseResource.Companion.id(id: Int): CourseResource {
    return mysql.courseResources.find { it.id eq id }
        ?: throw BusinessException("find course_resource($id) from database error")
}