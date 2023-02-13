package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.utils.IntOrString
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

interface Term : Entity<Term>, IEntity {
    companion object : Entity.Factory<Term>()

    var id: Int
    var name: String

    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}

object Terms : Table<Term>("term") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
}

val Database.terms get() = this.sequenceOf(Terms)