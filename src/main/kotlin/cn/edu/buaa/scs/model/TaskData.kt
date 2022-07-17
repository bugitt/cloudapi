package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.task.Task
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.long
import org.ktorm.schema.text
import org.ktorm.schema.varchar

interface TaskData : Entity<TaskData> {
    companion object : Entity.Factory<TaskData>()

    var id: Long
    var type: Task.Type
    var data: String // json string
    var createTime: Long
    var status: Task.Status
    var error: String
    var updateTime: Long
}

object TaskDataList : Table<TaskData>("task_data") {
    val id = long("id").primaryKey().bindTo { it.id }
    val type = varchar("type").transform({ Task.Type.valueOf(it) }, { it.name }).bindTo { it.type }
    val data = text("data").bindTo { it.data }
    val createTime = long("create_time").bindTo { it.createTime }
    val status = varchar("status").transform({ Task.Status.valueOf(it) }, { it.name }).bindTo { it.status }
    val error = text("error").bindTo { it.error }
    val updateTime = long("update_time").bindTo { it.updateTime }
}

val Database.taskDataList get() = this.sequenceOf(TaskDataList)