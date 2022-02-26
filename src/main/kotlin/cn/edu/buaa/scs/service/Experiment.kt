package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.controller.models.CreateExperimentRequest
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.S3
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.*
import io.ktor.application.*
import org.ktorm.dsl.*
import org.ktorm.entity.*
import java.util.*

val ApplicationCall.experiment get() = ExperimentService(this)

class ExperimentService(val call: ApplicationCall) : FileService.IFileManageService {
    fun create(req: CreateExperimentRequest): Experiment {
        val course = Course.id(req.courseId)
        // 检查一下有没有同名的实验
        if (mysql.experiments.exists { it.courseId.eq(req.courseId) and it.name.eq(req.name) }) {
            throw BadRequestException("实验名重复")
        }
        val experiment = Experiment {
            this.course = course
            this.name = req.name
            this.type = req.type
            this.detail = req.description ?: ""
            this.createTime = TimeUtil.currentDateTime()
            this.startTime = req.startTime
            this.endTime = req.endTime
            this.deadline = req.deadline
            this.isPeerAssessment = req.enablePeer
        }
        if (experiment.isPeerAssessment) {
            if (CommonUtil.isEmpty(req.peerDescription, req.peerEndTime, req.peerAppealDeadline)) {
                throw BusinessException("peer assessment info incomplete")
            }
            experiment.peerAssessmentRules = req.peerDescription!!
            experiment.peerAssessmentDeadline = req.peerEndTime!!
            experiment.appealDeadline = req.peerAppealDeadline!!
        }
        mysql.experiments.add(experiment)
        return experiment
    }

    fun get(id: Int): Experiment {
        val experiment = Experiment.id(id)
        call.user().assertRead(experiment)
        return experiment
    }

    fun getAll(termId: Int? = null, submitted: Boolean? = null, courseId: Int? = null): List<Experiment> {
        val aTermId = if (termId == null || termId <= 0) {
            mysql.terms.sortedBy { it.id }.last().id
        } else {
            termId
        }
        val courseIdList = if (courseId != null && courseId != 0) {
            call.user().assertRead(Course.id(courseId))
            listOf(courseId)
        } else {
            mysql.from(Courses)
                .leftJoin(CourseStudents, on = CourseStudents.courseId.eq(Courses.id)).select()
                .where { CourseStudents.studentId.eq(call.userId()) and Courses.termId.eq(aTermId) }
                .map { row -> Courses.createEntity(row) }
                .map { it.id }
        }
        if (courseIdList.isEmpty()) return emptyList()

        val experiments = mysql.experiments.filter { it.courseId.inList(courseIdList) }.toList()
        if (submitted == null) {
            return mysql.experiments.filter { it.courseId.inList(courseIdList) }.toList()
        }
        val submittedExperiments =
            mysql.assignments.filter { it.fileId.isNotNull() and it.fileId.notEq(0) }.map { it.experiment.id }.toSet()
        return if (submitted) {
            experiments.filter { submittedExperiments.contains(it.id) }
        } else {
            experiments.filterNot { submittedExperiments.contains(it.id) }
        }
    }

    fun statExp(experiment: Experiment): CourseService.StatCourseExps.ExpDetail {
        // 统计已经交作业的人数
        val submittedAssignmentCnt =
            mysql.assignments.count { it.expId.eq(experiment.id) and it.fileId.isNotNull() and it.fileId.notEq(0) }
        // TODO: 统计虚拟机数量
        val vmCnt = 0
        return CourseService.StatCourseExps.ExpDetail(experiment, vmCnt, submittedAssignmentCnt)
    }

    companion object {
        private const val bucket = "exp-resource"
        private val s3 by lazy { S3(bucket) }
    }

    override fun manager(): S3 {
        return s3
    }

    override fun fixName(originalName: String?, ownerId: String, involvedId: Int): Pair<String, String> {
        return Pair(originalName ?: "", "$originalName-${UUID.randomUUID()}.${originalName?.getFileExtension()}")
    }

    override fun checkPermission(ownerId: String, involvedId: Int): Boolean {
        val course = Experiment.id(involvedId).course
        val user = User.id(ownerId)
        return user.isCourseAssistant(course) || user.isCourseTeacher(course)
    }

    override fun storePath(): String {
        return bucket
    }

    override fun afterCreateOrUpdate(involvedEntity: IEntity, file: File) {
        val experiment = involvedEntity as Experiment
        experiment.resourceFile = file
        mysql.experiments.update(experiment)
    }
}

fun Experiment.Companion.id(id: Int): Experiment {
    return mysql.experiments.find { it.id eq id }
        ?: throw BusinessException("find experiment($id) from database error")
}