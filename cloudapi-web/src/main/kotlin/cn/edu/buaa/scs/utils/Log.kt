package cn.edu.buaa.scs.utils

import mu.KLogger
import mu.KotlinLogging

fun logger(name: String = "scs-common"): () -> KLogger {
    // 这里不用关心并发问题
    // 即使并发出了问题也没啥
    val loggerMap = mutableMapOf(
        "scs-common" to KotlinLogging.logger {}
    )
    return fun(): KLogger =
        loggerMap.getOrPut(name) { KotlinLogging.logger(name) }
}