@file:Suppress("unused")

package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

interface ExperimentWorkflowConfiguration : Entity<ExperimentWorkflowConfiguration>, IEntity {
    companion object : Entity.Factory<ExperimentWorkflowConfiguration>()

    var id: Long
    var expId: Int
    var name: String
    var studentIdList: List<String>
    var resourcePool: String
    var configuration: String
}

object ExperimentWorkflowConfigurations : Table<ExperimentWorkflowConfiguration>("experiment_workflow_configuration") {
    val id = long("id").primaryKey().bindTo { it.id }
    val expId = int("exp_id").bindTo { it.expId }
    val name = varchar("name").bindTo { it.name }
    val studentIdList = varchar("student_list").transform({ listStr ->
        listStr.split(",").map { it.trim() }.filterNot { it.isBlank() }
    }, { it.joinToString(",") }).bindTo { it.studentIdList }
    val resourcePool = varchar("resource_pool").bindTo { it.resourcePool }
    val configuration = varchar("configuration").bindTo { it.configuration }
}

val Database.experimentWorkflowConfigurations get() = this.sequenceOf(ExperimentWorkflowConfigurations)
