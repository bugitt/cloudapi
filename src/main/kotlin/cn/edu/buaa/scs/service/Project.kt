package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.bugit.GitClient
import cn.edu.buaa.scs.bugit.GitRepo
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.project.managerList
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.exists
import cn.edu.buaa.scs.utils.isValidProjectName
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.userId
import io.ktor.server.application.*
import org.apache.commons.lang3.RandomStringUtils
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.inList
import org.ktorm.entity.*

val ApplicationCall.project
    get() = ProjectService.getSvc(this) { ProjectService(this) }

class ProjectService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<ProjectService>()

    suspend fun createUser(userID: String) {
        val user = User.id(userID)
        if (user.paasToken != "") return
        val paasToken = RandomStringUtils.randomAlphanumeric(13)
        managerList.forEach {
            it.createUser(
                userID = user.id,
                realName = user.name,
                email = user.email,
                password = paasToken
            )
        }
        user.paasToken = paasToken
        createProjectForCurrentUser(
            "project-${user.id}",
            displayName = "${user.id}的个人项目",
            description = "${user.id}的个人项目",
            isPersonal = true,
        )
        user.flushChanges()
    }

    suspend fun createProjectForCurrentUser(
        name: String,
        expID: Int? = null,
        displayName: String = "",
        description: String = "",
        isPersonal: Boolean = false,
    ): Project {
        val experiment = if (expID != null) {
            val experiment = Experiment.id(expID)
            call.user().assertRead(experiment)
            // 检查之前在该实验下没有创建过Project
            if (mysql.projectMembers.exists { it.expId.eq(expID) and it.userId.eq(call.userId()) }) {
                throw BadRequestException("You have already created a project in this experiment")
            }
            experiment
        } else {
            null
        }
        if (!name.isValidProjectName()) {
            throw BadRequestException("Project name is invalid")
        }
        for ((i, manager) in managerList.withIndex()) {
            val result = manager.createProjectForUser(call.userId(), name, displayName, description)
            if (result.isFailure) {
                for (j in 0..i) {
                    managerList[j].deleteProject(name)
                }
                throw result.exceptionOrNull()!!
            }
        }
        val project = Project {
            this.name = name
            this.owner = call.userId()
            this.expID = expID
            this.courseID = experiment?.course?.id
            this.displayName = displayName
            this.description = description
            this.isPersonal = isPersonal
            this.createTime = System.currentTimeMillis()
        }
        val projectMember = ProjectMember {
            this.userId = call.userId()
            this.username = call.user().name
            this.projectId = project.id
            this.expID = expID
            this.role = ProjectRole.OWNER
        }
        try {
            mysql.useTransaction {
                mysql.projects.add(project)
                mysql.projectMembers.add(projectMember)
            }
        } catch (e: Throwable) {
            managerList.forEach { it.deleteProject(name) }
        }
        return project
    }

    fun getProjects(expID: Int?): List<Project> {
        if (expID != null) {
            val experiment = Experiment.id(expID)
            return if (call.user().isCourseAdmin(experiment.course)) {
                mysql.projects.filter { it.expID eq expID }.toList()
            } else {
                mysql.projectMembers.find { it.expId.eq(expID) and it.userId.eq(call.userId()) }
                    ?.let { projectMemeber ->
                        mysql.projects.filter { it.id eq projectMemeber.projectId }.toList()
                    } ?: listOf()
            }
        }
        if (call.user().isAdmin()) return mysql.projects.toList()
        mysql.projectMembers.filter { it.userId.eq(call.userId()) }.map { it.projectId }.let { projectIds ->
            return if (projectIds.isNotEmpty()) {
                mysql.projects.filter { it.id.inList(projectIds) }.toList()
            } else {
                listOf()
            }
        }
    }

    fun getAllMembers(projectID: Long): List<ProjectMember> {
        return mysql.projectMembers.filter { it.projectId.eq(projectID) }.toList()
    }

    suspend fun getAllReposForProject(projectName: String): List<GitRepo> {
        return GitClient.getRepoListOfProject(projectName).getOrThrow()
    }
}