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
            CommonScheduler.multiCoroutinesProduce(
                (0..max).map { { action(it) } },
                Dispatchers.IO
            )
        }
        assert(list.size == max + 1)
        (0..max).forEach { assert(list.contains(action(it))) }
    }
}