package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.VirtualMachineExtraInfo

fun VirtualMachine.applyExtraInfo(extraInfo: VirtualMachineExtraInfo) {
    this.adminId = extraInfo.adminId
    this.studentId = extraInfo.studentId
    this.teacherId = extraInfo.teacherId
    this.isExperimental = extraInfo.isExperimental
    this.experimentId = extraInfo.experimentId
    this.applyId = extraInfo.applyId
}