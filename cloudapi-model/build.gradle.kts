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
    api(project(":cloudapi-common"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
