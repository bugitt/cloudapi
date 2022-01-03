package cn.edu.buaa.scs.storage

import cn.edu.buaa.scs.getConfigString
import cn.edu.buaa.scs.utils.logger
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import org.ktorm.database.Database
import org.ktorm.logging.Slf4jLoggerAdapter

lateinit var mysql: Database
fun Application.dbModule() {
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
        }),
        logger = Slf4jLoggerAdapter(logger("mainDB")().underlyingLogger)
    )
    logger("mainDB")().info { "main database connected" }
}