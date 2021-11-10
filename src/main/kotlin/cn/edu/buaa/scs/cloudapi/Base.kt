package cn.edu.buaa.scs.cloudapi

import com.charleskorn.kaml.Yaml
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.ktorm.database.Database
import org.ktorm.logging.Slf4jLoggerAdapter
import java.io.File

// log
val logger = KotlinLogging.logger { }

// database
val database: Database by lazy {
    val dbConfig = Constant.config.db
    Database.connect(
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${dbConfig.host}:${dbConfig.port}/${dbConfig.name}"
            username = dbConfig.user
            password = dbConfig.password
        }),
        logger = Slf4jLoggerAdapter(logger.underlyingLogger)
    )
}

// Constants
object Constant {
    private var args: Array<String> = arrayOf()
    fun setArgs(args: Array<String>) = run { this.args = args }

    val config: Config by lazy { parseConfig(args) }
}

// Config
@Serializable
data class Config(
    val db: DBConfig,
) {

    @Serializable
    data class DBConfig(
        val host: String,
        val port: Int,
        val name: String,
        val user: String,
        val password: String,
    )
}

private fun parseConfig(args: Array<String>): Config =
    ((args.indices.firstOrNull { args[it] == "--conf" || args[it] == "-c" })?.let { args[it + 1] }
        ?: "conf.yaml")
        .let { filename ->
            // todo log filename
            println(filename)
            Yaml.default.decodeFromStream(Config.serializer(), File(filename).inputStream())
        }