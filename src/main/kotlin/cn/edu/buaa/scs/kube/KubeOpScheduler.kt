package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.utils.schedule.Event
import cn.edu.buaa.scs.utils.schedule.FanInScheduler

sealed class KubeOp(
    // meta
    val userId: String,
    val callId: String
) : Event {
    override fun getMeta() = "userId: $userId, callId: $callId"
}

class KubeDeployOp(
    userId: String,
    callId: String,
    val buildOption: BuildOption,
    val deployOption: DeployOption
) : KubeOp(userId, callId)

object KubeOpScheduler : FanInScheduler<KubeOp>("kube-op-scheduler") {

    override suspend fun work(event: KubeOp) =
        when (event) {
            is KubeDeployOp -> deploy(event)
        }

    private suspend fun deploy(op: KubeDeployOp) {
        Kube.syncDeploy(op.deployOption)
    }
}