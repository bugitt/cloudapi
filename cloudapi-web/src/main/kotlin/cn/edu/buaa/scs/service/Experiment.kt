package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.auth.authRead
import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.kube.BusinessKubeClient
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.file.FileManager
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.*
import cn.edu.buaa.scs.utils.schedule.CommonScheduler
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import org.apache.commons.lang3.RandomStringUtils
import org.ktorm.dsl.*
import org.ktorm.entity.*
import java.util.*

val ApplicationCall.experiment get() = ExperimentService.getSvc(this) { ExperimentService(this) }

class ExperimentService(val call: ApplicationCall) : IService, FileService.FileDecorator {

    fun create(req: CreateExperimentRequest): Experiment {
        val course = Course.id(req.courseId)

        call.user().assertWrite(course)

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
        patchExperimentPeerInfo(experiment, req.peerDescription, req.peerEndTime, req.peerAppealDeadline)
        mysql.experiments.add(experiment)
        return experiment
    }

    fun put(expId: Int, req: PutExperimentRequest): Experiment {
        val experiment = Experiment.id(expId)
        if (req.name != experiment.name) {
            // 检查一下有没有同名的实验
            if (mysql.experiments.exists { it.courseId.eq(experiment.course.id) and it.name.eq(req.name) }) {
                throw BadRequestException("实验名重复")
            }
        }
        call.user().assertWrite(experiment)
        experiment.name = req.name
        experiment.type = req.type
        experiment.detail = req.description ?: ""
        experiment.startTime = req.startTime
        experiment.endTime = req.endTime
        experiment.deadline = req.deadline
        experiment.isPeerAssessment = req.enablePeer
        patchExperimentPeerInfo(experiment, req.peerDescription, req.peerEndTime, req.peerAppealDeadline)
        mysql.experiments.update(experiment)
        return experiment
    }

    private fun patchExperimentPeerInfo(
        experiment: Experiment,
        peerDescription: String?,
        peerEndTime: String?,
        peerAppealDeadline: String?
    ) {
        if (experiment.isPeerAssessment) {
            if (CommonUtil.isEmpty(peerDescription, peerEndTime, peerAppealDeadline)) {
                throw BusinessException("peer assessment info incomplete")
            }
            experiment.peerAssessmentRules = peerDescription!!
            experiment.peerAssessmentDeadline = peerEndTime!!
            experiment.appealDeadline = peerAppealDeadline!!
        }
    }

    fun get(id: Int): Experiment {
        val experiment = Experiment.id(id)
        call.user().assertRead(experiment)
        return experiment
    }

    fun getNameList(courseId: Int? = null): List<String> {
        val experiments = if (courseId == null || courseId == 0) {
            mysql.experiments.toList()
        } else {
            mysql.experiments.filter { it.courseId.eq(courseId) }.toList()
        }
        return experiments.map { it.name }
    }

    fun getAll(termId: Int? = null, submitted: Boolean? = null, courseId: Int? = null): List<Experiment> {
        val aTermId = if (termId == null || termId <= 0) {
            mysql.terms.sortedBy { it.id }.last().id
        } else {
            termId
        }
        val courseIdList: List<Int> = if (courseId != null && courseId != 0) {
            call.user().assertRead(Course.id(courseId))
            listOf(courseId)
        } else {
            mysql.from(Courses)
                .leftJoin(CourseStudents, on = CourseStudents.courseId.eq(Courses.id))
                .select(Courses.id)
                .where { CourseStudents.studentId.eq(call.userId()) and Courses.termId.eq(aTermId) }
                .map { row -> row[Courses.id]!! }
                .toList()
        }
        if (courseIdList.isEmpty()) return emptyList()

        val experiments = mysql.experiments.filter { it.courseId.inList(courseIdList) }.toList()
        if (experiments.isEmpty()) return emptyList()
        if (submitted == null) {
            return experiments
        }
        val submittedExperiments = mysql
            .from(Assignments)
            .select(Assignments.expId)
            .where {
                Assignments.fileId.isNotNull() and
                        Assignments.fileId.notEq(0) and
                        Assignments.expId.inList(experiments.map { it.id }) and
                        Assignments.studentId.eq(call.userId())
            }
            .map { row -> row[Assignments.expId] }
            .toSet()
        return if (submitted) {
            experiments.filter { submittedExperiments.contains(it.id) }
        } else {
            experiments.filterNot { submittedExperiments.contains(it.id) }
        }.sortedByDescending { it.id }
    }

    fun selectStandardAssignments(expId: Int): List<Pair<Assignment, PeerStandard?>> {
        val baseCondition = Assignments.expId.eq(expId) and
                Assignments.fileId.notEq(0) and
                Assignments.fileId.isNotNull()

        val buildPairResult: (List<Assignment>, List<PeerStandard>) -> List<Pair<Assignment, PeerStandard?>> =
            { assignments, peerStandards ->
                if (peerStandards.isEmpty() || assignments.size != peerStandards.size) {
                    assignments.map { Pair(it, null) }
                } else {
                    val assignmentMap = assignments.associateBy { it.id }
                    peerStandards.map { peerStandard ->
                        Pair(assignmentMap[peerStandard.assignmentId] as Assignment, peerStandard)
                    }
                }
            }

        val experiment = Experiment.id(expId)

        call.user().assertWrite(experiment)

        // 获取标准评分任务列表
        val peerStandardList = mysql.peerStands.filter { it.expId.eq(expId) }.toList()
        if (peerStandardList.size >= 8 || experiment.peerAssessmentStart) {
            // 如果满足条件, 直接返回就好
            return buildPairResult(
                mysql
                    .assignments
                    .filter { it.id.inList(peerStandardList.map { p -> p.assignmentId }) }
                    .toList(),
                peerStandardList
            )
        }

        // 如果有脏数据，那么先清理一下
        if (peerStandardList.isNotEmpty()) {
            mysql.delete(PeerStandards) {
                it.id.inList(peerStandardList.map { p -> p.id })
            }
        }
        if (mysql.assignments.count { baseCondition } < 8) {
            throw BadRequestException("已提交作业人数不足8人，无法开启互评")
        }

        // 选出8份作业
        val randomAssignmentList = mysql
            .assignments
            .filter { baseCondition }
            .toList()
            .shuffled()
            .take(8)

        // 对每份作业创建一个作业互评任务
        mysql.useTransaction {
            mysql.batchInsert(PeerStandards) {
                randomAssignmentList.forEach { assignment ->
                    item {
                        set(it.assignmentId, assignment.id)
                        set(it.expId, assignment.experimentId)
                        set(it.isCompleted, false)
                    }
                }
            }
        }
        return buildPairResult(randomAssignmentList, listOf())
    }

    fun statExp(experiment: Experiment, submittedAssignmentCnt: Int): CourseService.StatCourseExps.ExpDetail {
        // TODO: 统计虚拟机数量
        val vmCnt = 0
        return CourseService.StatCourseExps.ExpDetail(experiment, vmCnt, submittedAssignmentCnt)
    }

    fun getSimpleWorkflowConfiguration(expId: Int): SimpleWorkflowConfigurationResponse {
        val experiment = Experiment.id(expId)
        call.user().assertRead(experiment)
        val entityList =
            mysql.experimentWorkflowConfigurations.filter { it.expId.eq(expId) }.map { SimpleEntity(it.id, it.name) }
                .toList()
        val hasSubmitType =
            mysql.experimentWorkflowConfigurations.exists { it.expId.eq(expId).and(it.needSubmit.eq(true)) }
        return SimpleWorkflowConfigurationResponse(entityList, hasSubmitType)
    }

    fun getWorkflowConfigurationById(id: Long): ExperimentWorkflowConfiguration {
        val conf = mysql.experimentWorkflowConfigurations.find { it.id eq id }
            ?: throw NotFoundException("workflow configuration $id not found")
        val experiment = Experiment.id(conf.expId)
        call.user().assertRead(experiment)
        return conf
    }

    fun deleteWorkflowConfigurationById(id: Long) {
        val conf = mysql.experimentWorkflowConfigurations.find { it.id eq id }
            ?: throw NotFoundException("workflow configuration $id not found")
        val experiment = Experiment.id(conf.expId)
        call.user().assertWrite(experiment)
        mysql.useTransaction {
            // delete conf
            conf.delete()
            // delete resourcePool
            mysql.resourcePools.removeIf { it.name eq conf.resourcePool }
            // delete resourcePoolCrd
            BusinessKubeClient.deleteResourcePool(conf.resourcePool)
        }
        conf.delete()
    }

    fun getWorkflowConfigurationListByExp(expId: Int): List<ExperimentWorkflowConfiguration> {
        val experiment = Experiment.id(expId)
        call.user().assertRead(experiment)
        return mysql.experimentWorkflowConfigurations.filter { it.expId.eq(expId) }.toList()
    }

    suspend fun createOrUpdateWorkflowConfiguration(
        expId: Int,
        resource: ResourceModel,
        configuration: String,
        name: String,
        reqStudentIdList: List<String>?,
        needSubmit: Boolean,
    ): ExperimentWorkflowConfiguration {
        if (needSubmit && mysql.experimentWorkflowConfigurations.exists {
                it.expId.eq(expId).and(it.needSubmit.eq(true))
            }) {
            throw BadRequestException("同一实验仅能配置一项用于作业提交与展示的工作流")
        }

        val experiment = Experiment.id(expId)
        call.user().assertWrite(experiment)

        // check reqStudentIdList
        val courseStudentList = call.course.getAllStudentsInternal(experiment.course.id).associateBy { it.id }
        val studentList = reqStudentIdList?.mapNotNull { courseStudentList[it] } ?: courseStudentList.values.toList()

        // create projects for every student
        CommonScheduler.multiCoroutinesProduceSync(
            studentList.map { student ->
                {
                    call.project.createProjectForUser(
                        student,
                        "exp-$expId-wf-${student.id}",
                        expId,
                        "${experiment.course.name}-${experiment.name}",
                        "${experiment.course.name}-${experiment.name}的实验项目",
                    )
                }
            }
        )

        val resourcePoolName = "exp-$expId-workflow-${RandomStringUtils.randomAlphanumeric(10).lowercase()}"
        val conf = ExperimentWorkflowConfiguration {
            this.expId = expId
            this.resourcePool = resourcePoolName
            this.configuration = configuration
            this.name = name
            this.studentIdList = studentList.map { it.id }
            this.needSubmit = needSubmit
        }

        mysql.useTransaction {
            mysql.experimentWorkflowConfigurations.add(conf)
            experiment.enableWorkflow = true
            mysql.experiments.update(experiment)

            // create resourcePool
            BusinessKubeClient
                .createResourcePool(
                    resourcePoolName,
                    resource.cpu * studentList.size,
                    resource.memory * studentList.size,
                    conf.id,
                )
                .getOrThrow()

            val resourcePool = ResourcePool {
                this.name = resourcePoolName
                this.ownerId = experiment.course.teacher.id
            }
            if (!mysql.resourcePools.exists { it.name.eq(resourcePoolName) }) {
                mysql.resourcePools.add(resourcePool)
            }
        }

        return conf
    }

    companion object : IService.Caller<ExperimentService>() {
        private const val bucket = "exp-resource"
        private val fileManager by lazy { FileManager.buildFileManager("local", bucket) }
    }

    override fun manager(): FileManager {
        return fileManager
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
        // 删除旧的实验资源record
        mysql.delete(CourseResources) { it.expId.eq(experiment.id) }
        mysql.courseResources.add(CourseResource {
            this.courseId = experiment.course.id
            this.expId = experiment.id
            this.file = file
        })
    }
}

fun Experiment.Companion.id(id: Int): Experiment {
    return mysql.experiments.find { it.id eq id }
        ?: throw BusinessException("find experiment($id) from database error")
}

val Experiment.resourceFile: File?
    get() {
        val resource = mysql.courseResources.find { it.expId eq this.id }
        return resource?.file
    }

internal object ExperimentWorkflowContextFileDecorator : FileService.FileDecorator {
    private const val storePath = "exp-workflow-context"
    override fun manager(): FileManager {
        return FileManager.buildFileManager("local", storePath)
    }

    override fun fixName(originalName: String?, ownerId: String, involvedId: Int): Pair<String, String> {
        val fileExt = originalName?.getFileExtension() ?: ""
        val expName = Experiment.id(involvedId).name.filterNot { it.isWhitespace() }
        val name = "${ownerId}-${expName}-workflow-${UUID.randomUUID()}.$fileExt"
        val storeName = "exp-${involvedId}/$name"
        return Pair(name, storeName)
    }

    override fun checkPermission(ownerId: String, involvedId: Int): Boolean {
        return User.id(ownerId).authRead(Experiment.id(involvedId))
    }

    override fun storePath(): String {
        return storePath
    }
}
