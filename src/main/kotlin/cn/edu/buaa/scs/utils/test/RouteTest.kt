package cn.edu.buaa.scs.utils.test

import cn.edu.buaa.scs.kube.BusinessKubeClient
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.test() {
    // just for test
    route("/test") {
        get {
            val builder = BusinessKubeClient.getBuilder("builder-sample", "default").getOrThrow()
            val spec = builder.spec
            println(spec.toString())
            val workflow = BusinessKubeClient.getWorkflow("workflow-sample", "default").getOrThrow()
            val workflowSpec = workflow.spec
            println(workflowSpec.toString())
            call.respond(builder)
        }
    }
}
