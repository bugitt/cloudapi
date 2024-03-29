@file:Suppress("unused")

package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.utils.jsonMapper
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.readValue
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

fun VirtualMachine.applyExtraInfo(extraInfo: VirtualMachineExtraInfo) {
    this.adminId = extraInfo.adminId
    this.studentId = extraInfo.studentId
    this.teacherId = extraInfo.teacherId
    this.isExperimental = extraInfo.experimental
    this.experimentId = extraInfo.experimentId
    this.applyId = extraInfo.applyId
    this.templateUuid = extraInfo.templateUuid
    this.initial = extraInfo.initial
}

fun VirtualMachine.applySangforExtraInfo(extraInfo: String) {
    val info = extraInfo.split(',')
    if (info.size == 4) {
        if (info[0] == "default") {
            this.adminId = "default"
            this.studentId = "default"
            this.teacherId = "default"
        } else if (info[0].length == 5) {
            this.adminId = "default"
            this.studentId = "default"
            this.teacherId = info[0]
        } else {
            this.adminId = "default"
            this.studentId = info[0]
            this.teacherId = "default"
        }
        this.isTemplate = info[1].toBoolean()
        this.isExperimental = info[2].toInt() != 0
        this.experimentId = info[2].toInt()
        this.applyId = info[3]
    } else {
        this.adminId = "default"
        this.studentId = "default"
        this.teacherId = "default"
        this.isExperimental = false
        this.isTemplate = false
        this.experimentId = 0
        this.applyId = "default"
    }
    this.templateUuid = "default"
    this.initial = false
}

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonDeserialize(using = JsonDeserializer.None::class)
data class VirtualMachineExtraInfo(
    @JsonProperty("adminID") val adminId: String = "default",
    @JsonProperty("studentID") val studentId: String = "default",
    @JsonProperty("teacherID") val teacherId: String = "default",
    @JsonProperty("experimental") @JsonAlias("isExperimental") val experimental: Boolean = false,
    @JsonProperty("experimentID") val experimentId: Int = 0,
    @JsonProperty("applyID") val applyId: String = "default",
    @JsonProperty("templateUuid") val templateUuid: String = "default",
    @JsonProperty("initial") val initial: Boolean = false,
) {
    companion object {
        fun valueFromVirtualMachine(vm: VirtualMachine) =
            VirtualMachineExtraInfo(
                vm.adminId,
                vm.studentId,
                vm.teacherId,
                vm.isExperimental,
                vm.experimentId,
                vm.applyId,
                vm.templateUuid,
                vm.initial,
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

    enum class PowerState(val value: String) {
        PoweredOff("poweredOff"),
        PoweredOn("poweredOn"),
        Suspended("suspended");

        companion object {
            fun from(v: String): PowerState =
                when (v.lowercase()) {
                    "poweredoff" -> PoweredOff
                    "poweredon" -> PoweredOn
                    "suspended" -> Suspended
                    else -> PoweredOff
                }

        }
    }

    enum class OverallStatus(val value: String) {
        Green("green"),
        Yellow("yellow"),
        Red("red"),
        Gray("gray");

        companion object {
            fun from(v: String): OverallStatus =
                when (v.lowercase()) {
                    "green" -> Green
                    "yellow" -> Yellow
                    "red" -> Red
                    "gray" -> Gray
                    else -> Gray
                }
        }

    }

    data class NetInfo(
        @JsonProperty("macAddress") val macAddress: String,
        @JsonProperty("ipList") val ipList: List<String> = listOf(),
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
    var templateUuid: String
    var initial: Boolean // initial in vm apply

    var lifeTime: Lifetime


    var memory: Int // MB
    var cpu: Int
    var osFullName: String
    var diskNum: Int
    var diskSize: Long  // bytes
    var powerState: PowerState
    var overallStatus: OverallStatus
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
    val powerState = varchar("power_state").transform({ VirtualMachine.PowerState.from(it) }, { it.value })
        .bindTo { it.powerState }
    val overallStatus = varchar("overall_status").transform({ VirtualMachine.OverallStatus.from(it) }, { it.value })
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
