plugins {
    kotlin("jvm")
    id("java-library")
}

group = "com.zelgius.gateController"
version = "1.0"

repositories {
    mavenCentral()
}

val slf4jVersion = "1.7.36"
dependencies {

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    api("org.junit.jupiter:junit-jupiter:5.9.0")

    //Firebase
    api("com.google.firebase:firebase-admin:9.2.0")
    api("org.apache.logging.log4j:log4j-slf4j-impl:2.18.0")

    api("org.slf4j:slf4j-api:$slf4jVersion")
    api("org.slf4j:slf4j-simple:$slf4jVersion")
}

tasks.test {
    useJUnitPlatform()
}