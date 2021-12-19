package cn.edu.buaa.scs.kube

import io.fabric8.kubernetes.api.model.apps.DaemonSet
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.api.model.batch.v1.Job

sealed interface Controller {

}

class DeploymentController(deployment: Deployment) : Controller {

}

class StatefulSetController(statefulSet: StatefulSet) : Controller {

}

class DaemonSetController(daemonSet: DaemonSet) : Controller {

}

class JobController(job: Job) : Controller {

}

enum class ControllerType {
    DEPLOYMENT,
    STATEFUL,
    DAEMON,
    JOB
}