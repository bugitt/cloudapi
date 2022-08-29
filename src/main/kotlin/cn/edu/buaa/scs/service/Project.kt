package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.bugit.GitClient
import cn.edu.buaa.scs.bugit.GitRepo
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.project.IProjectManager
import cn.edu.buaa.scs.project.managerList
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.exists
import cn.edu.buaa.scs.utils.isValidProjectName
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.userId
import io.ktor.server.application.*
import io.ktor.server.plugins.*
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
        createProjectForUser(
            user,
            user.personalProjectName(),
            displayName = "${user.id}的个人项目",
            description = "${user.id}的个人项目",
            isPersonal = true,
        )
        user.flushChanges()
    }

    private suspend fun createProjectForUser(
        user: User,
        name: String,
        expID: Int? = null,
        displayName: String = "",
        description: String = "",
        isPersonal: Boolean = false,
    ): Project {
        val experiment = if (expID != null) {
            val experiment = Experiment.id(expID)
            user.assertRead(experiment)
            // 检查之前在该实验下没有创建过Project
            if (mysql.projectMembers.exists { it.expId.eq(expID) and it.userId.eq(user.id) }) {
                throw BadRequestException("You have already created a project in this experiment")
            }
            experiment
        } else {
            null
        }
        if (!name.isValidProjectName()) {
            throw BadRequestException("Project name is invalid")
        }
        return transactionWork(
            { this.createProjectForUser(user.id, name, displayName, description) },
            { this.deleteProject(name) },
        ) {
            val project = Project {
                this.name = name
                this.owner = user.id
                this.expID = expID
                this.courseID = experiment?.course?.id
                this.displayName = displayName
                this.description = description
                this.isPersonal = isPersonal
                this.createTime = System.currentTimeMillis()
            }
            val projectMember = ProjectMember {
                this.userId = user.id
                this.username = user.name
                this.projectId = project.id
                this.expID = expID
                this.role = ProjectRole.OWNER
            }
            mysql.useTransaction {
                mysql.projects.add(project)
                mysql.projectMembers.add(projectMember)
            }
            project
        }.getOrThrow()
    }

    suspend fun createProjectForCurrentUser(
        name: String,
        expID: Int? = null,
        displayName: String = "",
        description: String = "",
        isPersonal: Boolean = false,
    ) = createProjectForUser(call.user(), name, expID, displayName, description, isPersonal)

    fun getProject(projectID: Long): Project {
        val project = Project.id(projectID)
        call.user().assertRead(project)
        return project
    }

    fun getProjects(expID: Int?): List<Project> {
        if (expID != null) {
            val experiment = Experiment.id(expID)
            return if (call.user().isCourseAdmin(experiment.course)) {
                mysql.projects.filter { it.expID eq expID }.toList()
            } else {
                mysql.projectMembers.find { it.expId.eq(expID) and it.userId.eq(call.userId()) }
                    ?.let { projectMember ->
                        mysql.projects.filter { it.id eq projectMember.projectId }.toList()
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

    suspend fun addProjectMember(projectID: Long, memberID: String, role: ProjectRole): ProjectMember {
        val project = Project.id(projectID)
        if (!call.user().isProjectAdmin(project)) {
            throw AuthorizationException("You are not the project admin")
        }
        return transactionWork(
            { this.addProjectMember(project.name, memberID) },
            { this.removeProjectMember(project.name, memberID) },
        ) {
            val projectMember = ProjectMember {
                this.userId = memberID
                this.username = memberID
                this.projectId = project.id
                this.expID = project.expID
                this.role = role
            }
            mysql.projectMembers.add(projectMember)
            projectMember
        }.getOrThrow()
    }

    suspend fun removeProjectMember(projectID: Long, memberID: String): ProjectMember {
        val project = Project.id(projectID)
        if (!call.user().isProjectAdmin(project)) {
            throw AuthorizationException("You are not the project admin")
        }
        val projectMember = mysql.projectMembers.find { it.projectId.eq(projectID) and it.userId.eq(memberID) }
            ?: throw NotFoundException("Project member not found")

        managerList.forEach { it.removeProjectMember(project.name, memberID) }
        projectMember.delete()
        return projectMember
    }

    fun getProjectMembers(projectID: Long): List<ProjectMember> {
        return mysql.projectMembers.filter { it.projectId.eq(projectID) }.toList()
    }

    suspend fun getAllReposForProject(projectName: String): List<GitRepo> {
        return GitClient.getRepoListOfProject(projectName).getOrThrow()
    }

    private suspend fun <T, K, U> transactionWork(
        `do`: suspend IProjectManager.() -> Result<K>,
        undo: suspend IProjectManager.() -> Result<U>,
        dbWork: suspend () -> T
    ): Result<T> {
        for ((i, manager) in managerList.withIndex()) {
            val result = manager.`do`()
            if (result.isFailure) {
                for (j in 0 until i) {
                    managerList[j].undo()
                }
                throw result.exceptionOrNull()!!
            }
        }
        return try {
            Result.success(dbWork())
        } catch (e: Throwable) {
            managerList.forEach { it.undo() }
            Result.failure(e)
        }
    }
}