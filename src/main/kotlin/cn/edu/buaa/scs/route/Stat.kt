package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.service.CourseService
import cn.edu.buaa.scs.service.course
import cn.edu.buaa.scs.service.resourceFile
import cn.edu.buaa.scs.service.stat
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.statRoute() {
    route("/stat") {
        route("/course") {
            route("/{courseId}") {

                fun ApplicationCall.getCourseIdFromPath() =
                    parameters["courseId"]?.toIntOrNull() ?: throw BadRequestException("courseId is not an integer")

                get("/experiments") {
                    call.course.statCourseExps(call.getCourseIdFromPath()).let {
                        call.respond(convertStatCourseExpsResponse(call, it))
                    }
                }
            }
        }

        route("/exp") {
            route("/{expId}") {
                fun ApplicationCall.getExpIdFromPath() =
                    parameters["expId"]?.toIntOrNull() ?: throw BadRequestException("expId is not an integer")

                route("/assignments") {
                    get {
                        call.respond(
                            convertStatExpAssignmentResponse(
                                call.stat.expAssignments(call.getExpIdFromPath())
                            )
                        )
                    }
                }
            }
        }
    }
}

internal fun convertStatExpAssignmentResponse(assignments: Map<User, Assignment?>): StatExpAssignmentResponse {
    val itemArray = assignments.map { (user, assignment) ->
        StatExpAssignmentResponseAssignments(user.id, user.name, assignment?.let { convertAssignment(it) })
    }
    return StatExpAssignmentResponse(itemArray)
}

internal fun convertStatCourseExp(expDetail: CourseService.StatCourseExps.ExpDetail): StatCourseExp {
    val experiment = expDetail.exp
    return StatCourseExp(
        id = experiment.id,
        name = experiment.name,
        type = experiment.type,
        detail = experiment.detail,
        resourceFile = experiment.resourceFile?.let { convertFileResponse(it) },
        createTime = experiment.createTime,
        startTime = experiment.startTime,
        endTime = experiment.endTime,
        deadline = experiment.deadline,
        isPeerAssessment = experiment.isPeerAssessment,
        peerAssessmentDeadline = experiment.peerAssessmentDeadline,
        appealDeadline = experiment.appealDeadline,
        peerAssessmentRules = experiment.peerAssessmentRules,
        peerAssessmentStart = experiment.peerAssessmentStart,
        sentEmail = experiment.sentEmail,
        vm = if (experiment.vmStatus == 0) null else ExpVmInfo(
            status = experiment.vmStatus,
            name = experiment.vmName,
            applyId = experiment.vmApplyId,
            password = experiment.vmPasswd,
            cnt = expDetail.vmCnt,
        ),
        submittedAssignmentsCnt = expDetail.submittedAssignmentsCnt
    )
}

internal fun convertStatCourseExpsResponse(
    call: ApplicationCall,
    source: CourseService.StatCourseExps
): StatCourseExpsResponse {
    return StatCourseExpsResponse(
        course = convertCourseResponse(call, source.course),
        teacher = convertUserModel(source.teacher),
        students = source.students.map { convertUserModel(it) },
        exps = source.expDetails.map { convertStatCourseExp(it) }
    )
}