package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.VirtualMachine

interface IVMClient {
    suspend fun getAllVMs(): Result<List<VirtualMachine>>

    @Throws(NotFoundException::class)
    suspend fun getVM(uuid: String): Result<VirtualMachine>

    suspend fun powerOnSync(uuid: String): Result<Unit>

    @Throws(NotFoundException::class)
    suspend fun powerOnAsync(uuid: String)

    suspend fun powerOffSync(uuid: String): Result<Unit>

    @Throws(NotFoundException::class)
    suspend fun powerOffAsync(uuid: String)
}