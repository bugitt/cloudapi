package cn.edu.buaa.scs.cloudapi

import cn.edu.buaa.scs.cloudapi.service.AuthRedis
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class AuthenticationTest {

    @Test
    fun getString() {
        val v = AuthRedis.getId("b8f27eb8-4edd-4611-8570-25207032d388") ?: ""
        println(v)
        println(v.length)
        assert(v.isNotEmpty())
    }
}