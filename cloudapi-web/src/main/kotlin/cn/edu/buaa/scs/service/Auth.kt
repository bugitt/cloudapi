package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.application
import cn.edu.buaa.scs.auth.authAdmin
import cn.edu.buaa.scs.auth.authRead
import cn.edu.buaa.scs.auth.authWrite
import cn.edu.buaa.scs.auth.generateRSAToken
import cn.edu.buaa.scs.cache.authRedis
import cn.edu.buaa.scs.controller.models.LoginUserResponse
import cn.edu.buaa.scs.controller.models.SimpleCourse
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.*
import cn.edu.buaa.scs.utils.encrypt.RSAEncrypt
import com.yufeixuan.captcha.Captcha
import com.yufeixuan.captcha.SpecCaptcha
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.inList
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.map
import java.io.ByteArrayOutputStream
import java.util.*

val ApplicationCall.auth
    get() = AuthService.getSvc(this) { AuthService(this) }

class AuthService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<AuthService>() {
        private val buaaSSOClient = HttpClient(CIO) {}
    }

    // return: captchaToken, captchaImageBase64
    fun createCaptcha(): Pair<String, String> {
        val captcha = SpecCaptcha(130, 48, 5)
        captcha.charType = Captcha.TYPE_NUM_AND_UPPER
        var imageString: String
        ByteArrayOutputStream().use { bs ->
            captcha.out(bs)
            imageString = Base64.getEncoder().encodeToString(bs.toByteArray())
        }
        return Pair(
            RSAEncrypt.encrypt(captcha.text().lowercase()),
            imageString,
        )
    }

    private fun checkCaptcha(token: String, text: String): Boolean {
        if (authRedis.checkStrUsed(token)) {
            return false
        }
        authRedis.setUsedStr(token)

        val shouldText = RSAEncrypt.decrypt(token).getOrNull() ?: return false
        return text.lowercase() == shouldText.lowercase()
    }

    suspend fun login(
        userId: String,
        passwordHash: String,
        captchaToken: String,
        captchaText: String
    ): LoginUserResponse {
        // check useId
        if (!mysql.users.exists { it.id.eq(userId) }) {
            throw BadRequestException("")
        }
        // check password
        val user = mysql.users.find { it.id.eq(userId) and it.password.eq(passwordHash) }
            ?: throw BadRequestException("")
        // check active
        if (!user.isAccepted) {
            throw BadRequestException("账号未激活")
        }
        // check captcha
        if (!checkCaptcha(captchaToken, captchaText)) {
            throw BadRequestException("验证码错误")
        }

        // generate token
        val token = generateRSAToken(user.id)

        return afterLogin(token, user)
    }

    suspend fun whoami(listProjects: Boolean): LoginUserResponse {
        val user = User.id(call.userId())
        val resp = afterLogin(call.token(), user)
        if (listProjects) {
            val projectList =
                mysql
                    .projectMembers
                    .filter { it.userId eq user.id }
                    .map { it.projectId }
                    .distinct()
                    .let { idList ->
                        if (idList.isNotEmpty()) {
                            mysql.projects.filter { it.id inList idList }.map { it.name }
                        } else {
                            listOf()
                        }
                    }
            return resp.copy(projects = projectList)
        }
        return resp
    }

    suspend fun buaaSSOLogin(ssoToken: String): LoginUserResponse {
        val user = User.id(verifySSOToken(ssoToken).getOrThrow())
        return afterLogin(generateRSAToken(user.id), user)
    }

    private suspend fun afterLogin(token: String, user: User): LoginUserResponse {
        if (user.paasToken.isBlank()) {
            call.project.createUser(user.id)
        }

        // insert token in redis (just for compatibility with older platforms)
        authRedis.setToken(token, user.id)

        return LoginUserResponse(
            username = user.name,
            token = token,
            userId = user.id,
            role = if (user.isStudent()) "student" else if (user.isTeacher()) "teacher" else "superAdmin",
            paasToken = user.paasToken,
            isAssistant = mysql.assistants.exists { it.studentId.eq(user.id) },
            adminCourses = user.getAdminCourses().map { SimpleCourse(it.first, it.second) }
        )
    }

    // return userID
    private suspend fun verifySSOToken(ssoToken: String): Result<String> = runCatching {
        val resp = buaaSSOClient
            .get(application.getConfigString("auth.buaaSSOValidationURL") + ssoToken)
            .body<String>()
        val regex = application.getConfigString("auth.buaaSSOValidation.regex").toRegex()
        regex.find(resp)?.groupValues?.get(1)?.uppercase() ?: throw BadRequestException("BUAA SSO Token 验证失败")
    }

    fun checkPermission(entityType: String, entityId: String, action: String): Boolean {
        val entity: IEntity = when (entityType.lowercase()) {
            "project" -> Project.id(entityId.toLong())
            "projectname" -> mysql.projects.find { it.name eq entityId } ?: throw BadRequestException("项目不存在")
            "course" -> Course.id(entityId.toInt())
            "assignment" -> Assignment.id(entityId.toInt())
            "experiment" -> Experiment.id(entityId.toInt())
            else -> throw BadRequestException("未知的实体类型")
        }
        return when (action.lowercase()) {
            "read" -> call.user().authRead(entity)
            "write" -> call.user().authWrite(entity)
            "admin" -> call.user().authAdmin(entity)
            else -> throw BadRequestException("未知的操作类型")
        }
    }
}
