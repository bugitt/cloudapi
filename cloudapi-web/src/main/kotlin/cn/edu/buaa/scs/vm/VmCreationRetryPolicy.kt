package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.error.RemoteServiceException
import java.util.concurrent.ConcurrentHashMap

internal const val DEFAULT_VM_CREATION_RETRY_DELAY_MILLIS = 10_000L

internal class VmCreationRetryPolicy(private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000L }) {
    private val nextAttempts = ConcurrentHashMap<String, Long>()

    fun remainingDelayMillis(resourceId: String): Long {
        val nextAttempt = nextAttempts[resourceId] ?: return 0L
        val remaining = (nextAttempt - nowMillis()).coerceAtLeast(0L)
        if (remaining == 0L) nextAttempts.remove(resourceId, nextAttempt)
        return remaining
    }

    fun recordFailure(resourceId: String, platform: String, failure: Throwable): Long {
        val delay = vmCreationRetryDelayMillis(platform, failure)
        nextAttempts[resourceId] = nowMillis() + delay
        return delay
    }

    fun clear(resourceId: String) {
        nextAttempts.remove(resourceId)
    }
}

internal fun vmCreationRetryDelayMillis(platform: String, failure: Throwable): Long =
    if (platform.equals("vcenter", ignoreCase = true) && failure is RemoteServiceException && failure.status == 409) {
        180_000L
    } else {
        DEFAULT_VM_CREATION_RETRY_DELAY_MILLIS
    }
