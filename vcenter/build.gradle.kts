plugins {
    id("java")
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
}

group = "scs.buaa.edu.cn"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("cn.edu.buaa.scs.vcenter.ApplicationKt")
}

dependencies {
    implementation(project(":cloudapi-model"))

    val ktor_version = "2.2.3"
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-jackson:$ktor_version")

    implementation("ch.qos.logback:logback-classic:1.2.11")

    implementation("com.vmware.photon.controller:photon-vsphere-adapter-util:0.6.60")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks {
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "cn.edu.buaa.scs.vcenter.ApplicationKt"))
        }
        isZip64 = true
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    "test"(Test::class) {
        useJUnitPlatform()
    }
}
