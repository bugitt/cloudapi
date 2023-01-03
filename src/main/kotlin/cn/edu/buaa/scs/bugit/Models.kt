package cn.edu.buaa.scs.bugit

import com.fasterxml.jackson.annotation.JsonProperty

data class GitUser(
    val id: Long,
    val username: String,
    val login: String,
    @JsonProperty("full_name") val fullName: String,
    val email: String,
    @JsonProperty("avatar_url") val avatarURL: String,
)

data class CreateOrgRequest(
    val username: String,
    @JsonProperty("full_name") val fullName: String,
    val description: String,
    val website: String,
    val location: String
)

data class CreateRepoRequest(
    val name: String,
    val description: String,
    val private: Boolean,
    @JsonProperty("auto_init") val autoInit: Boolean,
    val gitignores: String,
    val license: String,
    val readme: String = "Default",
)

data class GitProject(
    val id: Long,
    val username: String,
    @JsonProperty("full_name") val fullName: String,
    @JsonProperty("avatar_url") val avatarURL: String,
    val description: String,
    val website: String,
    val location: String,
)

data class GitRepo(
    val id: Long,
    val owner: GitUser,
    val name: String,

    @JsonProperty("full_name")
    val fullName: String,

    val description: String,
    val private: Boolean,
    val fork: Boolean,
    val parent: Any? = null,
    val empty: Boolean,
    val mirror: Boolean,
    val size: Long,

    @JsonProperty("html_url")
    val htmlURL: String,

    @JsonProperty("ssh_url")
    val sshURL: String,

    @JsonProperty("clone_url")
    val cloneURL: String,

    val website: String,

    @JsonProperty("stars_count")
    val starsCount: Long,

    @JsonProperty("forks_count")
    val forksCount: Long,

    @JsonProperty("watchers_count")
    val watchersCount: Long,

    @JsonProperty("open_issues_count")
    val openIssuesCount: Long,

    @JsonProperty("default_branch")
    val defaultBranch: String,

    @JsonProperty("created_at")
    val createdAt: String,

    @JsonProperty("updated_at")
    val updatedAt: String,

    val permissions: RepoPermissions
)

data class RepoPermissions(
    val admin: Boolean,
    val push: Boolean,
    val pull: Boolean
)

data class CreateUserReq(
    @JsonProperty("login_name")
    val loginName: String,

    val username: String,

    @JsonProperty("full_name")
    val fullName: String,

    val email: String,
    val password: String
)
