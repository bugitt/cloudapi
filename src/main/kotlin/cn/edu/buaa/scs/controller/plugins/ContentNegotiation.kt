package cn.edu.buaa.scs.controller.plugins

import cn.edu.buaa.scs.utils.InstantSerializer
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import org.ktorm.jackson.KtormModule
import java.time.Instant

fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        jackson {
            registerModule(KtormModule())
            registerModule(JavaTimeModule().apply {
                addSerializer(
                    Instant::class.java,
                    InstantSerializer()
                )
            })
            registerModule(Jdk8Module())
        }
    }
}