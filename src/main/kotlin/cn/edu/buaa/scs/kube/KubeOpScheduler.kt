package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.utils.schedule.Event
import cn.edu.buaa.scs.utils.schedule.FanInScheduler

data class KubeOp(
    // meta
    val userId: String,
    val callId: String
) : Event {
    override fun getMeta(): String {
        TODO("Not yet implemented")
    }
}

object KubeOpScheduler : FanInScheduler<KubeOp>("kube-op-scheduler") {

    override suspend fun work(event: KubeOp) {
        TODO("Not yet implemented")
    }

}