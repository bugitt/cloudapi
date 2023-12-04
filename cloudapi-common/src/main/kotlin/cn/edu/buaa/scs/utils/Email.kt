package cn.edu.buaa.scs.utils

import cn.edu.buaa.scs.config.Constant
import cn.edu.buaa.scs.config.globalConfig
import java.util.*
import javax.activation.CommandMap
import javax.activation.MailcapCommandMap
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object Email {
    fun sendEmail(to: String, subject: String, content: String): Result<Unit> = runCatching {
        val mc: MailcapCommandMap = CommandMap.getDefaultCommandMap() as MailcapCommandMap
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html")
        CommandMap.setDefaultCommandMap(mc)
        Thread.currentThread().contextClassLoader = javaClass.classLoader

        val config = globalConfig.email

        val properties = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "false")
            put("mail.smtp.host", globalConfig.email.smtpServer)
            put("mail.smtp.port", globalConfig.email.smtpPort)
        }

        val session = Session.getInstance(properties,
            object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(config.username, config.password)
                }
            })


        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(config.fromAddress, config.personal))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            setSubject(subject)
            setContent(content, "text/html; charset=utf-8")
        }
        Transport.send(message)
    }

    fun sendSpecEmail(userName: String, id: String, specContent: String, to: String, subject: String): Result<Unit> {
        val titleImage = globalConfig.email.titlePicture
        val content = """
            <table style="width:88%; margin-top:20px; margin-bottom:20px; background:#fafafa; border:1px solid #ddd;"
                   cellspacing="0" cellpadding="0" border="0" align="center">
                <tbody>
                <tr>
                    <td width="24">&nbsp;</td>
                    <td style="padding-top:16px;"><img width="250px" style="display:block; vertical-align:top;"
                                                       src="$titleImage" height="50px"></td>
                </tr>
                <tr>
                    <td width="24">&nbsp;</td>
                    <td style="color:#858585; font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height:20px; padding-top:24px;"
                        colspan="2">$id $userName 您好：
                    </td>
                    <td width="24">&nbsp;</td>
                </tr>
                $specContent
                <tr>
                    <td width="24" style="padding-top:18px; padding-bottom:32px; border-bottom:1px solid #e1e1e1;">&nbsp;</td>
                    <td style="color:#858585; font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height:20px; padding-top:18px; padding-bottom:32px; border-bottom:1px solid #e1e1e1;"
                        colspan="2">谢谢使用！<br> 北航软院云平台
                    </td>
                    <td width="24" style="padding-top:18px; padding-bottom:32px; border-bottom:1px solid #e1e1e1;">&nbsp;</td>
                </tr>
                <tr>
                    <td width="24">&nbsp;</td>
                    <td style="color:#858585; font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height:20px; padding-top:18px;"
                        colspan="2">您可以<a style="color:#50b7f1;text-decoration:none;font-weight:bold" rel="noopener noreferrer"
                                             href="${Constant.baseUrl}">点击此处访问云平台</a></td>
                    <td width="24">&nbsp;</td>
                </tr>
                <tr>
                    <td width="24">&nbsp;</td>
                    <td style="color:#858585; font-family:Arial, Helvetica, sans-serif; font-size:14px; line-height:20px; padding-top:24px;"
                        colspan="2">如果上述链接无法点击，请复制以下链接到浏览器地址栏进行访问：<a rel="noopener noreferrer"
                                                                                                 href="${Constant.baseUrl}">${Constant.baseUrl}</a>
                    </td>
                    <td width="24">&nbsp;</td>
                </tr>
                </tbody>
            </table>
        """.trimIndent()
        return sendEmail(to, subject, content)
    }
}
