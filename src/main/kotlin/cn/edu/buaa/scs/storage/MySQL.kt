package cn.edu.buaa.scs.storage

import cn.edu.buaa.scs.utils.getConfigString
import cn.edu.buaa.scs.utils.logger
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.ktorm.database.Database
import org.ktorm.logging.Logger

lateinit var mysql: Database
lateinit var bugitDB: Database

@Suppress("unused")
fun Application.mysqlModule() {
    val mainDBHost = getConfigString("db.main.host", "localhost")
    val mainDBPort = getConfigString("db.main.port", "3306")
    val mainDBName = getConfigString("db.main.name")
    val mainDBUser = getConfigString("db.main.username", "root")
    val mainDBPassword = getConfigString("db.main.password")
    mysql = Database.connect(
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://$mainDBHost:$mainDBPort/$mainDBName"
            username = mainDBUser
            password = mainDBPassword
            addDataSourceProperty("useUnicode", "true")
            addDataSourceProperty("characterEncoding", "utf8")
        }),
        logger = DBLogger(logger("mainDB")().underlyingLogger)
    )
    logger("mainDB")().info { "main database connected" }

    bugitDB = Database.connect(
        dataSource = HikariDataSource(HikariConfig()).apply {
            jdbcUrl = getConfigString("db.bugit.connectionString")
            username = getConfigString("db.bugit.username")
            password = getConfigString("db.bugit.password")
            addDataSourceProperty("useUnicode", "true")
            addDataSourceProperty("characterEncoding", "utf8")
        },
        logger = DBLogger(logger("bugitDB")().underlyingLogger)
    )
    logger("bugitDB")().info { "bugit database connected" }
}

internal class DBLogger(private val logger: org.slf4j.Logger) : Logger {

    override fun isTraceEnabled(): Boolean {
        return false
    }

    override fun trace(msg: String, e: Throwable?) {
        logger.trace(msg, e)
    }

    override fun isDebugEnabled(): Boolean {
        return false
    }

    override fun debug(msg: String, e: Throwable?) {
        logger.debug(msg, e)
    }

    override fun isInfoEnabled(): Boolean {
        return false
    }

    override fun info(msg: String, e: Throwable?) {
        logger.info(msg, e)
    }

    override fun isWarnEnabled(): Boolean {
        return true
    }

    override fun warn(msg: String, e: Throwable?) {
        logger.warn(msg, e)
    }

    override fun isErrorEnabled(): Boolean {
        return true
    }

    override fun error(msg: String, e: Throwable?) {
        logger.error(msg, e)
    }
}
