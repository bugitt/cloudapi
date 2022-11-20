@file:Suppress("unused")

package cn.edu.buaa.scs.model

import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.newId

data class Resource(
    val cpu: Int, // m, 1000m = 1 core
    val memory: Int, // MB
)

val emptyResource = Resource(0, 0)

data class ResourcePool(
    @BsonId val _id: Id<ResourcePool> = newId(),
    val name: String,
    val ownerId: String,
    val capacity: Resource,
    val used: Resource = emptyResource,
    val usedRecordList: List<Id<ResourceUsedRecord>> = emptyList(),
    val exchangeRecordList: List<Id<ResourceExchangeRecord>> = emptyList(),
)

val CoroutineDatabase.resourcePool get() = getCollection<ResourcePool>()

data class ResourceExchangeRecord(
    @BsonId val _id: Id<ResourceExchangeRecord> = newId(),
    val sender: String,
    val receiver: String,
    val resource: Resource,
    val time: Long = System.currentTimeMillis(),
)

val CoroutineDatabase.resourceExchangeRecord get() = getCollection<ResourceExchangeRecord>()

data class ResourceUsedRecord(
    @BsonId val _id: Id<ResourceUsedRecord> = newId(),
    val resource: Resource,
    val project: Project,
    val containerService: ContainerService,
    val container: Container,
    val time: Long = System.currentTimeMillis(),
)

val CoroutineDatabase.resourceUsedRecord get() = getCollection<ResourceUsedRecord>()
