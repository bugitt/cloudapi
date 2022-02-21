package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.S3
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.getFileExtension
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.userId
import io.ktor.application.*
import org.ktorm.dsl.*
import org.ktorm.entity.*
import java.util.*

val ApplicationCall.experiment get() = ExperimentService(this)

class ExperimentService(val call: ApplicationCall) : FileService.IFileManageService {
    fun get(id: Int): Experiment {
        val experiment = Experiment.id(id)
        call.user().assertRead(experiment)
        return experiment
    }

    fun getAll(termId: Int? = null, submitted: Boolean? = null): List<Experiment> {
        val aTermId = if (termId == null || termId <= 0) {
            mysql.terms.sortedBy { it.id }.last().id
        } else {
            termId
        }
        val courseIdList = mysql.from(Courses)
            .leftJoin(CourseStudents, on = CourseStudents.courseId.eq(Courses.id)).select()
            .where { CourseStudents.studentId.eq(call.userId()) and Courses.termId.eq(aTermId) }
            .map { row -> Courses.createEntity(row) }
            .map { it.id }
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