package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.project.managerList
import io.ktor.server.application.*
import org.apache.commons.lang3.RandomStringUtils

val ApplicationCall.project
    get() = ProjectService.getSvc(this) { ProjectService(this) }

class ProjectService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<ProjectService>()

    suspend fun createUser(userID: String) {
        val user = User.id(userID)
        if (user.paasToken != "") return
        val paasToken = RandomStringUtils.randomAlphanumeric(13)
        managerList.forEach {
            it.createUser(
                userID = user.id,
                realName = user.name,
                email = user.email,
                password = paasToken
            )
        }
        user.paasToken = paasToken
        user.flushChanges()
    }
}