package cn.edu.buaa.scs.utils.schedule

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList

object CommonScheduler {
    private val defaultDispatcher = Dispatchers.Default

    suspend fun <T> multiCoroutinesProduceSync(
        actionList: List<suspend () -> T>,
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): List<T> =
        withContext(defaultDispatcher) {
            val output = Channel<T>(actionList.size)
            val jobs = mutableListOf<Job>()
            actionList.forEach { action ->
                jobs += launch(dispatcher) {
                    output.send(action.invoke())
                }
            }
            jobs.forEach { it.join() }
            output.close()
            output.toList()
        }
}