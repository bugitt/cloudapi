@file:Suppress("unused")

package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.long
import org.ktorm.schema.varchar

interface GitRepo : Entity<GitRepo>, IEntity {
    companion object : Entity.Factory<GitRepo>()

    var id: Long
    var ownerId: Long
    var lowerName: String
    var name: String
}

object GitRepoList : Table<GitRepo>("repository") {
    val id = long("id").primaryKey().bindTo { it.id }
    val ownerId = long("owner_id").bindTo { it.ownerId }
    val lowerName = varchar("lower_name").bindTo { it.lowerName }
    val name = varchar("name").bindTo { it.name }
}

val Database.gitRepoList
    get() = this.sequenceOf(GitRepoList)
