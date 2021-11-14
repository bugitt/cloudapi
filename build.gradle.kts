val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.31"

    // 打包用的插件
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "scs.buaa.edu.cn"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")

    // coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.5.2")

    // kotlin-log
    implementation("io.github.microutils:kotlin-logging:2.0.11")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // auth
    implementation("io.ktor:ktor-auth:$ktor_version")

    // serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation("io.ktor:ktor-jackson:$ktor_version")

    // Redis
    implementation("io.lettuce:lettuce-core:6.1.5.RELEASE")

    // database
    val ktorm_version = "3.4.1"
    implementation("com.zaxxer:HikariCP:5.0.0")
    implementation("org.ktorm:ktorm-core:$ktorm_version")
    implementation("org.ktorm:ktorm-jackson:$ktorm_version")
    implementation("org.ktorm:ktorm-support-mysql:$ktorm_version")
    implementation("org.ktorm:ktorm-support-mysql:3.4.1")
    implementation("mysql:mysql-connector-java:8.0.25")

    // common utils
    implementation("com.google.guava:guava:31.0.1-jre")


    // test
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

tasks {
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "cn.edu.buaa.scs.ApplicationKt"))
        }
    }
}