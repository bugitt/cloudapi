package cn.edu.buaa.scs.utils.test

import cn.edu.buaa.scs.utils.encrypt.RSAEncrypt
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.test() {
    // just for test
    route("/test") {
        get {
            val string = "hdjfosdjfodsjfiodjsfodjsoifjhfhgd"
            val encryptedString = RSAEncrypt.encrypt(string)
            println(encryptedString)
            val gotString =
                RSAEncrypt.decrypt(Base64.getEncoder().encodeToString("fjsdofjdosfjdj".toByteArray())).getOrThrow()
            println(gotString)
            assert(string == gotString)
            call.respond("ok")
        }
    }
}