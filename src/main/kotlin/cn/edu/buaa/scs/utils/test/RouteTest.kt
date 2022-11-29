package cn.edu.buaa.scs.utils.test

import cn.edu.buaa.scs.model.ContainerServiceTemplate
import cn.edu.buaa.scs.model.IPProtocol
import cn.edu.buaa.scs.model.containerServiceTemplateList
import cn.edu.buaa.scs.storage.mongo
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.commons.lang3.RandomStringUtils

fun Route.test() {
    // just for test
    route("/test") {
        get {
            val template1 = ContainerServiceTemplate(
                name = "MySQL",
                category = "数据库",
                segment = "关系型数据库",
                baseImage = "docker.io/library/mysql",
                portList = listOf(ContainerServiceTemplate.Port(IPProtocol.TCP, 3306)),
                iconUrl = "https://scs.buaa.edu.cn/scsos/public/40d934d3-7d8f-4af4-ac7c-1df83b28113b-mysql.svg",
                configs = listOf(
                    ContainerServiceTemplate.ConfigItem(
                        name = "tag",
                        label = "版本",
                        type = ContainerServiceTemplate.ConfigItem.ValueType.STRING,
                        description = "MySQL的版本",
                        required = true,
                        default = "8",
                        options = listOf("5.7", "8", "latest"),
                        target = ContainerServiceTemplate.ConfigItem.Target.TAG
                    ),
                    ContainerServiceTemplate.ConfigItem(
                        name = "MYSQL_ROOT_PASSWORD",
                        label = "root密码",
                        type = ContainerServiceTemplate.ConfigItem.ValueType.STRING,
                        description = "root用户的密码",
                        required = true,
                        default = RandomStringUtils.randomAlphanumeric(10),
                        target = ContainerServiceTemplate.ConfigItem.Target.ENV
                    ),
                    ContainerServiceTemplate.ConfigItem(
                        name = "MYSQL_DATABASE",
                        label = "数据库名",
                        type = ContainerServiceTemplate.ConfigItem.ValueType.STRING,
                        description = "创建的数据库名",
                        required = true,
                        default = "test",
                        target = ContainerServiceTemplate.ConfigItem.Target.ENV
                    ),
                ),
            )
            val template2 = ContainerServiceTemplate(
                name = "Redis",
                category = "数据库",
                segment = "键值型数据库",
                description = "Redis是一个开源的使用ANSI C语言编写、支持网络、可基于内存亦可持久化的日志型、Key-Value数据库，并提供多种语言的API。",
                baseImage = "docker.io/bitnami/redis",
                portList = listOf(ContainerServiceTemplate.Port(IPProtocol.TCP, 6379)),
                iconUrl = "https://scs.buaa.edu.cn/scsos/public/b5aef802-fdb4-4ae7-bfc2-cfb5bce7d984-redis.svg",
                configs = listOf(
                    ContainerServiceTemplate.ConfigItem(
                        name = "tag",
                        label = "版本",
                        type = ContainerServiceTemplate.ConfigItem.ValueType.STRING,
                        description = "Redis的版本",
                        required = true,
                        default = "latest",
                        options = listOf("7.0.5", "6.2", "5.0.14", "latest"),
                        target = ContainerServiceTemplate.ConfigItem.Target.TAG
                    ),
                    ContainerServiceTemplate.ConfigItem(
                        name = "REDIS_PASSWORD",
                        label = "密码",
                        type = ContainerServiceTemplate.ConfigItem.ValueType.STRING,
                        description = "Redis的密码",
                        default = RandomStringUtils.randomAlphanumeric(10),
                        required = true,
                        target = ContainerServiceTemplate.ConfigItem.Target.ENV
                    ),
                ),
            )
            val template3 = ContainerServiceTemplate(
                name = "MongoDB",
                category = "数据库",
                segment = "文档型数据库",
                baseImage = "docker.io/bitnami/mongodb",
                description = "MongoDB是一个基于分布式文件存储的数据库。由C++编写。旨在为WEB应用提供可扩展的高性能数据存储解决方案。",
                iconUrl = "https://scs.buaa.edu.cn/scsos/public/7b7c9122-e73e-45b2-a665-3d0c2f4ae966-mongodb.svg",
                portList = listOf(ContainerServiceTemplate.Port(IPProtocol.TCP, 27017)),
                configs = listOf(
                    ContainerServiceTemplate.ConfigItem(
                        name = "tag",
                        label = "版本",
                        type = ContainerServiceTemplate.ConfigItem.ValueType.STRING,
                        description = "MongoDB的版本",
                        required = true,
                        default = "latest",
                        options = listOf("6.0.3", "5.0.14", "4.2.21", "latest"),
                        target = ContainerServiceTemplate.ConfigItem.Target.TAG
                    ),
                    ContainerServiceTemplate.ConfigItem(
                        name = "MONGODB_DATABASE",
                        label = "数据库名",
                        type = ContainerServiceTemplate.ConfigItem.ValueType.STRING,
                        description = "创建的数据库名",
                        required = true,
                        default = "test",
                        target = ContainerServiceTemplate.ConfigItem.Target.ENV
                    ),
                    ContainerServiceTemplate.ConfigItem(
                        name = "MONGODB_USERNAME",
                        label = "用户名",
                        description = "该用户拥有访问上述数据库的权限",
                        type = ContainerServiceTemplate.ConfigItem.ValueType.STRING,
                        required = true,
                        default = "mongoadmin",
                        target = ContainerServiceTemplate.ConfigItem.Target.ENV
                    ),
                    ContainerServiceTemplate.ConfigItem(
                        name = "MONGODB_PASSWORD",
                        label = "密码",
                        type = ContainerServiceTemplate.ConfigItem.ValueType.STRING,
                        description = "上述用户的密码",
                        required = true,
                        default = RandomStringUtils.randomAlphanumeric(10),
                        target = ContainerServiceTemplate.ConfigItem.Target.ENV
                    ),
                ),
            )
            mongo.containerServiceTemplateList.insertOne(template1)
            mongo.containerServiceTemplateList.insertOne(template2)
            mongo.containerServiceTemplateList.insertOne(template3)
            call.respond("OK")
        }
    }
}
