package cn.edu.buaa.scs.utils.schedule

import cn.edu.buaa.scs.utils.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

interface Event {
    fun getMeta(): String
}

/**
 * FanInScheduler 从多个协程接收任务, 然后调度给其他协程执行
 */
abstract class FanInScheduler<T : Event>(val name: String) {
    private val logger = logger(name)()
    private val defaultBuffer = 100
    protected open val channel = Channel<T>(defaultBuffer)
    protected open val coroutineDispatcher = Dispatchers.Default

    fun launch() = run {
        Thread { dispatch() }.start()
    }

    private fun dispatch() = runBlocking {
        for (event in channel) {
            logger.info { "dispatch event: ${event.getMeta()}" }
            launch(coroutineDispatcher) {
                work(event)
            }
        }
    }

    suspend fun sendEvent(e: T) = run { channel.send(e) }

    /**
     * 具体处理每个 event 的业务逻辑
     */
    abstract suspend fun work(event: T)
}
