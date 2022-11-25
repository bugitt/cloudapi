@file:Suppress("unused")

package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.service.IService
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.jsonMapper
import cn.edu.buaa.scs.utils.jsonReadValue
import com.fasterxml.jackson.annotation.JsonIgnore
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.*
import org.ktorm.schema.*

enum class IPProtocol {
    TCP,
    UDP,
    SCTP,
}

interface ContainerService : Entity<ContainerService>, IService {
    companion object : Entity.Factory<ContainerService>() {
        fun id(id: Long): ContainerService {
            return mysql.containerServiceList.find { it.id eq id }
                ?: throw NotFoundException("No such ContainerService($id) in database")
        }
    }

    enum class Type {
        SERVICE,
        JOB
    }

    enum class Status {
        UNDO,
        NOT_READY,
        RUNNING,
        SUCCESS,
        FAIL,
    }

    data class Port(
        val name: String,
        val port: Int,
        val protocol: IPProtocol,
        val exportIP: String? = null,
        val exportPort: Int? = null,
    ) {
        @JsonIgnore
        val exportEndpoint: String? = if (exportIP != null && exportPort != null) "$exportIP:$exportPort" else null
    }

    var id: Long
    var name: String
    var creator: String
    var projectId: Long
    var serviceType: Type
    var createTime: Long
    val containers: List<Container>
        get() = mysql.containerList.filter { it.serviceId eq this.id }.toList()
}

interface Container : Entity<Container> {
    companion object : Entity.Factory<Container>()

    var id: Long
    var name: String
    var serviceId: Long
    var image: String
    var command: String?
    var workingDir: String?
    var envs: Map<String, String>?
    var ports: List<ContainerService.Port>?
    var resourcePoolId: String
    var resourceUsedRecordId: String
}

object ContainerServiceList : Table<ContainerService>("container_service") {
    val id = long("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val creator = varchar("creator").bindTo { it.creator }
    val projectId = long("project_id").bindTo { it.projectId }
    val serviceType =
        varchar("service_type").transform({ ContainerService.Type.valueOf(it) }, { it.toString() })
            .bindTo { it.serviceType }
    val createTime = long("create_time").bindTo { it.createTime }
}

object ContainerList : Table<Container>("container") {
    val id = long("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val serviceId = long("service_id").bindTo { it.serviceId }
    val image = varchar("image").bindTo { it.image }
    val command = varchar("command").bindTo { it.command }
    val workingDir = varchar("working_dir").bindTo { it.workingDir }
    val envs =
        varchar("envs").transform({ jsonReadValue<Map<String, String>>(it) }, { jsonMapper.writeValueAsString(it) })
            .bindTo { it.envs }
    val ports = varchar("ports").transform({ jsonReadValue<List<ContainerService.Port>>(it) },
        { jsonMapper.writeValueAsString(it) }).bindTo { it.ports }
    val resourcePoolId = varchar("resource_pool_id").bindTo { it.resourcePoolId }
    val resourceUsedRecordId = varchar("resource_used_record_id").bindTo { it.resourceUsedRecordId }
}

val Database.containerServiceList get() = this.sequenceOf(ContainerServiceList)

val Database.containerList get() = this.sequenceOf(ContainerList)