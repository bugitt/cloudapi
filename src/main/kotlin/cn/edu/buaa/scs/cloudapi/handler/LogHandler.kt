package cn.edu.buaa.scs.cloudapi.handler

import cn.edu.buaa.scs.cloudapi.logger
import io.vertx.ext.web.RoutingContext

fun log(ctx: RoutingContext) {
    logger.info { "get it" }
    ctx.next()
    ctx.next()
}