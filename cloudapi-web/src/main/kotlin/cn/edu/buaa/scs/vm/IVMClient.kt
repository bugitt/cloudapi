package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.model.Host
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.vm.sangfor.SangforClient
import cn.edu.buaa.scs.vm.vcenter.VCenterClient

interface IVMClient {
    suspend fun getHosts(): Result<List<Host>>

    suspend fun getAllVMs(): Result<List<VirtualMachine>>

    suspend fun getVM(uuid: String): Result<VirtualMachine>

    suspend fun getVMByName(name: String, applyId: String): Result<VirtualMachine>

    suspend fun powerOnSync(uuid: String): Result<Unit>

    suspend fun powerOnAsync(uuid: String)

    suspend fun powerOffSync(uuid: String): Result<Unit>

    suspend fun powerOffAsync(uuid: String)

    // TODO: 添加更多配置项
    suspend fun configVM(
        uuid: String,
        experimentId: Int? = null,
        adminId: String? = null,
        teacherId: String? = null,
        studentId: String? = null,
    ): Result<VirtualMachine>

    suspend fun createVM(options: CreateVmOptions): Result<VirtualMachine>

    suspend fun deleteVM(uuid: String): Result<Unit>

    suspend fun convertVMToTemplate(uuid: String): Result<VirtualMachine>
}

fun newVMClient(platform: String): IVMClient {
    return when (platform.lowercase()) {
        "vcenter" -> VCenterClient
        "sangfor" -> SangforClient
        else -> throw Exception("unknown platform: $platform")
    }
}
