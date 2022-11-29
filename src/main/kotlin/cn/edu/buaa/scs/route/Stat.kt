package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.StatCourseExp
import cn.edu.buaa.scs.controller.models.StatCourseExpsResponse
import cn.edu.buaa.scs.controller.models.StatExpAssignmentResponse
import cn.edu.buaa.scs.controller.models.StatExpAssignmentResponseAssignments
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.service.CourseService
import cn.edu.buaa.scs.service.course
import cn.edu.buaa.scs.service.project
import cn.edu.buaa.scs.service.stat
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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

        route("/resourcePools/{resourcePoolId}") {
            fun ApplicationCall.getResourceId(): String {
                return parameters["resourcePoolId"] ?: throw BadRequestException("expId is not an integer")
            }

            get("/used") {
                call.respond(call.project.getResourcePoolUsedStat(call.getResourceId()))
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
        vm = experiment.getVmApply()?.let { convertExpVmInfo(it) },
        submittedAssignmentsCnt = expDetail.submittedAssignmentsCnt
    )
}

internal fun convertStatCourseExpsResponse(
    call: ApplicationCall,
    source: CourseService.StatCourseExps
): StatCourseExpsResponse {
    return StatCourseExpsResponse(
        course = call.convertCourseResponse(source.course),
        teacher = convertUserModel(source.teacher),
        studentCnt = source.studentCnt,
        exps = source.expDetails.map { convertStatCourseExp(it) }
    )
}