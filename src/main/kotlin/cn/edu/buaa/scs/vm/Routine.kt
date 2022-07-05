package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.model.Experiment
import cn.edu.buaa.scs.model.vmApplyList
import cn.edu.buaa.scs.service.id
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.greater
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList

object Routine {
    private val routineList = listOf<suspend () -> Unit>(
        ::checkAndApplyVmCreation,
        // add more routine
    )

    fun run() {
        Thread {
            runBlocking {
                routineList.forEach { launch { it() } }
            }
        }.start()
    }
}

suspend fun checkAndApplyVmCreation() {
    withContext(Dispatchers.IO) {
        while (true) {
            try {
                mysql.vmApplyList.filter {
                    it.status.eq(1)
                        .and(it.exceptedNum.greater(it.actualNum))
                }
                    .toList()
                    .sortedBy { it.applyTime }
                    .map { vmApply ->
                        val baseOptions = CreateVmOptions(
                            name = vmApply.namePrefix,
                            templateUuid = vmApply.templateUuid,
                            memory = vmApply.memory,
                            cpu = vmApply.cpu,
                            diskSize = vmApply.diskSize,
                            applyId = vmApply.id,
                        )
                        when {
                            vmApply.studentId.isNotBlank() && vmApply.studentId != "default" ->
                                listOf(
                                    baseOptions.copy(
                                        name = "${vmApply.namePrefix}-${vmApply.studentId}",
                                        studentId = vmApply.studentId
                                    )
                                )
                            vmApply.teacherId.isNotBlank() && vmApply.teacherId != "default" ->
                                listOf(
                                    baseOptions.copy(
                                        name = "${vmApply.namePrefix}-${vmApply.teacherId}",
                                        teacherId = vmApply.teacherId
                                    )
                                )
                            vmApply.experimentId != 0 -> {
                                val experiment = Experiment.id(vmApply.experimentId)
                                vmApply.studentIdList.map { studentId ->
                                    baseOptions.copy(
                                        name = "${vmApply.namePrefix}-$studentId",
                                        studentId = studentId,
                                        teacherId = experiment.course.teacher.id,
                                        isExperimental = true,
                                        experimentId = experiment.id
                                    )
                                }
                            }
                            else -> listOf<CreateVmOptions>()
                        }
                    }
                    .flatten()
                    .filter { !it.existInDb() }
                    .forEach { options ->
                        vmClient.createVM(options).getOrThrow()
                        mysql.vmApplyList.find { it.id.eq(options.applyId) }?.let { vmApply ->
                            vmApply.actualNum += 1
                            vmApply.flushChanges()
                        }
                    }

            } catch (e: Throwable) {
                logger("vm-routine-check-and-apply-vm-creation")().error { e.stackTraceToString() }
            }
        }
    }
}