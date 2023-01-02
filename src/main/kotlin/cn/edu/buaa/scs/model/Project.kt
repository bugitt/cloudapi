@file:Suppress("unused")

package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.storage.mysql
import io.ktor.server.plugins.*
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.or
import org.ktorm.entity.*
import org.ktorm.schema.*

interface Project : Entity<Project>, IEntity {
    companion object : Entity.Factory<Project>() {
        fun id(projectID: Long): Project {
            return mysql.projects.find { it.id.eq(projectID) }
                ?: throw NotFoundException("project(${projectID}) not found")
        }

        fun inAnySameProject(userIdA: String, userIdB: String): Boolean {
            return mysql.projectMembers.filter { it.userId.eq(userIdA).or(it.userId.eq(userIdB)) }.map { it.projectId }
                .toList()
                .let {
                    it.toSet().size != it.size
                }
        }
    }

    var id: Long
    var name: String
    var owner: String
    var displayName: String
    var description: String?
    var courseID: Int?
    var expID: Int?
    var isPersonal: Boolean
    var createTime: Long
}

object Projects : Table<Project>("project") {
    val id = long("id").primaryKey().bindTo { it.id }
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
