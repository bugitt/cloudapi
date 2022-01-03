package cn.edu.buaa.scs.web.plugins

import cn.edu.buaa.scs.auth.authRoute
import cn.edu.buaa.scs.auth.fetchToken
import cn.edu.buaa.scs.utils.test.test
import io.ktor.application.*
import io.ktor.routing.*

fun Application.configureRouting() {

    intercept(ApplicationCallPipeline.Call) {
        fetchToken(call)
    }

    routing {
        route("/api/v2") {
            authRoute()
            // 添加其他的 route
        }

        // just for test
        test()
    }
}
