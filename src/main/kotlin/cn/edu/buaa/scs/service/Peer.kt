package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.authWrite
import cn.edu.buaa.scs.error.BadRequestException
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
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList
import org.ktorm.entity.update
import java.util.concurrent.ConcurrentHashMap

val ApplicationCall.peer get() = PeerService.getSvc(this) { PeerService(this) }

class PeerService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<PeerService>() {
        val enableServiceMutexMap: MutableMap<Int, Mutex> = ConcurrentHashMap()
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

    fun createOrUpdate(assignmentId: Int, score: Double, reason: String? = null): AssessmentInfo {
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

    private fun nonAdminCreateOrUpdate(
        experiment: Experiment,
        assignment: Assignment,
        score: Double,
        srcReason: String? = null
    ): AssessmentInfo {
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
        }
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
}