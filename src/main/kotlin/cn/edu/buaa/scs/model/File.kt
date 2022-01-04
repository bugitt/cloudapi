package cn.edu.buaa.scs.model

import org.ktorm.entity.Entity
import org.ktorm.schema.*
import java.time.LocalDateTime

enum class StoreType {
    @Suppress("unused")
    S3
}

enum class FileType {
    @Suppress("unused")
    Assignment
}

interface File : Entity<File> {
    companion object : Entity.Factory<File>()

    var id: Int
    var name: String
    var storeType: StoreType
    var storeName: String
    var storePath: String
    var uploadTime: LocalDateTime
    var fileType: FileType
    var size: Long
    var uploader: String
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
    val uploadTime = datetime("upload_time").bindTo { it.uploadTime }

    @Suppress("unused")
    val fileType = varchar("file_type").transform({ FileType.valueOf(it) }, { it.name }).bindTo { it.fileType }

    @Suppress("unused")
    val size = long("size").bindTo { it.size }

    @Suppress("unused")
    val uploader = varchar("uploader").bindTo { it.uploader }
}