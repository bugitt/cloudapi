package cn.edu.buaa.scs.task

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
    }

    val routineList: List<RoutineTask>

    fun run() {
        Thread {
            runBlocking {
                routineList.forEach {
                    launch { it.action() }
                }
            }
        }.start()
    }
}