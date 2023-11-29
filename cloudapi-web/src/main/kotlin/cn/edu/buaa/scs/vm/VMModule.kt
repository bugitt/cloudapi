package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.utils.getConfigString
import cn.edu.buaa.scs.vm.sangfor.SangforClient
import cn.edu.buaa.scs.vm.vcenter.VCenterClient
import io.ktor.server.application.*

//lateinit var vmClient: IVMClient
lateinit var sfClient: SangforClient

@Suppress("unused")
fun Application.vmModule() {
    sshConfig = SSH.initSSHConfig(
        privateKey = getConfigString("vm.ssh.privateKey"),
        username = getConfigString("vm.ssh.username"),
    )

//    vmClient = VCenterClient
    sfClient = SangforClient
    VMRoutine.run()
}
