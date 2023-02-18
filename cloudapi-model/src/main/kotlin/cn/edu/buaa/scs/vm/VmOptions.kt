package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.model.VirtualMachine
import com.fasterxml.jackson.annotation.JsonProperty

data class CreateVmOptions(
    @JsonProperty("name") val name: String,
    @JsonProperty("templateUuid") val templateUuid: String,

    // course related
    @JsonProperty("adminId") val adminId: String = "default",
    @JsonProperty("studentId") val studentId: String = "default",
    @JsonProperty("teacherId") val teacherId: String = "default",
    @JsonProperty("experimental") val experimental: Boolean = false,
    @JsonProperty("experimentId") val experimentId: Int = 0,
    @JsonProperty("applyId") val applyId: String,

    @JsonProperty("memory") val memory: Int, // MB
    @JsonProperty("cpu") val cpu: Int,
    @JsonProperty("diskNum") val disNum: Int = 1,
    @JsonProperty("diskSize") val diskSize: Long, // bytes

    @JsonProperty("powerOn") val powerOn: Boolean = false,
)

data class ConfigVmOptions(
    val vm: VirtualMachine,
    val experimentId: Int?,
    val adminId: String?,
    val teacherId: String?,
    val studentId: String?,
)
