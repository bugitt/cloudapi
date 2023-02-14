package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.VirtualMachines
import cn.edu.buaa.scs.model.virtualMachines
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.exists
import cn.edu.buaa.scs.vm.vcenter.VCenterClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import org.ktorm.schema.ColumnDeclaring

interface IVMClient {
    suspend fun getAllVMs(): Result<List<VirtualMachine>>

    suspend fun getVM(uuid: String): Result<VirtualMachine>

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

    suspend fun waitForVMInDB(predicate: (VirtualMachines) -> ColumnDeclaring<Boolean>): Result<VirtualMachine> {
        return try {
            Result.success(withTimeout(500000L) {
                while (!mysql.virtualMachines.exists(predicate)) {
                    delay(10L)
                }
                mysql.virtualMachines.find(predicate)!!
            })
        } catch (e: Throwable) {
            Result.failure(Exception("waitForVMInDB timeout", e))
        }
    }
}

fun newVMClient(platform: String): IVMClient {
    return when (platform.lowercase()) {
        "vcenter" -> VCenterClient
        else -> throw Exception("unknown platform: $platform")
    }
}

internal fun CreateVmOptions.existInDb() = mysql.virtualMachines.exists(existPredicate())

internal fun CreateVmOptions.existPredicate(): (VirtualMachines) -> ColumnDeclaring<Boolean> {
    return {
        it.name.eq(this.name)
            .and(it.studentId.eq(this.studentId))
            .and(it.teacherId.eq(this.teacherId))
            .and(it.experimentId.eq(this.experimentId))
            .and(it.applyId.eq(this.applyId))
    }
}
