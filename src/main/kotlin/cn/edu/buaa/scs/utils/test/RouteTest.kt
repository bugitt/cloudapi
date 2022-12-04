package cn.edu.buaa.scs.utils.test

import cn.edu.buaa.scs.model.FileType
import cn.edu.buaa.scs.model.files
import cn.edu.buaa.scs.storage.mysql
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.forEach

fun Route.test() {
    // just for test
    route("/test") {
        get {
            val fileList = mysql.files.filter { it.fileType.eq(FileType.CourseResource) and (it.involvedId.eq(438)) }
            fileList.forEach {
                HttpClient(CIO).use { client ->
                    val response =
                        client.post("https://scs.buaa.edu.cn/api/v2/file/convert?fileId=${it.id}&token=PxEYxR8mqJWZEtHwC2J2")
                    println(it.name + "   " + response.status.toString())
                }
            }
            call.respond("OK")
        }
    }
}
