package cn.edu.buaa.scs

import io.ktor.server.application.*

lateinit var application: Application

@Suppress("unused") // Referenced in application.conf
fun Application.mainModule() {
    application = this
}

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)
