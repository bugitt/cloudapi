package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertAdmin
import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.authWrite
import cn.edu.buaa.scs.controller.models.PatchPeerAppealRequest
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.getOrPut
import cn.edu.buaa.scs.utils.schedule.CommonScheduler
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.userId
import io.ktor.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.schema.ColumnDeclaring
import java.util.concurrent.ConcurrentHashMap

val ApplicationCall.peer get() = PeerService.getSvc(this) { PeerService(this) }

class PeerService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<PeerService>() {
        val enableServiceMutexMap: MutableMap<Int, Mutex> = ConcurrentHashMap()
        val studentPeerTaskMutexMap: MutableMap<String, Mutex> = ConcurrentHashMap()
    }

    data class AssessmentInfo(
        val assignmentId: Int,
        val assessorId: String,
        val assessorName: String,
        val assessedTime: Long,
        val score: Double,
        val reason: String
    )

    data class PeerTaskWithFile(
        val assignmentId: Int,
        val file: File,
        val peerTask: PeerTask
    )

    data class PeerAssessmentResult(
        val assignmentId: Int,
        val finalScore: Double?,
        val peerScore: Double?,
        val peerInfoList: List<PeerTask>,
        val isAdmin: Boolean,
    )

    suspend fun createOrUpdate(assignmentId: Int, score: Double, reason: String? = null): AssessmentInfo {
        val assignment = Assignment.id(assignmentId)
        val experiment = Experiment.id(assignment.experimentId)
        val isAdmin = call.user().isAdmin() || call.user().isCourseAdmin(experiment.course)
        if (score < 0 || score > 100) {
            throw BadRequestException("score must be between 0 and 100")
        }
        if (assignment.file == null) {
            // 如果没有文件，不能评分
            throw BadRequestException("Assignment has no file")
        }
        return if (isAdmin)
            adminCreateOrUpdate(experiment, assignment, score)
        else
            nonAdminCreateOrUpdate(experiment, assignment, score, reason)
    }

    fun getTasks(expId: Int): List<PeerTaskWithFile> {
        val tasks = mysql.peerTasks.filter { it.expId.eq(expId) and it.assessorId.eq(call.userId()) }.toList()
        val assignments = mysql.assignments.filter { it.id.inList(tasks.map { t -> t.assignmentId }) }.toList()
        val taskMap = tasks.associateBy { it.assignmentId }
        return assignments.map { assignment ->
            PeerTaskWithFile(
                assignment.id,
                assignment.file as File,
                taskMap[assignment.id] as PeerTask
            )
        }
    }

    fun getResult(assignmentId: Int): PeerAssessmentResult {
        val assignment = Assignment.id(assignmentId)
        call.user().assertRead(assignment)
        val tasks = mysql.peerTasks.filter { it.assignmentId.eq(assignmentId) }.toList()
        return PeerAssessmentResult(
            assignment.id,
            finalScore = assignment.finalScore.toDouble(),
            peerScore = assignment.peerScore,
            tasks,
            call.userId() != assignment.studentId
        )
    }

    /**
     * 正式开启互评
     */
    suspend fun enable(expId: Int) {
        val mutex = enableServiceMutexMap.getOrPut(expId) { Mutex() }
        mutex.lock()
        val experiment = Experiment.id(expId)
        call.user().authWrite(experiment)
        // 检查是否开启互评了
        if (experiment.peerAssessmentStart) {
            return
        }
        // 检查是不是已经有8个标准作业了
        val peerStandardList =
            mysql.peerStands.filter { it.expId.eq(expId) and it.isCompleted.eq(true) }.toList()
        if (peerStandardList.size < 8) {
            throw BadRequestException("互评作业的标准作业数量小于8，无法开启互评")
        }

        val buildPeerTask: BatchInsertStatementBuilder<PeerTasks>.(User, Assignment, Boolean) -> Unit =
            { assessor, assignment, isStandard ->
                item {
                    set(it.studentId, assignment.studentId)
                    set(it.assignmentId, assignment.id)
                    set(it.expId, assignment.experimentId)
                    set(it.assessorId, assessor.id)
                    set(it.assessorName, assessor.name)
                    set(it.isStandard, isStandard)
                    set(it.status, 0)
                }
            }

        // 选择分数居中的四个, 设置为真正的标准作业
        val middlePeerStands = peerStandardList.sortedBy { it.score }.slice(2..5)
        val standardAssignments =
            mysql.assignments.filter { it.id.inList(middlePeerStands.map { p -> p.assignmentId }) }.toList()

        mysql.useTransaction {
            // 删掉没用的标准作业
            mysql.delete(PeerStandards) {
                it.expId.eq(expId) and it.id.notInList(middlePeerStands.map { p -> p.id })
            }

            val assignments = mysql.assignments.filter {
                it.expId.eq(expId) and
                        it.fileId.isNotNull() and it.fileId.notEq(0)
            }
                .toList()
                .filterNot { middlePeerStands.map { pa -> pa.assignmentId }.contains(it.id) }
                .shuffled()

            val userMap = call.course.getAllStudents(experiment.course.id).associateBy { it.id }

            val len = assignments.size
            val batchInsertActionList: MutableList<suspend () -> IntArray> = mutableListOf()
            for (i in 0 until len) {
                val action = suspend {
                    mysql.batchInsert(PeerTasks) {
                        // 每个学生选取其后的三个人的作业作为自己的互评任务
                        val assessor = userMap[assignments[i].studentId] as User
                        for (j in 1 until 4) {
                            val target = (i + j) % len
                            this.buildPeerTask(assessor, assignments[target], false)
                        }
                        // 除此之外，还应该为该学生添加一个标准作业
                        this.buildPeerTask(assessor, standardAssignments.random(), true)
                    }
                }
                batchInsertActionList.add(action)
            }
            // 特别地，还需要为标准作业的学生分配互评任务
            val standardLen = standardAssignments.size
            for (i in 0 until standardLen) {
                val assessor = userMap[standardAssignments[i].studentId] as User
                val action = suspend {
                    val targetAssignmentList = assignments.shuffled().take(3)
                    mysql.batchInsert(PeerTasks) {
                        targetAssignmentList.forEach { targetAssignment ->
                            this.buildPeerTask(assessor, targetAssignment, false)
                        }
                        // 然后, 还需要分配一个标准作业
                        this.buildPeerTask(assessor, standardAssignments[(i + 1) % standardLen], true)
                    }
                }
                batchInsertActionList.add(action)
            }
            CommonScheduler.multiCoroutinesProduceSync(batchInsertActionList, Dispatchers.IO)

            experiment.peerAssessmentStart = true
            mysql.experiments.update(experiment)
        }
        mutex.unlock()
    }

    private fun adminCreateOrUpdate(
        experiment: Experiment,
        assignment: Assignment,
        score: Double
    ): AssessmentInfo {
        if (experiment.peerAssessmentStart) {
            // 互评已经开始，管理员不能评分了
            throw BadRequestException("peer assessment has started")
        }
        val task = mysql.peerStands.find { it.assignmentId.eq(assignment.id) }
            ?: throw BadRequestException("assignment should be marked 'mayStandard' to be assessed")
        task.assessorName = call.user().name
        task.assessorId = call.userId()
        task.score = score
        task.createdAt = System.currentTimeMillis()
        task.isCompleted = true
        mysql.peerStands.update(task)
        return AssessmentInfo(assignment.id, call.userId(), call.user().name, task.createdAt!!, task.score!!, "")
    }

    private suspend fun nonAdminCreateOrUpdate(
        experiment: Experiment,
        assignment: Assignment,
        score: Double,
        srcReason: String? = null
    ): AssessmentInfo {
        val mutex = studentPeerTaskMutexMap.getOrPut(call.userId()) { Mutex() }
        mutex.lock()
        if (!experiment.peerAssessmentStart) {
            // 互评还没开始，此时不能评分
            throw BadRequestException("Peer assessment has not started")
        }
        val task = mysql.peerTasks.find {
            it.assessorId.eq(call.user().id) and
                    it.assignmentId.eq(assignment.id)
        } ?: throw BadRequestException("Assignment is not allowed to be assessed by you")

        val reason = srcReason
            ?: throw BadRequestException("必须给出评分理由")

        mysql.useTransaction {
            task.originalScore = score
            task.adjustedScore = if (task.isStandard) score else null
            task.reason = reason
            task.createdAt = System.currentTimeMillis()
            task.status = if (task.isStandard) 2 else 1
            mysql.peerTasks.update(task)

            // 进行善后工作
            // 如果都评分完成，修正之前左右的评分
            val tasks =
                mysql.peerTasks.filter { it.expId.eq(experiment.id) and (it.status.notEq(0)) }.toList()

            if (tasks.size < 4) {
                // 评分还没完成，没必要调整
                return@useTransaction
            }
            // 否则，需要调整之前所有的分数
            val standardTask = tasks.find { it.isStandard } as PeerTask
            val standardScore =
                (mysql.peerStands.find { it.assignmentId.eq(standardTask.assignmentId) } as PeerStandard).score
            tasks.forEach { task ->
                val adjustedScore = adjustScore(task.originalScore!!, standardTask.originalScore!!, standardScore!!)
                task.adjustedScore = adjustedScore
                task.status = 2
                mysql.peerTasks.update(task)
            }

            // 检查涉及到的作业是不是都已经评分完成了，如果完成的话，更新该作业的分数
            val updateAssignmentActionList = tasks.map { task ->
                suspend {
                    if (mysql.peerTasks.none { it.assignmentId.eq(task.assignmentId) and it.status.less(2) }) {
                        // 说明该作业的评分都搞完了，算一下平均值
                        val peerScore =
                            mysql.peerTasks.filter { it.assignmentId.eq(task.assignmentId) }
                                .averageBy { it.adjustedScore }
                        mysql.update(Assignments) {
                            set(it.peerScore, peerScore)
                            set(it.peerCompleted, peerScore != null)
                            where { it.id.eq(task.assignmentId) }
                        }
                    }
                }
            }
            CommonScheduler.multiCoroutinesProduceSync(updateAssignmentActionList, Dispatchers.IO)
        }
        mutex.unlock()
        return AssessmentInfo(
            assignment.id,
            call.userId(),
            call.user().name,
            task.createdAt!!,
            task.originalScore!!,
            reason
        )
    }

    private fun adjustScore(originScore: Double, selfStandardScore: Double, standardScore: Double): Double {
        val rate = if (selfStandardScore > standardScore) {
            -0.05
        } else if (selfStandardScore < standardScore) {
            0.05
        } else {
            0.0
        }
        val score = originScore * (1.0 + rate)
        return if (score > 100) 100.0 else if (score < 0) 0.0 else score
    }

    fun createAppeal(expId: Int, content: String): PeerAppeal {
        val peerAppeal = PeerAppeal {
            this.expId = expId
            this.studentId = call.userId()
            this.content = content
            this.appealedAt = System.currentTimeMillis()
            this.processStatus = 0
        }
        call.user().assertAdmin(peerAppeal)
        mysql.peerAppeals.add(peerAppeal)
        return peerAppeal
    }

    fun getAllAppeal(expId: Int, studentId: String?): List<PeerAppeal> {
        val course = Experiment.id(expId).course
        val isAdmin = call.user().isCourseAdmin(course)
        val stuId = if (!isAdmin) call.userId() else studentId
        val condition: (PeerAppeals) -> ColumnDeclaring<Boolean> = stuId?.let { id ->
            { it.expId.eq(expId) and it.studentId.eq(id) }
        } ?: { it.expId.eq(expId) }
        return mysql.peerAppeals.filter(condition).toList().sortedBy { it.appealedAt }
    }

    fun getAppeal(id: Int): PeerAppeal {
        val appeal = mysql.peerAppeals.find { it.id.eq(id) }
            ?: throw NotFoundException("找不到该申诉")
        call.user().assertRead(appeal)
        return appeal
    }

    fun updateAppeal(appealId: Int, req: PatchPeerAppealRequest): PeerAppeal {
        val appeal = mysql.peerAppeals.find { it.id.eq(appealId) }
            ?: throw NotFoundException("找不到该申诉")
        return if (req.content != null) {
            patchAppeal(appeal, req.content)
        } else {
            processAppeal(
                appeal,
                req.processStatus ?: throw BadRequestException("invalid processStatus"),
                req.processContent
            )
        }
    }

    /**
     * 修改申诉的内容
     */
    private fun patchAppeal(appeal: PeerAppeal, content: String): PeerAppeal {
        if (appeal.studentId != call.userId() && !call.user().isAdmin()) {
            throw AuthorizationException("您没有权限修改该申诉")
        }
        appeal.content = content
        mysql.peerAppeals.update(appeal)
        return appeal
    }

    /**
     * 处理申诉
     */
    private fun processAppeal(appeal: PeerAppeal, processStatus: Int, processContent: String?): PeerAppeal {
        val experiment = Experiment.id(appeal.expId)
        if (!call.user().isCourseAdmin(experiment.course)) {
            throw AuthorizationException("您没有权限处理该申诉")
        }
        appeal.processStatus = processStatus
        appeal.processContent = processContent ?: ""
        appeal.processedAt = System.currentTimeMillis()
        appeal.processorId = call.userId()
        appeal.processorName = call.user().name
        mysql.peerAppeals.update(appeal)
        return appeal
    }

    /**
     * 删除申诉
     */
    fun deleteAppeal(appealId: Int) {
        val appeal = mysql.peerAppeals.find { it.id.eq(appealId) }
            ?: throw NotFoundException("找不到该申诉")
        if (appeal.studentId != call.userId() && !call.user().isAdmin()) {
            throw AuthorizationException("您没有权限删除该申诉")
        }
        mysql.delete(PeerAppeals) { it.id.eq(appealId) }
    }
}

fun PeerAppeal.Companion.id(pid: Int): PeerAppeal {
    return mysql.peerAppeals.find { it.id eq pid }
        ?: throw BusinessException("find peerAppeal($pid) from database error")
}