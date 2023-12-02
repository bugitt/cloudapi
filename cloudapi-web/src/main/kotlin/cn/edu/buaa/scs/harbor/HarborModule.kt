package cn.edu.buaa.scs.harbor

import cn.edu.buaa.scs.sdk.harbor.infrastructure.ApiClient
import cn.edu.buaa.scs.utils.getConfigString
import io.ktor.server.application.*

@Suppress("unused") // Referenced in application.conf
fun Application.harborModule() {
    System.setProperty(ApiClient.baseUrlKey, getConfigString("harbor.baseUrl"))
    ApiClient.username = "admin"
    ApiClient.password = getConfigString("harbor.adminPassword")
}
