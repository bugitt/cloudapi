package cn.edu.buaa.scs.task

import cn.edu.buaa.scs.utils.logger
import kotlinx.coroutines.*

interface Routine {
    companion object {
        fun alwaysDo(
            name: String,
            dispatcher: CoroutineDispatcher = Dispatchers.IO,
            detention: Long = 0L,
            action: suspend () -> Unit
        ): suspend () -> Unit {
            return suspend {
                withContext(dispatcher) {
                    while (true) {
                        try {
                            action()
                        } catch (e: Throwable) {
                            logger(name)().error { e.stackTraceToString() }
                        }
                        delay(detention)
                    }
                }
            }
        }
    }

    val routineList: List<suspend () -> Unit>

    fun run() {
        Thread {
            runBlocking {
                routineList.forEach { launch { it() } }
            }
        }.start()
    }
}