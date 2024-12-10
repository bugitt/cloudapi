val kotlin_version: String by project

plugins {
    id("java")
    application
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"

    // 打包用的插件
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "scs.buaa.edu.cn"
version = "0.0.1"

application {
    mainClass.set("cn.edu.buaa.scs.ApplicationKt")
}

repositories {
    maven(url = "https://maven.aliyun.com/repository/public/")
    maven(url = "https://maven.aliyun.com/repository/google/")
    maven(url = "https://maven.aliyun.com/repository/gradle-plugin/")
    maven(url = "https://maven.aliyun.com/repository/jcenter/")
    maven(url = "https://maven.aliyun.com/nexus/content/groups/public/")
    mavenCentral()
    maven(url = "https://www.jitpack.io")
}
