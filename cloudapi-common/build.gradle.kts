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
    val ktorm_version = "3.4.1"
    api("org.ktorm:ktorm-core:$ktorm_version")
    api("org.ktorm:ktorm-jackson:$ktorm_version")
    api("org.ktorm:ktorm-support-mysql:$ktorm_version")
    api("org.ktorm:ktorm-support-mysql:3.4.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
