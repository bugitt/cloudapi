@file:Suppress("unused")

package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

interface Project : Entity<Project> {
    companion object : Entity.Factory<Project>()

    var id: Long
    var name: String
    var owner: String
    var displayName: String?
    var description: String?
    var courseID: Int?
    var expID: Int?
    var isPersonal: Boolean
    var createTime: Long
}

object Projects : Table<Project>("project") {
    val id = long("id").primaryKey()
    val name = varchar("name").bindTo { it.name }
    val displayName = varchar("display_name").bindTo { it.displayName }
    val description = text("description").bindTo { it.description }
    val owner = varchar("owner").bindTo { it.owner }
    val courseID = int("course_id").bindTo { it.courseID }
    val expID = int("exp_id").bindTo { it.expID }
    val isPersonal = boolean("is_personal").bindTo { it.isPersonal }
    val createTime = long("create_time").bindTo { it.createTime }
}

val Database.projects get() = this.sequenceOf(Projects)