package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.utils.IntOrString
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

enum class StoreType {
    @Suppress("unused")
    S3
}

sealed interface FileType {
    val name: String

    companion object {
        fun valueOf(name: String): FileType {
            return when (name) {
                "Assignment" -> Assignment
                "CourseResource" -> CourseResource
                "ExperimentResource" -> ExperimentResource
                else -> throw IllegalArgumentException("Unknown file type: $name")
            }
        }
    }

    object Assignment : FileType {
        override val name: String
            get() = "Assignment"
    }

    sealed interface Resource : FileType

    object CourseResource : Resource {
        override val name: String
            get() = "CourseResource"
    }

    object ExperimentResource : Resource {
        override val name: String
            get() = "ExperimentResource"
    }
}

interface File : Entity<File>, IEntity {
    companion object : Entity.Factory<File>()

    var id: Int
    var name: String
    var storeType: StoreType
    var storeName: String
    var storePath: String
    var uploadTime: Long
    var fileType: FileType
    var involvedId: Int
    var size: Long
    var uploader: String
    var contentType: String
    var owner: String

    var createdAt: Long
    var updatedAt: Long

    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}

object Files : Table<File>("file_v2") {
    @Suppress("unused")
    val id = int("id").primaryKey().bindTo { it.id }

    @Suppress("unused")
    val name = varchar("name").bindTo { it.name }

    @Suppress("unused")
    val storeType = varchar("store_type").transform({ StoreType.valueOf(it) }, { it.name }).bindTo { it.storeType }

    @Suppress("unused")
    val storeName = varchar("store_name").bindTo { it.storeName }

    @Suppress("unused")
    val storePath = varchar("store_path").bindTo { it.storePath }

    @Suppress("unused")
    val uploadTime = long("upload_time").bindTo { it.uploadTime }

    @Suppress("unused")
    val fileType = varchar("file_type").transform({ FileType.valueOf(it) }, { it.name }).bindTo { it.fileType }

    @Suppress("unused")
    val involvedId = int("involved_id").bindTo { it.involvedId }

    @Suppress("unused")
    val size = long("size").bindTo { it.size }

    @Suppress("unused")
    val contentType = varchar("content_type").bindTo { it.contentType }

    @Suppress("unused")
    val uploader = varchar("uploader").bindTo { it.uploader }

    @Suppress("unused")
    val owner = varchar("owner").bindTo { it.owner }

    @Suppress("unused")
    val createdAt = long("created_at").bindTo { it.createdAt }

    @Suppress("unused")
    val updatedAt = long("updated_at").bindTo { it.updatedAt }
}

val Database.files get() = this.sequenceOf(Files)