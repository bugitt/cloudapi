@file:Suppress("unused")

package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

enum class ProjectRole {
    OWNER, ADMIN, MEMBER
}

interface ProjectMember : Entity<ProjectMember> {
    companion object : Entity.Factory<ProjectMember>()

    var id: Long
    var projectId: Long
    var userId: String
    var username: String
    var expID: Int?
    var role: ProjectRole
}

object ProjectMembers : Table<ProjectMember>("project_member") {
    val id = long("id").primaryKey()
    val projectId = long("project_id").bindTo { it.projectId }
    val userId = varchar("user_id").bindTo { it.userId }
    val username = varchar("username").bindTo { it.username }
    val expId = int("exp_id").bindTo { it.expID }
    val role = varchar("role")
        .transform({ ProjectRole.valueOf(it) }, { it.name })
        .bindTo { it.role }
}

val Database.projectMembers get() = this.sequenceOf(ProjectMembers)