package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.jsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.Entity
import org.ktorm.entity.count
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

interface VmApply : Entity<VmApply>, IEntity {
    companion object : Entity.Factory<VmApply>() {
        fun id(id: String): VmApply? = mysql.vmApplyList.find { it.id.eq(id) }
    }

    var id: String
    var namePrefix: String
    var studentId: String
    var teacherId: String
    var experimentId: Int
    var studentIdList: List<String>
    var cpu: Int
    var memory: Int // MB
    var diskSize: Long // bytes
    var templateUuid: String
    var description: String
    var applyTime: Long
    var status: Int // 0: 还未处理; 1: 允许; 2: 拒绝
    var handleTime: Long
    var expectedNum: Int
    var replyMsg: String
    var dueTime: Long

    var done: Boolean   // 是否完成所有虚拟机的创建

    fun isApproved(): Boolean = this.status == 1

    fun getActualNum(): Int = mysql.virtualMachines.count { it.applyId.eq(this.id) }

    fun getApplicant(): String {
        return if (studentId == "default" && teacherId == "default") "管理员"
        else if (studentId == "default") mysql.users.find { it.id.eq(this.teacherId) }?.name ?: ""
        else mysql.users.find { it.id.eq(this.studentId) }?.name ?: ""
    }

    fun getTemplateName(): String = mysql.virtualMachines.find { it.uuid.eq(this.templateUuid) }?.name ?: ""
}

object VmApplyList : Table<VmApply>("vm_apply") {
    val id = varchar("id").primaryKey().bindTo { it.id }
    val namePrefix = varchar("name_prefix").bindTo { it.namePrefix }
    val studentId = varchar("student_id").bindTo { it.studentId }
    val teacherId = varchar("teacher_id").bindTo { it.teacherId }
    val experimentId = int("experiment_id").bindTo { it.experimentId }
    val studentIdList = text("student_id_list")
        .transform({ jsonMapper.readValue<List<String>>(it) }, { jsonMapper.writeValueAsString(it) })
        .bindTo { it.studentIdList }
    val cpu = int("cpu").bindTo { it.cpu }
    val memory = int("memory").bindTo { it.memory }
    val diskSize = long("disk_size").bindTo { it.diskSize }
    val templateUuid = varchar("template_uuid").bindTo { it.templateUuid }
    val description = text("description").bindTo { it.description }
    val applyTime = long("apply_time").bindTo { it.applyTime }
    val status = int("status").bindTo { it.status }
    val handleTime = long("handle_time").bindTo { it.handleTime }
    val exceptedNum = int("expected_num").bindTo { it.expectedNum }
    var replyMsg = text("reply_msg").bindTo { it.replyMsg }
    var dueTime = long("due_time").bindTo { it.dueTime }

    var done = boolean("done").bindTo { it.done }
}

val Database.vmApplyList get() = this.sequenceOf(VmApplyList)
