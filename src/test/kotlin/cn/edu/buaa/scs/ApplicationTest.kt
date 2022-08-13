package cn.edu.buaa.scs

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*
import io.ktor.server.testing.*

val testEnv = createTestEnvironment {
    config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
}