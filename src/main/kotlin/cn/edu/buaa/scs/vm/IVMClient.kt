package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.VirtualMachines
import cn.edu.buaa.scs.model.virtualMachines
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.exists
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.schema.ColumnDeclaring

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

    suspend fun deleteVM(uuid: String): Result<Unit>
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
) {
    internal fun existInDb() = mysql.virtualMachines.exists(existPredicate())

    internal fun existPredicate(): (VirtualMachines) -> ColumnDeclaring<Boolean> {
        return {
            it.name.eq(this.name)
                .and(it.studentId.eq(this.studentId))
                .and(it.teacherId.eq(this.teacherId))
                .and(it.experimentId.eq(this.experimentId))
                .and(it.applyId.eq(this.applyId))
        }
    }
}