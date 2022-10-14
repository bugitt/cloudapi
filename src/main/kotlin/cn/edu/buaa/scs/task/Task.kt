package cn.edu.buaa.scs.task

import cn.edu.buaa.scs.model.TaskData
import cn.edu.buaa.scs.model.taskDataList
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.ktorm.entity.update

abstract class Task(protected val taskData: TaskData) {
    enum class Status {
        UNDO, DOING, SUCCESS, FAIL
    }

    enum class Type {
        VirtualMachine, ImageBuild, ContainerService
    }

    protected abstract suspend fun internalProcess(): Result<Unit>

    suspend fun process(): Result<Unit> {
        return try {
            taskData.doing()
            internalProcess().getOrThrow()
            taskData.success()
            Result.success(Unit)
        } catch (e: Throwable) {
            taskData.fail(e.stackTraceToString())
            Result.failure(e)
        }
    }

    fun doing(): Result<Unit> =
        try {
            taskData.doing()
            Result.success(Unit)
        } catch (e: Throwable) {
            taskData.fail(e.stackTraceToString())
            Result.failure(e)
        }


    class TaskExecutorPool(
        private val name: String,
        private val poolSize: Int = 100,
        bufferSize: Int = 100000,
        private val dispatcher: CoroutineDispatcher = Dispatchers.Default
    ) {
        private val channel = Channel<Task>(bufferSize)

        suspend fun send(task: Task) {
            if (task.doing().isSuccess) {
                channel.send(task)
            }
        }

        fun start() {
            Thread {
                runBlocking {
                    withContext(dispatcher) {
                        repeat(poolSize) {
                            launch {
                                for (task in channel) {
                                    task.process().exceptionOrNull()?.let { logger(name)().error { it.stackTrace } }
                                }
                            }
                        }
                    }
                }
            }.start()
        }
    }
}

fun TaskData.doing() {
    this.status = Task.Status.DOING
    this.updateTime = System.currentTimeMillis()
    mysql.taskDataList.update(this)
}

fun TaskData.success() {
    this.status = Task.Status.SUCCESS
    this.updateTime = System.currentTimeMillis()
    this.endTime = System.currentTimeMillis()
    mysql.taskDataList.update(this)
}

fun TaskData.fail(errMsg: String) {
    this.status = Task.Status.FAIL
    this.error = errMsg
    this.updateTime = System.currentTimeMillis()
    this.endTime = System.currentTimeMillis()
    mysql.taskDataList.update(this)
}