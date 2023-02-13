package cn.edu.buaa.scs.utils.test

import cn.edu.buaa.scs.kube.BusinessKubeClient
import cn.edu.buaa.scs.model.ResourcePool
import cn.edu.buaa.scs.model.resourcePools
import cn.edu.buaa.scs.model.users
import cn.edu.buaa.scs.storage.mysql
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ktorm.entity.add
import org.ktorm.entity.forEach
import org.ktorm.entity.map
import org.ktorm.entity.toList

fun Route.test() {
    // just for test
    route("/test") {
        get {
            call.respondText("Hello, world!")
        }
    }
}
