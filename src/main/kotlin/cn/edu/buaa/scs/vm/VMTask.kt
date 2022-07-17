package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.model.TaskData
import cn.edu.buaa.scs.task.Task
import cn.edu.buaa.scs.utils.jsonMapper
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue

class VMTask(taskData: TaskData) : Task(taskData) {

    enum class Type {
        Create, Delete
    }

    data class Content(
        @JsonProperty("type") var type: Type,
        @JsonProperty("data") var data: String,
    )

    companion object {
        private fun commonVMTask() = TaskData {
            this.type = Task.Type.VirtualMachine
            this.createTime = System.currentTimeMillis()
            this.status = Task.Status.UNDO
        }

        fun vmCreateTask(options: CreateVmOptions): TaskData {
            val taskData = commonVMTask()
            taskData.data = jsonMapper.writeValueAsString(Content(Type.Create, jsonMapper.writeValueAsString(options)))
            return taskData
        }

        fun vmDeleteTask(uuid: String): TaskData {
            val taskData = commonVMTask()
            taskData.data = jsonMapper.writeValueAsString(Content(Type.Delete, uuid))
            return taskData
        }
    }

    override suspend fun internalProcess(): Result<Unit> {
        val content = jsonMapper.readValue<Content>(taskData.data)
        return when (content.type) {
            Type.Create -> {
                val options = jsonMapper.readValue<CreateVmOptions>(content.data)
                vmClient.createVM(options).map { Unit }
            }
            Type.Delete -> {
                vmClient.deleteVM(content.data)
            }
        }
    }

}