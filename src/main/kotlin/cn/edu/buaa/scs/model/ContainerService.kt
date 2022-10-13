@file:Suppress("unused")

package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.service.IService
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.jsonMapper
import cn.edu.buaa.scs.utils.jsonReadValue
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.Entity
import org.ktorm.entity.filter
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import org.ktorm.schema.*

enum class IPProtocol {
    TCP,
    UDP,
    SCTP,
}

interface ContainerService : Entity<ContainerService>, IService {
    companion object : Entity.Factory<ContainerService>()

    enum class Type {
        SERVICE,
        JOB
    }

    data class Port(
        val name: String,
        val port: Int,
        val protocol: IPProtocol,
        val exportIP: String,
        val exportPort: Int,
    ) {
        val exportEndpoint: String
            get() = "$exportIP:$exportPort"
    }

    var id: Long
    var name: Long
    var projectId: Long
    var serviceType: Type
    var containers: List<Container>

    fun fillContainers() {
        this.containers = mysql.containerList.filter { it.serviceId eq this.id }.toList()
    }
}

interface Container : Entity<Container> {
    companion object : Entity.Factory<Container>()

    var id: Long
    var serviceId: Long
    var image: String
    var command: String
    var workingDir: String
    var envs: Map<String, String>
    var ports: List<ContainerService.Port>
}

object ContainerServiceList : Table<ContainerService>("container_service") {
    val id = long("id").primaryKey().bindTo { it.id }
    val name = long("name").bindTo { it.name }
    val projectId = long("project_id").bindTo { it.projectId }
    val serviceType =
        varchar("service_type").transform({ ContainerService.Type.valueOf(it) }, { it.toString() })
            .bindTo { it.serviceType }
}

object ContainerList : Table<Container>("container") {
    val id = long("id").primaryKey().bindTo { it.id }
    val serviceId = long("service_id").bindTo { it.serviceId }
    val image = varchar("image").bindTo { it.image }
    val command = varchar("command").bindTo { it.command }
    val workingDir = varchar("working_dir").bindTo { it.workingDir }
    val envs =
        varchar("envs").transform({ jsonReadValue<Map<String, String>>(it) }, { jsonMapper.writeValueAsString(it) })
            .bindTo { it.envs }
    val ports = varchar("ports").transform({ jsonReadValue<List<ContainerService.Port>>(it) },
        { jsonMapper.writeValueAsString(it) }).bindTo { it.ports }
}

val Database.containerServiceList get() = this.sequenceOf(ContainerServiceList)

val Database.containerList get() = this.sequenceOf(ContainerList)