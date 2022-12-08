package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.utils.IntOrString
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
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