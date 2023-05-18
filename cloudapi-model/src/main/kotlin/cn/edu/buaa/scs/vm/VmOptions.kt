package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.VirtualMachineExtraInfo
import com.fasterxml.jackson.annotation.JsonProperty

data class CreateVmOptions(
    @JsonProperty("name") val name: String,

    // course related
    @JsonProperty("extraInfo") val extraInfo: VirtualMachineExtraInfo,

    @JsonProperty("memory") val memory: Int, // MB
    @JsonProperty("cpu") val cpu: Int,
    @JsonProperty("diskNum") val disNum: Int = 1,
    @JsonProperty("diskSize") val diskSize: Long, // Bytes

    @JsonProperty("powerOn") val powerOn: Boolean = false,

    @JsonProperty("hostId") val hostId: String = "",
)

data class ConfigVmOptions(
    val vm: VirtualMachine,
    val experimentId: Int?,
    val adminId: String?,
    val teacherId: String?,
    val studentId: String?,
)
