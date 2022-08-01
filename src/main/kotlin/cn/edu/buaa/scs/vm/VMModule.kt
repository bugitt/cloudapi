package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.utils.getConfigString
import cn.edu.buaa.scs.vm.vcenter.VCenterClient
import io.ktor.application.*

lateinit var vmClient: IVMClient

@Suppress("unused")
fun Application.vmModule() {
    sshConfig = SSH.initSSHConfig(
        privateKey = getConfigString("vm.ssh.privateKey"),
        username = getConfigString("vm.ssh.username"),
    )

    VCenterClient.initialize(this)
    vmClient = VCenterClient
    VMRoutine.run()
}