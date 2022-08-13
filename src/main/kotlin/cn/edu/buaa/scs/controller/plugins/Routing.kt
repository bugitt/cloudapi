package cn.edu.buaa.scs.controller.plugins

import cn.edu.buaa.scs.auth.authRoute
import cn.edu.buaa.scs.auth.fetchToken
import cn.edu.buaa.scs.route.*
import cn.edu.buaa.scs.utils.test.test
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

    intercept(ApplicationCallPipeline.Call) {
        fetchToken(call)
    }

    routing {
        route("/api/v2") {
            authRoute()
            courseRoute()
            experimentRoute()
            fileRoute()
            statRoute()
            peerRoute()
            vmRoute()
            websocketRoute()
            // 添加其他的 route
        }

        // just for test
        test()
    }
}
