val kotlin_version: String by project

plugins {
    application
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"

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
    maven(url = "https://jitpack.io")
}

dependencies {
    val ktor_version = "1.6.8"
    // ktor server
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-websockets:$ktor_version")

    // ktor client
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")

    // coroutine
    val coroutine_version = "1.6.4"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutine_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutine_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutine_version")

    // kotlin-log
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    // auth
    implementation("io.ktor:ktor-auth:$ktor_version")

    // serialization
    implementation("io.ktor:ktor-jackson:$ktor_version")

    // Redis
    implementation("io.lettuce:lettuce-core:6.1.5.RELEASE")

    // database
    val ktorm_version = "3.4.1"
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.ktorm:ktorm-core:$ktorm_version")
    implementation("org.ktorm:ktorm-jackson:$ktorm_version")
    implementation("org.ktorm:ktorm-support-mysql:$ktorm_version")
    implementation("org.ktorm:ktorm-support-mysql:3.4.1")
    implementation("mysql:mysql-connector-java:8.0.25")

    // minio
    implementation("io.minio:minio:8.3.4")

    // tika
    implementation("org.apache.tika:tika-core:2.2.1")
    implementation("org.apache.tika:tika-parsers-standard-package:2.2.1")

    // common utils
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("io.netty:netty-all:4.1.78.Final")

    // kubernetes
    val kubernetes_client_version = "5.12.1"
    implementation("io.fabric8:kubernetes-model:$kubernetes_client_version")
    implementation("io.fabric8:kubernetes-client:$kubernetes_client_version")
    implementation("com.github.fkorotkov:k8s-kotlin-dsl:3.1.1")

    // vm
//    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("com.vmware.photon.controller:photon-vsphere-adapter-util:0.6.60")

    // test
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks {
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "cn.edu.buaa.scs.ApplicationKt"))
        }
        isZip64 = true
    }

    "test"(Test::class) {
        useJUnitPlatform()
    }
}