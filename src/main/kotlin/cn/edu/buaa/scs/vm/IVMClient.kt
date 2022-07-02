package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.model.VirtualMachine

interface IVMClient {
    suspend fun getAllVMs(): Result<List<VirtualMachine>>

    suspend fun getVM(uuid: String): Result<VirtualMachine>

    suspend fun powerOnSync(uuid: String): Result<Unit>

    suspend fun powerOnAsync(uuid: String)

    suspend fun powerOffSync(uuid: String): Result<Unit>

    suspend fun powerOffAsync(uuid: String)

    // TODO: 添加更多配置项
    suspend fun configVM(uuid: String, experimentId: Int?): Result<Unit>

    suspend fun createVM(options: CreateVmOptions): Result<VirtualMachine>
}

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