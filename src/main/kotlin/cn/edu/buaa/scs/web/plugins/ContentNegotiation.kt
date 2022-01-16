package cn.edu.buaa.scs.web.plugins

import cn.edu.buaa.scs.utils.LocalDateTimeSerializer
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import org.ktorm.jackson.KtormModule
import java.time.LocalDateTime

fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        jackson {
            registerModule(KtormModule())
            registerModule(JavaTimeModule().apply {
                addSerializer(
                    LocalDateTime::class.java,
                    LocalDateTimeSerializer()
                )
            })
            registerModule(Jdk8Module())
        }
    }
}