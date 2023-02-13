@file:Suppress("unused")

package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.long
import org.ktorm.schema.varchar

interface ResourcePool : Entity<ResourcePool> {
    companion object : Entity.Factory<ResourcePool>()

    var id: Long
    var name: String
    var ownerId: String
}

object ResourcePools : Table<ResourcePool>("resource_pool") {
    val id = long("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val ownerId = varchar("owner_id").bindTo { it.ownerId }
}

val Database.resourcePools get() = this.sequenceOf(ResourcePools)
