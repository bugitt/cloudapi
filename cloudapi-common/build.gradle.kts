plugins {
    id("java")

    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "scs.buaa.edu.cn"
version = "0.0.1"

kotlin {
    jvmToolchain(8)
}

repositories {
    mavenCentral()
}

dependencies {
    val ktor_version = "2.1.3"
    // ktor client
    api("io.ktor:ktor-client-core-jvm:$ktor_version")
    api("io.ktor:ktor-client-cio-jvm:$ktor_version")
    api("io.ktor:ktor-client-content-negotiation:$ktor_version")

    // mysql
    val ktorm_version = "3.4.1"
    api("org.ktorm:ktorm-core:$ktorm_version")
    api("org.ktorm:ktorm-jackson:$ktorm_version")
    api("org.ktorm:ktorm-support-mysql:$ktorm_version")
    api("org.ktorm:ktorm-support-mysql:3.4.1")

    // kotlin-log
    api("io.github.microutils:kotlin-logging:2.1.21")
    api("ch.qos.logback:logback-classic:1.2.11")

    // kubernetes
    val kubernetes_client_version = "6.2.0"
    api("io.fabric8:kubernetes-model:$kubernetes_client_version")
    api("io.fabric8:kubernetes-client:$kubernetes_client_version")

    // coroutine
    val coroutine_version = "1.6.4"
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutine_version")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutine_version")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutine_version")

    // mongodb
    val kmongo_version = "4.7.2"
    api("org.litote.kmongo:kmongo-coroutine:$kmongo_version")
    api("org.litote.kmongo:kmongo-id:$kmongo_version")

    // json
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.4")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.13.4")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
