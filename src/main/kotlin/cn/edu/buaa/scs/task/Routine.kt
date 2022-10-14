package cn.edu.buaa.scs.task

import cn.edu.buaa.scs.application
import cn.edu.buaa.scs.utils.getConfigList
import cn.edu.buaa.scs.utils.logger
import kotlinx.coroutines.*

data class RoutineTask(
    val name: String,
    val action: suspend () -> Unit
)

interface Routine {
    companion object {
        fun alwaysDo(
            name: String,
            dispatcher: CoroutineDispatcher = Dispatchers.IO,
            detention: Long = 0L,
            preAction: suspend () -> Unit = {},
            loopAction: suspend () -> Unit,
        ): RoutineTask {
            return RoutineTask(name) {
                preAction()
                withContext(dispatcher) {
                    while (true) {
                        try {
                            loopAction()
                        } catch (e: Throwable) {
                            logger(name)().error { e.stackTraceToString() }
                        }
                        delay(detention)
                    }
                }
            }
        }

        fun alwaysDoInPool(
            name: String,
            poolSize: Int = 100,
            poolBufferSize: Int = 100000,
            poolDispatcher: CoroutineDispatcher = Dispatchers.Default,
            dispatcher: CoroutineDispatcher = Dispatchers.IO,
            detention: Long = 0L,
            preAction: suspend () -> Unit = {},
            loopAction: suspend (pool: Task.TaskExecutorPool) -> Unit,
        ): RoutineTask {
            val pool = Task.TaskExecutorPool(name, poolSize, poolBufferSize, poolDispatcher)
            return alwaysDo(name, dispatcher, detention, { pool.start(); preAction() }, { loopAction(pool) })
        }
    }

    val routineList: List<RoutineTask>

    fun run() {
        Thread {
            val shouldRunTaskList = application.getConfigList("routine.taskList").toSet()
            runBlocking {
                routineList
                    .filter { shouldRunTaskList.contains(it.name) }
                    .forEach {
                        launch { it.action() }
                    }
            }
        }.start()
    }
}