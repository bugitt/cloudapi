package cn.edu.buaa.scs.storage

import cn.edu.buaa.scs.getConfigString
import cn.edu.buaa.scs.model.Assistants
import cn.edu.buaa.scs.model.Courses
import cn.edu.buaa.scs.model.Experiments
import cn.edu.buaa.scs.model.Users
import cn.edu.buaa.scs.utils.logger
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import org.ktorm.database.Database
import org.ktorm.entity.sequenceOf
import org.ktorm.logging.Slf4jLoggerAdapter

lateinit var mysql: Database

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
        }),
        logger = Slf4jLoggerAdapter(logger("mainDB")().underlyingLogger)
    )
    logger("mainDB")().info { "main database connected" }
}

@Suppress("unused")
val Database.assistants
    get() = this.sequenceOf(Assistants)

@Suppress("unused")
val Database.courses
    get() = this.sequenceOf(Courses)

@Suppress("unused")
val Database.experiments
    get() = this.sequenceOf(Experiments)

val Database.users get() = this.sequenceOf(Users)