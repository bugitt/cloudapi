@file:Suppress("unused")

package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.storage.MongoIdFinder
import cn.edu.buaa.scs.storage.mongo
import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.newId

data class ContainerServiceTemplate(
    @BsonId val _id: Id<ContainerServiceTemplate> = newId(),
    val name: String,
    val category: String,
    val segment: String? = null,
    val baseImage: String,
    val description: String = "",
    val configs: List<ConfigItem>,
    val portList: List<Port> = listOf(),
    val iconUrl: String? = null,
) {
    data class Port(
        val protocol: IPProtocol,
        val port: Int,
    )

    class ConfigItem(
        val name: String,
        val label: String,
        val type: ValueType,
        val description: String,
        val required: Boolean,
        val default: String? = null,
        val options: List<String> = listOf(),
        val allowCustom: Boolean = false,
        val target: Target,
    ) {
        enum class ValueType {
            STRING, NUMBER, BOOLEAN
        }

        enum class Target {
            TAG, ENV
        }
    }

    companion object : MongoIdFinder<ContainerServiceTemplate> {
        override val col: CoroutineCollection<ContainerServiceTemplate>
            get() = mongo.containerServiceTemplateList
    }
}

val CoroutineDatabase.containerServiceTemplateList
    get() = getCollection<ContainerServiceTemplate>()

