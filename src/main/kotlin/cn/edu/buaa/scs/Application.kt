package cn.edu.buaa.scs

import cn.edu.buaa.scs.plugins.*
import cn.edu.buaa.scs.utils.logger
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.ktor.application.*
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import org.ktorm.database.Database
import org.ktorm.logging.Slf4jLoggerAdapter
import java.io.File

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused")
fun Application.webModule() {
    configureCORS()
    configureCallID()
    configureMonitoring()
    configureContentNegotiation()
    configureStatusPage()
    configureRouting()
}

fun Application.getConfig(name: String, default: String = ""): String =
    this.environment.config.propertyOrNull(name)?.getString() ?: default

fun Application.getFile(filename: String): File =
    File(this.javaClass.getResource(filename)?.toURI()!!)

lateinit var authRedis: RedisClient

@Suppress("unused")
fun Application.redisModule() {
    // redis for auth
    authRedis = RedisClient.create(
        RedisURI.Builder.redis(getConfig("redis.auth.host", "localhost"))
            .withPort(getConfig("redis.auth.port", "6379").toInt())
            .withPassword(getConfig("redis.auth.password", "").toCharArray())
            .build()
    )
    logger("auth-redis")().info { "auth redis connected" }
}

lateinit var db: Database
fun Application.dbModule() {
    val mainDBHost = getConfig("db.main.host", "localhost")
    val mainDBPort = getConfig("db.main.port", "3306")
    val mainDBName = getConfig("db.main.name")
    val mainDBUser = getConfig("db.main.username", "root")
    val mainDBPassword = getConfig("db.main.password")
    db = Database.connect(
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://$mainDBHost:$mainDBPort/$mainDBName"
            username = mainDBUser
            password = mainDBPassword
        }),
        logger = Slf4jLoggerAdapter(logger("mainDB")().underlyingLogger)
    )
    logger("mainDB")().info { "main database connected" }
}

lateinit var kubeClient: () -> KubernetesClient

@Suppress("unused")
fun Application.kubeModule() {
    val configString = getFile("/kubeconfig.yaml").readText()
    kubeClient = fun(): KubernetesClient {
        return DefaultKubernetesClient.fromConfig(configString)
    }
}
