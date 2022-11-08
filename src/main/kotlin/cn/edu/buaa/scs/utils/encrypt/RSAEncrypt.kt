package cn.edu.buaa.scs.utils.encrypt

import cn.edu.buaa.scs.application
import cn.edu.buaa.scs.utils.getConfigString
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher

object RSAEncrypt {
    private val privateKey: PrivateKey by lazy {
        KeyFactory
            .getInstance("RSA")
            .generatePrivate(
                PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(
                        application.getConfigString("auth.rsa.privateKey")
                    )
                )
            )
    }

    private val publicKey: PublicKey by lazy {
        KeyFactory
            .getInstance("RSA")
            .generatePublic(
                X509EncodedKeySpec(
                    Base64.getDecoder().decode(
                        application.getConfigString("auth.rsa.publicKey")
                    )
                )
            )
    }

    fun encrypt(message: String): String {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return encode(Base64.getEncoder().encodeToString(cipher.doFinal(message.toByteArray())))
    }

    fun decrypt(secret: String): Result<String> = runCatching {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return Result.success(String(cipher.doFinal(Base64.getDecoder().decode(decode(secret))), Charsets.UTF_8))
    }

    private const val partition = "l"

    private fun encode(src: String): String {
        return src.toCharArray().joinToString(partition) {
            it.code.toString(16).lowercase()
        }
    }

    private fun decode(src: String): String {
        return src.split(partition).map {
            it.toInt(16).toChar()
        }.joinToString("")
    }
}