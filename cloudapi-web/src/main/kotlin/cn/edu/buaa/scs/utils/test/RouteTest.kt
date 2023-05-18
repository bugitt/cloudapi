package cn.edu.buaa.scs.utils.test

import cn.edu.buaa.scs.kube.crd.v1alpha1.VirtualMachineSpec
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.VirtualMachineExtraInfo
import cn.edu.buaa.scs.utils.logger
import cn.edu.buaa.scs.vm.CreateVmOptions
import cn.edu.buaa.scs.vm.GeneticAlgorithm
import cn.edu.buaa.scs.vm.PhysicalHost
import cn.edu.buaa.scs.vm.vcenter.VCenterClient
import cn.edu.buaa.scs.vm.vcenter.VCenterClient.getVcenterHostsUsage
import cn.edu.buaa.scs.service.VmService
import cn.edu.buaa.scs.vm.sangfor.SangforClient
import cn.edu.buaa.scs.vm.vcenter.VCenterClient.transferHost
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.test() {
    // just for test
    route("/test") {
        get {
            val logger = logger("UNIT TEST")()
            logger.info { "......TESTING FUNCTIONS......" }


            // 👍深信服平台获取资源占用 getHostsUsage
//            logger.info {"TEST getSangforHostsUsage()"}
//            var usage = getHostsUsage().getOrThrow()
//            logger.info {usage.size}
//            usage.forEach{
//                logger.info { "This VM: IP=" + it["ip"] + ", ID=" + it["hostId"]}
//                logger.info { it["cpu_total_mhz"]+ " " + it["cpu_used_mhz"]+ " " + it["cpu_ratio"] }
//                logger.info { it["memory_total_mb"]+ " " + it["memory_used_mb"]+ " " + it["memory_ratio"] }
//            }

            // 👍深信服平台迁移虚拟机
//            logger.info {"TEST migrateVmToNewHost()"}
//            val vmUuid = "e44a24b3-4c15-4608-ab95-1b16dfb5d880"
//            val hostId = "host-e03676dde96c"
//            val vmName = "centos-base_克隆"
//            val code = migrateVmToNewHost(vmName, vmUuid, hostId)
//            logger.info { "code = $code" }

            // 👍深信服平台创建（克隆）并迁移虚拟机到指定主机
//            val option = CreateVmOptions("zmqVM", VirtualMachineExtraInfo(templateUuid="8d907a8a-f34f-4dc1-b755-47b5aec9e98a"),
//                1024, 1, 100000000, 10, false, "host-e03676dde96c")
//            val vm = SangforClient.createVM(option).getOrThrow()
//            logger.info { vm.name }
//            logger.info { vm.uuid }
//            logger.info { vm.platform }
//            logger.info { vm.host }

            // 👍vCenter平台获取资源占用
//            logger.info {"TEST getVcenterHostsUsage()"}
//            val hosts = getVcenterHostsUsage().getOrThrow()
//            logger.info { hosts.size }
//            hosts.forEach{
//                logger.info { "This host: NAME=" +  it.hostId}
//                logger.info { it.cpu_total_mhz.toString()+ " " + it.cpu_used_mhz.toString()+ " " + it.cpu_ratio.toString() }
//                logger.info { it.memory_total_mb.toString()+ " " + it.memory_used_mb.toString()+ " " + it.memory_ratio.toString() }
//            }

            // 👍vCenter平台直接创建（克隆）虚拟机到指定主机
//            val vm = VCenterClient.createVM(
//                CreateVmOptions(
//                    name = "zmqVM-test",
//                    extraInfo = VirtualMachineExtraInfo(templateUuid="4207e974-8edd-8555-abdc-a664fabf92a3"),
//                    memory = 2048,
//                    cpu = 2,
//                    diskSize = 16106127360,
//                    powerOn =  false,
//                    hostId = "10.251.254.22"
//                )
//            ).getOrThrow()
//            logger.info { vm.name }
//            logger.info { vm.uuid }
//            logger.info { vm.platform }
//            logger.info { vm.host }


            // 👍测试遗传算法
//                val vm1 = VirtualMachineSpec("name1", "sangfor", false, "extraInfo",
//                    4, 256, 1, 2048, VirtualMachine.PowerState.PoweredOff)
//                val vm2 = VirtualMachineSpec("name2", "sangfor", false, "extraInfo",
//                    4, 256, 1, 2048, VirtualMachine.PowerState.PoweredOff)
//                val vm3 = VirtualMachineSpec("name2", "vcenter", false, "extraInfo",
//                    4, 256, 1, 2048, VirtualMachine.PowerState.PoweredOff)
//                val host1 = PhysicalHost("hostid1", 1000, 900, 0.9,
//                    2000, 1800, 0.9)
//                val host2 = PhysicalHost("hostid2", 1000, 4, 0.1,
//                2000, 256, 0.5)
//                val host3 = PhysicalHost("hostid3", 1000, 0, 0.0,
//                2000, 0, 0.0)
//                val vmList = listOf(vm1, vm2, vm3)
//                val hostList = listOf(host1, host2, host3)
//                val ga = GeneticAlgorithm(vmList, hostList,
//                    20, 0.2, 0.2, 3, 20)
//                ga.evolve()
//                ga.getBestSolution().getAllocation().forEach { logger.info {hostList[it].hostId} }
//

            // 测试虚拟机是否创建
//            val vs = VmService()
//            vs.getExperimentVms()

            // 测试动态迁移
            logger.info { "Testing hotMigrate VM......" }
            val hostR = mapOf(
                "hostRefType" to "HostSystem",
                "hostRefValue" to "host-180" // 10.251.254.22
            )
//            val vmUuid = "42194af9-4cae-0681-185e-1b7bd7094e4a" // "zmq-test001-99131004"
            val vmUuid = "4219fd26-3670-90cd-db1c-06d76ed2b306"
            transferHost(vmUuid, hostR)
            logger.info { "VM Migrate FINISH." }


            call.respondText("All Finish!")
        }
    }
}
