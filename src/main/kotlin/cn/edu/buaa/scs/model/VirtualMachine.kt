@file:Suppress("unused")

package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.utils.jsonMapper
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import com.vmware.vim25.ManagedEntityStatus
import com.vmware.vim25.VirtualMachinePowerState
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

data class VirtualMachineExtraInfo(
    @JsonProperty("adminID") var adminId: String = "default",
    @JsonProperty("studentID") var studentId: String = "default",
    @JsonProperty("teacherID") var teacherId: String = "default",
    @JsonProperty("isExperimental") var isExperimental: Boolean = false,
    @JsonProperty("experimentID") var experimentId: Int = 0,
    @JsonProperty("applyID") var applyId: String = "default",
) {
    companion object {
        fun valueFromJson(jsonStr: String?): VirtualMachineExtraInfo {
            if (jsonStr == null) return VirtualMachineExtraInfo()
            return try {
                jsonMapper.readValue(jsonStr)
            } catch (e: Exception) {
                VirtualMachineExtraInfo()
            }
        }

        fun valueFromVirtualMachine(vm: VirtualMachine) =
            VirtualMachineExtraInfo(
                vm.adminId,
                vm.studentId,
                vm.teacherId,
                vm.isExperimental,
                vm.experimentId,
                vm.applyId
            )
    }

    fun toJson(): String = jsonMapper.writeValueAsString(this)
}

interface VirtualMachine : Entity<VirtualMachine>, IEntity {
    companion object : Entity.Factory<VirtualMachine>()

    enum class Lifetime {
        USING,
        DELETED
    }

    data class NetInfo(
        val macAddress: String,
        val ipList: List<String>
    )

    // meta
    var uuid: String
    var platform: String // eg: vcenter, kvm, openstack
    var name: String
    var isTemplate: Boolean
    var host: String

    // course related
    var adminId: String
    var studentId: String
    var teacherId: String
    var isExperimental: Boolean
    var experimentId: Int
    var applyId: String

    var lifeTime: Lifetime


    var memory: Int // MB
    var cpu: Int
    var osFullName: String
    var diskNum: Int
    var diskSize: Long  // bytes
    var powerState: VirtualMachinePowerState
    var overallStatus: ManagedEntityStatus
    var netInfos: List<NetInfo>

    fun markDeleted() {
        this.lifeTime = Lifetime.DELETED
    }
}

open class VirtualMachines(alias: String?) : Table<VirtualMachine>("vm", alias) {
    companion object : VirtualMachines(null)
    override fun aliased(alias: String) = VirtualMachines(alias)

    val uuid = varchar("uuid").primaryKey().bindTo { it.uuid }
    val platform = varchar("platform").bindTo { it.platform }
    val name = varchar("name").bindTo { it.name }
    val isTemplate = boolean("is_template").bindTo { it.isTemplate }
    val host = varchar("host").bindTo { it.host }
    val adminId = varchar("admin_id").bindTo { it.adminId }
    val studentId = varchar("student_id").bindTo { it.studentId }
    val teacherId = varchar("teacher_id").bindTo { it.teacherId }
    val isExperimental = boolean("is_experimental").bindTo { it.isExperimental }
    val experimentId = int("experiment_id").bindTo { it.experimentId }
    val applyId = varchar("apply_id").bindTo { it.applyId }
    val lifetime =
        varchar("lifetime").transform({ VirtualMachine.Lifetime.valueOf(it) }, { it.name }).bindTo { it.lifeTime }
    val memory = int("memory").bindTo { it.memory }
    val cpu = int("cpu").bindTo { it.cpu }
    val osFullName = varchar("os_full_name").bindTo { it.osFullName }
    val diskNum = int("disk_num").bindTo { it.diskNum }
    val diskSize = long("disk_size").bindTo { it.diskSize }
    val powerState = varchar("power_state").transform({ VirtualMachinePowerState.fromValue(it) }, { it.value() })
        .bindTo { it.powerState }
    val overallStatus = varchar("overall_status").transform({ ManagedEntityStatus.fromValue(it) }, { it.value() })
        .bindTo { it.overallStatus }
    val netInfos = text("net_info")
        .transform(
            {
                val netInfos: List<VirtualMachine.NetInfo> = jsonMapper.readValue(it)
                netInfos
            },
            { jsonMapper.writeValueAsString(it) })
        .bindTo { it.netInfos }
}

val Database.virtualMachines get() = this.sequenceOf(VirtualMachines)