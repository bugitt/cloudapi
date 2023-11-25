package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.storage.mysql
import io.ktor.server.plugins.*
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import org.ktorm.schema.Table
import org.ktorm.schema.varchar

interface Department : Entity<Department>, IEntity {
    companion object : Entity.Factory<Department>()

    var id: String
    var name: String

    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}

object Departments : Table<Department>("department") {
    val id = varchar("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
}

val Database.departments
    get() = this.sequenceOf(Departments)

val departments: Map<String, Department> by lazy {
    mysql.departments.toList().associateBy { it.id }
}

fun Department.Companion.id(id: String): Department {
    return departments[id] ?: throw BadRequestException("department($id) not found")
}

fun Department.Companion.id(id: Int): Department {
    return Department.id(id.toString())
}
