package cn.edu.buaa.scs.kube

import io.fabric8.kubernetes.api.model.apps.DaemonSet
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.api.model.batch.v1.Job

sealed interface Workload {

}

class DeploymentWorkload(deployment: Deployment) : Workload {

}

class StatefulSetWorkload(statefulSet: StatefulSet) : Workload {

}

class DaemonSetWorkload(daemonSet: DaemonSet) : Workload {

}

class JobWorkload(job: Job) : Workload {

}