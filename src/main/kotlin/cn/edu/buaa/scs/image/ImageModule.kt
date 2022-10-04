package cn.edu.buaa.scs.image

import io.ktor.server.application.*

@Suppress("unused")
fun Application.imageModule() {
    ImageBuildRoutine.run()
}