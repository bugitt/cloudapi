package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.model.VirtualMachine

data class CreateVmOptions(
    val name: String,
    val templateUuid: String,

    // course related
    val adminId: String = "default",
    val studentId: String = "default",
    val teacherId: String = "default",
    val isExperimental: Boolean = false,
    val experimentId: Int = 0,
    val applyId: String,

    val memory: Int, // MB
    val cpu: Int,
    val disNum: Int = 1,
    val diskSize: Long, // bytes

    val powerOn: Boolean = false,
)

data class ConfigVmOptions(
    val vm: VirtualMachine,
    val experimentId: Int?,
    val adminId: String?,
    val teacherId: String?,
    val studentId: String?,
)
