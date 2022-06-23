package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.model.VirtualMachine

interface IVMClient {
    suspend fun getAllVMs(): Result<List<VirtualMachine>>

    suspend fun getVM(uuid: String): Result<VirtualMachine>

    suspend fun powerOnSync(uuid: String): Result<Unit>

    suspend fun powerOnAsync(uuid: String)

    suspend fun powerOffSync(uuid: String): Result<Unit>

    suspend fun powerOffAsync(uuid: String)
}