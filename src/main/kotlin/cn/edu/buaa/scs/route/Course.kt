package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.CourseResourceResponse
import cn.edu.buaa.scs.controller.models.CourseResponse
import cn.edu.buaa.scs.controller.models.DeleteCourseResourcesRequest
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.Course
import cn.edu.buaa.scs.model.CourseResource
import cn.edu.buaa.scs.model.FileType
import cn.edu.buaa.scs.service.course
import cn.edu.buaa.scs.service.courseResource
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.courseRoute() {

    route("/courses") {
        get {
            call.respond(call.course.getAllCourses().map { call.convertCourseResponse(it) })
        }
    }

    route("course/{courseId}") {
        fun ApplicationCall.getCourseIdFromPath(): Int =
            parameters["courseId"]?.toInt()
                ?: throw BadRequestException("course id is invalid")

        get {
            val courseId = call.getCourseIdFromPath()
            val course = call.course.get(courseId)
            call.respond(call.convertCourseResponse(course, true))
        }

        route("/resource") {
            route("/{resourceId}") {
                fun ApplicationCall.getResourceIdFromPath(): Int =
                    parameters["resourceId"]?.toInt()
                        ?: throw BadRequestException("course_resource id is invalid")

                delete {
                    val callback = call.courseResource.delete(
                        call.getCourseIdFromPath(),
                        call.getResourceIdFromPath()
                    )
                    call.respond("OK")
                    callback.invoke()
                }
            }
        }

        route("/resources") {

            /**
             * 获取课程资源信息
             */
            get {
                val type = call.parameters["type"]?.let { FileType.Resource.valueOf(it) }
                call.courseResource.getAll(call.getCourseIdFromPath(), type).let {
                    call.respond(it.map { resource -> convertCourseResource(resource) })
                }
            }

            /**
             * 批量删除课程资源
             */
            delete {
                val req = call.receive<DeleteCourseResourcesRequest>()
                val callback = call.courseResource.deleteBatch(call.getCourseIdFromPath(), req.idList)
                call.respond("OK")
                callback.invoke()
            }
        }
    }

}

internal fun ApplicationCall.convertCourseResponse(course: Course, hasCount: Boolean = false): CourseResponse {
    return CourseResponse(
        id = course.id,
        name = course.name,
        teacher = course.teacher.name,
        term = convertTermModel(course.term),
        createTime = course.createTime,
        departmentId = course.departmentId,
        studentCnt = if(hasCount) this.course.studentCnt(course.id) else null
    )
}

internal fun convertCourseResource(courseResource: CourseResource): CourseResourceResponse {
    return CourseResourceResponse(
        id = courseResource.id,
        courseId = courseResource.id,
        experimentId = courseResource.expId ?: 0,
        file = convertFileResponse(courseResource.file)
    )
}