package cn.edu.buaa.scs.cloudapi

import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext

// auto call next handler
fun Route.handle(worker: (RoutingContext) -> Unit): Route {
    this.handler { ctx ->
        worker(ctx)
        ctx.next()
    }
    return this
}