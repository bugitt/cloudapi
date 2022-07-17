package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.vm.vcenter.VCenterClient
import io.ktor.application.*

lateinit var vmClient: IVMClient

@Suppress("unused")
fun Application.vmModule() {
    VCenterClient.initialize(this)
    vmClient = VCenterClient
    VMRoutine.run()
}