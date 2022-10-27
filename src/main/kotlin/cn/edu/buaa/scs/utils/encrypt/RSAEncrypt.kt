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
        return Base64.getEncoder().encodeToString(cipher.doFinal(message.toByteArray()))
    }

    fun decrypt(secret: String): Result<String> = runCatching {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return Result.success(String(cipher.doFinal(Base64.getDecoder().decode(secret)), Charsets.UTF_8))
    }
}