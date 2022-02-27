package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.CourseResourceListResponse
import cn.edu.buaa.scs.controller.models.CourseResourceResponse
import cn.edu.buaa.scs.controller.models.CourseResponse
import cn.edu.buaa.scs.controller.models.DeleteCourseResourcesRequest
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.Course
import cn.edu.buaa.scs.model.CourseResource
import cn.edu.buaa.scs.service.course
import cn.edu.buaa.scs.service.courseResource
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.courseRoute() {

    route("course/{courseId}") {
        fun ApplicationCall.getCourseIdFromPath(): Int =
            parameters["courseId"]?.toInt()
                ?: throw BadRequestException("course id is invalid")

        get {
            val courseId = call.getCourseIdFromPath()
            val course = call.course.get(courseId)
            call.respond(convertCourseResponse(call, course))
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
                call.courseResource.getAll(call.getCourseIdFromPath()).let {
                    call.respond(convertCourseResourceList(it))
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

internal fun convertCourseResponse(call: ApplicationCall, course: Course): CourseResponse {
    return CourseResponse(
        id = course.id,
        name = course.name,
        teacher = course.teacher.name,
        term = convertTermModel(course.term),
        createTime = course.createTime,
        departmentId = course.departmentId,
        studentCnt = call.course.studentCnt(course.id)
    )
}

internal fun convertCourseResource(courseResource: CourseResource): CourseResourceResponse {
    return CourseResourceResponse(
        id = courseResource.id,
        courseId = courseResource.course.id,
        file = convertFileResponse(courseResource.file)
    )
}

internal fun convertCourseResourceList(courseResourceList: List<CourseResource>): CourseResourceListResponse {
    return CourseResourceListResponse(
        resources = courseResourceList.sortedBy { it.file.uploadTime }.map { convertCourseResource(it) }
    )
}