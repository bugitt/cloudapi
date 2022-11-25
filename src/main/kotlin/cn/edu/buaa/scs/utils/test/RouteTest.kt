package cn.edu.buaa.scs.utils.test

import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mongo
import cn.edu.buaa.scs.storage.mysql
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.apache.commons.lang3.RandomStringUtils
import org.ktorm.entity.toList

fun Route.test() {
    // just for test
    route("/test") {
        get {
            val jobs = mutableListOf<Job>()
            val getResource: (user: User) -> Resource = {
                if (it.isTeacher()) Resource(8000, 16384)
                else Resource(2000, 4096)
            }
            mongo.resourcePool.deleteMany()
            mysql.users.toList().forEach {
                jobs += launch {
                    val resourcePool = ResourcePool(
                        name = "${it.id}-${RandomStringUtils.randomNumeric(5)}",
                        ownerId = it.id,
                        capacity = getResource(it),
                    )
                    mongo.resourcePool.insertOne(resourcePool)
                    println(it.id)
                }
            }
            jobs.forEach { it.join() }
            call.respond("OK")
        }
    }
}
