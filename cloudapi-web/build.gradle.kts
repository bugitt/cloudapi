val kotlin_version: String by project

plugins {
    id("java")
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
}

group = "scs.buaa.edu.cn"
version = "0.0.1"

application {
    mainClass.set("cn.edu.buaa.scs.ApplicationKt")
}

kotlin {
    jvmToolchain(11)
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation(project(":cloudapi-model"))

    val ktor_version = "2.1.3"
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-call-id:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-jackson:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-cio-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktor_version")

    // coroutine
    val coroutine_version = "1.6.4"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutine_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutine_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutine_version")

    // json
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.4")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.13.4")

    // kotlin-log
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    // Redis
    implementation("io.lettuce:lettuce-core:6.1.5.RELEASE")

    // database
    // mysql
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("mysql:mysql-connector-java:8.0.25")
    // mongodb
    val kmongo_version = "4.7.2"
    implementation("org.litote.kmongo:kmongo-coroutine:$kmongo_version")
    implementation("org.litote.kmongo:kmongo-id:$kmongo_version")

    // minio
    implementation("io.minio:minio:8.3.4")

    // tika
    implementation("org.apache.tika:tika-core:2.2.1")
    implementation("org.apache.tika:tika-parsers-standard-package:2.2.1")

    // common utils
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("io.netty:netty-all:4.1.78.Final")
    implementation("io.github.yufeixuan:easy-captcha:1.5.2")
    implementation("org.apache.commons:commons-compress:1.22")


    // kubernetes
    val kubernetes_client_version = "6.2.0"
    implementation("io.fabric8:kubernetes-model:$kubernetes_client_version")
    implementation("io.fabric8:kubernetes-client:$kubernetes_client_version")
    annotationProcessor("io.fabric8:crd-generator-apt:$kubernetes_client_version")
    implementation("com.github.fkorotkov:k8s-kotlin-dsl:3.2.0")
    val javaOperatorSdk = "4.2.6"
    implementation("io.javaoperatorsdk:operator-framework:$javaOperatorSdk")
    annotationProcessor("io.javaoperatorsdk:operator-framework:$javaOperatorSdk")

    // vm
    implementation("com.vmware.photon.controller:photon-vsphere-adapter-util:0.6.60")

    // ssh
    implementation("com.hierynomus:sshj:0.33.0")

    // test
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
}

tasks {
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "cn.edu.buaa.scs.ApplicationKt"))
        }
        isZip64 = true
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }

    "test"(Test::class) {
        useJUnitPlatform()
    }
}
