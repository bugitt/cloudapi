package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.application
import cn.edu.buaa.scs.auth.*
import cn.edu.buaa.scs.cache.authRedis
import cn.edu.buaa.scs.config.Constant
import cn.edu.buaa.scs.controller.models.LoginUserResponse
import cn.edu.buaa.scs.controller.models.SimpleCourse
import cn.edu.buaa.scs.controller.models.TokenInfoResponse
import cn.edu.buaa.scs.controller.models.TokenInfoResponseData
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.*
import cn.edu.buaa.scs.utils.encrypt.RSAEncrypt
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.f4b6a3.ulid.UlidCreator
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
            throw BadRequestException("账号未激活，或信息不完整，请重新激活账户")
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

    fun getTokenInfo(token: String, service: String): TokenInfoResponse {
        val userId =
            // rsa token
            RSAEncrypt.decrypt(token).getOrNull()?.let { tokenInfo ->
                jsonReadValue<TokenInfo>(tokenInfo).userId
            } ?:
            // redis uuid token
            authRedis.checkToken(token) ?:
            // error
            return TokenInfoResponse(2001, "$service Token错误")
        val user = User.id(userId)
        return TokenInfoResponse(1003, "$service 验证成功", TokenInfoResponseData(user.id, if (user.isStudent()) "student" else if (user.isTeacher()) "teacher" else "superAdmin", service))
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
            adminCourses = user.getAdminCourses().map { SimpleCourse(it.first, it.second) },
            nickname = user.nickName,
            email = user.email,
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

    data class ActiveMessage(
        val id: String,
        val name: String,
        val email: String,
    )

    data class ResetPassword(
        val id: String,
    )

    fun sendActiveEmail(id: String, name: String, email: String) {
        val user = User.id(id)
        if (user.isAccepted) {
            throw cn.edu.buaa.scs.error.BadRequestException("the user is already active")
        }
        val token = "${user.id}${user.password}${System.currentTimeMillis()}".md5() + UlidCreator.getUlid().toString()

        val activeUrl = "${Constant.baseUrl}/#/security/activateAccount?token=$token"

        val activeMsg = ActiveMessage(id, name, email)

        authRedis.setExpireKey(token, jsonMapper.writeValueAsString(activeMsg), 60 * 60)

        val content = """
            <tr>
                <td width="24">&nbsp;</td>
                <td style="color:#858585; font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height:20px; padding-top:18px;"
                    colspan="2">请点击以下网址来激活用户
                </td>
                <td width="24">&nbsp;</td>
            </tr>
            <tr>
                <td width="24">&nbsp;</td>
                <td style="color:#858585; font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height:20px; padding-top:18px;"
                    colspan="2"><a style="color:#50b7f1;text-decoration:none;font-weight:bold" rel="noopener noreferrer"
                                   href="$activeUrl">账户激活</a>
                </td>
                <td width="24">&nbsp;</td>
            </tr>
            <tr>
                <td width="24">&nbsp;</td>
                <td style="color:#858585; font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height:20px; padding-top:24px;"
                    colspan="2">如果上述链接无法点击，请复制以下链接<a rel="noopener noreferrer"
                                                                      href="$activeUrl">$activeUrl</a>
                </td>
                <td width="24">&nbsp;</td>
            </tr>
        """.trimIndent()

        Email.sendSpecEmail(name, id, content, email, "北航软件学院账号激活").getOrThrow()

        user.email = email
        user.name = name
        user.flushChanges()
    }

    fun sendResetPasswordEmail(id: String, email: String) {
        val user = User.id(id)
        if (user.email != email) {
            throw cn.edu.buaa.scs.error.BadRequestException("Email地址错误")
        }

        val token = "${user.id}${email}${System.currentTimeMillis()}".md5() + UlidCreator.getUlid().toString()

        authRedis.setExpireKey(token, jsonMapper.writeValueAsString(ResetPassword(id)), 60 * 60)

        val resetPasswordUrl = "${Constant.baseUrl}/#/security/findPwd?token=$token"

        val content = """
            <tr>
                <td width="24">&nbsp;</td>
                <td style="color:#858585; font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height:20px; padding-top:18px;"
                    colspan="2">请点击以下网址来重置密码
                </td>
                <td width="24">&nbsp;</td>
            </tr>
            <tr>
                <td width="24">&nbsp;</td>
                <td style="color:#858585; font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height:20px; padding-top:18px;"
                    colspan="2"><a style="color:#50b7f1;text-decoration:none;font-weight:bold" rel="noopener noreferrer"
                                   href="$resetPasswordUrl">重置密码</a>
                </td>
                <td width="24">&nbsp;</td>
            </tr>
            <tr>
                <td width="24">&nbsp;</td>
                <td style="color:#858585; font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height:20px; padding-top:24px;"
                    colspan="2">如果上述链接无法点击，请复制以下链接<a rel="noopener noreferrer"
                                                                      href="$resetPasswordUrl">$resetPasswordUrl</a>
                </td>
                <td width="24">&nbsp;</td>
            </tr>
        """.trimIndent()

        Email.sendSpecEmail(user.name, user.id, content, email, "北航软件学院云平台密码重置").getOrThrow()
    }

    suspend fun activeUser(token: String, password: String): LoginUserResponse {
        val activeMsgStr =
            authRedis.getValueByKey(token) ?: throw cn.edu.buaa.scs.error.BadRequestException("invalid token")
        val activeMsg = jsonMapper.readValue<ActiveMessage>(activeMsgStr)
        val user = User.id(activeMsg.id)

        user.email = activeMsg.email
        user.name = activeMsg.name
        user.password = password
        user.isAccepted = true
        user.acceptTime = System.currentTimeMillis().toString()
        user.flushChanges()

        call.project.createUser(user)

        return afterLogin(generateRSAToken(user.id), user)
    }

    fun resetPassword(token: String, password: String) {
        val activeMsgStr =
            authRedis.getValueByKey(token) ?: throw cn.edu.buaa.scs.error.BadRequestException("invalid token")
        val resetPassword = jsonMapper.readValue<ResetPassword>(activeMsgStr)

        val user = User.id(resetPassword.id)
        user.password = password
        user.flushChanges()
    }

}
