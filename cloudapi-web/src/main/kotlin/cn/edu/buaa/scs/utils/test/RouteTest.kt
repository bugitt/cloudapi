package cn.edu.buaa.scs.utils.test

import cn.edu.buaa.scs.kube.BusinessKubeClient
import cn.edu.buaa.scs.model.ResourcePool
import cn.edu.buaa.scs.model.resourcePools
import cn.edu.buaa.scs.model.users
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.vm.CreateVmOptions
import cn.edu.buaa.scs.vm.sfClient
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ktorm.entity.add
import org.ktorm.entity.forEach
import org.ktorm.entity.map
import org.ktorm.entity.toList
import kotlin.system.measureTimeMillis

fun Route.test() {
    // just for test
    route("/test") {
        get {
            val time = measureTimeMillis {
                sfClient.createVM(
                    CreateVmOptions(
                        "centos-test04",
                        "8d907a8a-f34f-4dc1-b755-47b5aec9e98a",
                        applyId = "",
                        memory = 1024,
                        cpu = 2,
                        diskSize = 21474836480L,
                        powerOn = true
                    )
                )
            }
            println("Time usage: $time ms.")
            call.respondText("Hello, world!")
        }
    }
}
