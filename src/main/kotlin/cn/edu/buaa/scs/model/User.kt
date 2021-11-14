package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.db
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.Entity
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.varchar

interface User : Entity<User> {
    companion object : Entity.Factory<User>()

    var id: String
    var name: String
    var nickName: String
    var password: String
    var email: String
    var role: Int
    var departmentID: String
    var isAccepted: Boolean
    var acceptTime: String
}

object Users : Table<User>("user") {
    @Suppress("unused")
    val id = varchar("id").primaryKey().bindTo { it.id }

    @Suppress("unused")
    val name = varchar("name").bindTo { it.name }

    @Suppress("unused")
    val nickName = varchar("nick_name").bindTo { it.nickName }

    @Suppress("unused")
    val password = varchar("passwd").bindTo { it.password }

    @Suppress("unused")
    val email = varchar("email").bindTo { it.email }

    @Suppress("unused")
    val role = int("role").bindTo { it.role }

    @Suppress("unused")
    val departmentID = varchar("department_id").bindTo { it.departmentID }

    @Suppress("unused")
    val isAccepted = boolean("is_accept").bindTo { it.isAccepted }

    @Suppress("unused")
    val acceptTime = varchar("accept_time").bindTo { it.acceptTime }

    fun getByID(id: String): User? =
        db.users.find { it.id eq id }
}

private val Database.users get() = this.sequenceOf(Users)
