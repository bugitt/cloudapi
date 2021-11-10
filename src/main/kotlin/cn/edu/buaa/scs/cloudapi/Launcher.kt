package cn.edu.buaa.scs.cloudapi

import cn.edu.buaa.scs.cloudapi.handler.log
import com.sun.tools.javac.Main
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.impl.LoggerHandlerImpl

fun main(args: Array<String>) {
    logger.info { args.joinToString() }
    // init Constant
    Constant.setArgs(args)
    logger.info { Constant.config }

    // init database connection
    logger.info { database.name }

    // launch vertx
    val vertx: Vertx = Vertx.vertx()
    vertx.deployVerticle(MainVerticle())
}

class MainVerticle : AbstractVerticle() {

    private val helloRouter: Router = Router.router(vertx).apply {
        get("/hello").handler{ctx ->
            ctx.response().end("hello")
        }
        post()
    }

    private val authenticationRouter: Router = Router.router(vertx).apply {
        mountSubRouter("/", Router.router(vertx).apply {
            get()

        })
    }

    private val mainRouter: Router = Router.router(vertx).apply {
        route().handler(::log)
        mountSubRouter("/api/v2", Router.router(vertx).apply {
            mountSubRouter("/authentications", authenticationRouter)
        })
    }

    override fun start(startPromise: Promise<Void>) {
        vertx
            .createHttpServer()
            .requestHandler(mainRouter)
            .listen(8888) { http ->
                if (http.succeeded()) {
                    startPromise.complete()
                    logger.info("HTTP server started on port 8888")
                } else {
                    startPromise.fail(http.cause())
                }
            }
    }
}
