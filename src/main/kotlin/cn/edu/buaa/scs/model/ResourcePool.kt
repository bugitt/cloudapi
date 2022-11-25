@file:Suppress("unused")

package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.storage.MongoIdFinder
import cn.edu.buaa.scs.storage.mongo
import cn.edu.buaa.scs.utils.IntOrString
import io.fabric8.kubernetes.api.model.Quantity
import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.newId

data class Resource(
    val cpu: Int, // m, 1000m = 1 core
    val memory: Int, // MB
) {
    operator fun minus(other: Resource): Resource {
        return Resource(
            cpu = this.cpu - other.cpu,
            memory = this.memory - other.memory,
        )
    }

    operator fun plus(other: Resource): Resource {
        return Resource(
            cpu = this.cpu + other.cpu,
            memory = this.memory + other.memory,
        )
    }

    fun convertToKubeResourceMap(): Map<String, Quantity> {
        return mapOf(
            "cpu" to Quantity("${cpu}m"),
            "memory" to Quantity("${memory}Mi"),
        )
    }
}

val emptyResource = Resource(0, 0)

data class ResourcePool(
    @BsonId val _id: Id<ResourcePool> = newId(),
    val name: String,
    val ownerId: String,
    val capacity: Resource,
    val used: Resource = emptyResource,
    val usedRecordList: List<Id<ResourceUsedRecord>> = emptyList(),
    val exchangeRecordList: List<Id<ResourceExchangeRecord>> = emptyList(),
    val time: Long = System.currentTimeMillis(),
) : IEntity {
    companion object : MongoIdFinder<ResourcePool> {
        override val col: CoroutineCollection<ResourcePool>
            get() = mongo.getCollection()
    }

    suspend fun use(
        reqResource: Resource,
        project: Project,
        container: Container,
        containerService: ContainerService
    ): Pair<ResourcePool, ResourceUsedRecord> {
        val capacity = this.capacity
        val used = this.used
        if (reqResource.cpu + used.cpu > capacity.cpu || reqResource.memory + used.memory > capacity.memory) {
            throw BadRequestException("${this.name} 资源不足")
        }
        val resourceUsedRecord = ResourceUsedRecord(
            resource = reqResource,
            project = project,
            container = container,
            containerService = containerService,
        )
        mongo.resourceUsedRecord.insertOne(resourceUsedRecord)
        val newResourcePool = this.copy(
            used = this.used + reqResource,
            usedRecordList = this.usedRecordList + resourceUsedRecord._id
        )
        mongo.resourcePool.updateOneById(this._id, newResourcePool)
        return newResourcePool to resourceUsedRecord
    }

    suspend fun release(recordId: String) {
        val resourceUsedRecord = ResourceUsedRecord.id(recordId)
        if (resourceUsedRecord.released) return
        val newRecord = resourceUsedRecord.copy(
            released = true,
        )
        val newResourcePool = this.copy(
            used = this.used - resourceUsedRecord.resource,
            usedRecordList = this.usedRecordList - resourceUsedRecord._id
        )
        mongo.resourceUsedRecord.updateOneById(resourceUsedRecord._id, newRecord)
        mongo.resourcePool.updateOneById(this._id, newResourcePool)
    }

    override fun entityId(): IntOrString {
        return IntOrString(this._id.toString())
    }
}

val CoroutineDatabase.resourcePool get() = getCollection<ResourcePool>()

data class ResourceExchangeRecord(
    @BsonId val _id: Id<ResourceExchangeRecord> = newId(),
    val sender: String,
    val receiver: String,
    val resource: Resource,
    val time: Long = System.currentTimeMillis(),
) {
    companion object : MongoIdFinder<ResourceExchangeRecord> {
        override val col: CoroutineCollection<ResourceExchangeRecord>
            get() = mongo.getCollection()
    }
}

val CoroutineDatabase.resourceExchangeRecord get() = getCollection<ResourceExchangeRecord>()

data class ResourceUsedRecord(
    @BsonId val _id: Id<ResourceUsedRecord> = newId(),
    val resource: Resource,
    val project: Project,
    val containerService: ContainerService,
    val container: Container,
    val released: Boolean = false,
    val time: Long = System.currentTimeMillis(),
) {
    companion object : MongoIdFinder<ResourceUsedRecord> {
        override val col: CoroutineCollection<ResourceUsedRecord>
            get() = mongo.getCollection()
    }
}

val CoroutineDatabase.resourceUsedRecord get() = getCollection<ResourceUsedRecord>()
