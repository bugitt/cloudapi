package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.model.VirtualMachine

interface IVMClient {
    suspend fun getAllVMs(): List<VirtualMachine>
}