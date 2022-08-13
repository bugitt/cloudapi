package cn.edu.buaa.scs.task

import cn.edu.buaa.scs.model.TaskData
import cn.edu.buaa.scs.model.taskDataList
import cn.edu.buaa.scs.storage.mysql
import org.ktorm.entity.update

abstract class Task(protected val taskData: TaskData) {
    enum class Status {
        UNDO, DOING, SUCCESS, FAIL
    }

    enum class Type {
        VirtualMachine
    }

    protected abstract suspend fun internalProcess(): Result<Unit>

    suspend fun process(): Result<Unit> {
        taskData.doing()
        try {
            internalProcess().getOrThrow()
        } catch (e: Throwable) {
            taskData.fail(e.stackTraceToString())
            return Result.failure(e)
        }
        taskData.success()
        return Result.success(Unit)
    }
}

fun TaskData.doing() {
    this.status = Task.Status.DOING
    this.updateTime = System.currentTimeMillis()
    mysql.taskDataList.update(this)
}

fun TaskData.success() {
    this.status = Task.Status.SUCCESS
    this.updateTime = System.currentTimeMillis()
    mysql.taskDataList.update(this)
}

fun TaskData.fail(errMsg: String) {
    this.status = Task.Status.FAIL
    this.error = errMsg
    this.updateTime = System.currentTimeMillis()
    mysql.taskDataList.update(this)
}