package cn.edu.buaa.scs.cloudapi

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment


@SpringBootApplication
class CloudapiApplication {
    @Bean
    fun applicationRunner(environment: Environment): ApplicationRunner {
        return ApplicationRunner {
            EnvInitialHelper.env = environment
        }
    }
}

fun main(args: Array<String>) {
    runApplication<CloudapiApplication>(*args)
}
