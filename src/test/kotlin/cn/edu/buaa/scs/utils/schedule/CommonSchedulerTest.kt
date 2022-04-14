package cn.edu.buaa.scs.utils.schedule

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class CommonSchedulerTest {

    @Test
    fun multiCoroutinesProduce() {
        val max = 10000
        val action: (Int) -> Int = { it * it }
        val list = runBlocking {
            CommonScheduler.multiCoroutinesProduceSync(
                (0..max).map { { action(it) } },
                Dispatchers.IO
            )
        }
        assert(list.size == max + 1)
        (0..max).forEach { assert(list.contains(action(it))) }
    }

    @Test
    fun multiCoroutinesProduceReturnUnit() {
        val max = 10000
        val action: (Int) -> Unit = { println(it) }
        runBlocking {
            CommonScheduler.multiCoroutinesProduceSync(
                (0..max).map { { action(it) } },
                Dispatchers.IO
            )
        }
    }

    @Test
    fun multiCoroutinesProduceUseActionFunction() {
        val max = 10000
        val action: (Int) -> Int = { it * it }
        val list = runBlocking {
            CommonScheduler.multiCoroutinesProduceSync(List(max + 1) { it }) { it * it }
        }
        assert(list.size == max + 1)
        (0..max).forEach { assert(list.contains(action(it))) }
    }
}