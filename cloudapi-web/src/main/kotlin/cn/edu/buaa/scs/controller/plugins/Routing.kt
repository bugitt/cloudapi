package cn.edu.buaa.scs.controller.plugins

import cn.edu.buaa.scs.kube.podLogWsRoute
import cn.edu.buaa.scs.route.*
import cn.edu.buaa.scs.utils.test.test
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        route("/api/v2") {
            authRoute()
            userRoute()
            courseRoute()
            experimentRoute()
            fileRoute()
            statRoute()
            peerRoute()
            projectRoute()
            vmRoute()
            routineRoute()
            logRoute()
            adminRoute()
            termRoute()
            // 添加其他的 route


            // ws
            websocketRoute()
            podLogWsRoute()
        }

        // just for test
        test()
    }
}
