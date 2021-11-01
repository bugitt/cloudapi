package cn.edu.buaa.scs.cloudapi

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class ConfigTest {

    @Test
    fun getEnv() {
        println(Config.getEnvVal<Int>("redis.port"))
        println(Config.getEnvVal<String>("redis.host"))
        println(Config.getEnvVal<String>("redis.password"))
    }
}