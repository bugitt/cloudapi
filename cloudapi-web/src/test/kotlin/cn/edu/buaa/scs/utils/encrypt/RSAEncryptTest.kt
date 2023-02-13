package cn.edu.buaa.scs.utils.encrypt

import org.junit.jupiter.api.Test
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import kotlin.test.assertEquals

internal class RSAEncryptTest {
    @Test
    fun testRSAEncode() {
        val generator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(516)
        val pair: KeyPair = generator.generateKeyPair()
        val privateKey = pair.private
        val publicKey = pair.public
        val privateKeyStr = Base64.getEncoder().encodeToString(privateKey.encoded)
        val publicKeyStr = Base64.getEncoder().encodeToString(publicKey.encoded)
        println(privateKeyStr)
        println(publicKeyStr)
        val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")
        val gotPrivateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyStr)))
        val gotPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyStr)))
        assertEquals(privateKey, gotPrivateKey)
        assertEquals(publicKey, gotPublicKey)
    }
}