import java.util.Properties
import java.io.FileInputStream
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "com.zelgius.gateController"
version = "1.0-SNAPSHOT"

/*
application {
    mainClass.set("$mainPackage.MainKt")
}
*/

repositories {
    mavenCentral()
    google()
    maven(url = "https://jitpack.io")
}

val mainPackage = "com.zelgius.gateController"
group = mainPackage


var raspberry = Remote(
    host = "192.168.1.123",
    user = "pi",
    password = getProps("password")
)

val pi4jVersion = "2.1.1"
val slf4jVersion = "1.7.36"
dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.pi4j:pi4j-core:$pi4jVersion")
    implementation("com.pi4j:pi4j-plugin-raspberrypi:$pi4jVersion")
    implementation("com.pi4j:pi4j-plugin-pigpio:$pi4jVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    testImplementation(kotlin("test-junit5"))
    implementation("org.junit.jupiter:junit-jupiter:5.9.0")

    //Firebase
    implementation("com.google.firebase:firebase-admin:9.0.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.18.0")

    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
}


lateinit var jarFile: ShadowJar

tasks {
    named<ShadowJar>("shadowJar") {
        manifest {
            manifest {
                attributes(mapOf("Main-Class" to "$mainPackage.MainKt"))
            }
        }
        archiveVersion.set("1.2-SNAPSHOT")
        archiveBaseName.set("GateController")
        mergeServiceFiles()

        jarFile = this
    }
}


tasks.create("deploy") {
    logging.captureStandardOutput(LogLevel.INFO)
    doLast {
        val archive = jarFile.archiveFile.get().asFile

        try {
            ssh("sudo rm ${archive.name}",  remote = raspberry)
        } catch (e: Exception) {
            logger.error(e.message)
        }

        logger.lifecycle("Deploying ...")
        scp(file(getProps("firebase_admin_file")), "/home/pi/",  remote = raspberry)
        scp(archive, "/home/pi/",  remote = raspberry)
        ssh("sudo pkill -f ${archive.name}",  remote = raspberry)
        ssh("chmod +x ${archive.name}", remote =  raspberry)
        ssh("sudo java -jar ${archive.name}",  remote = raspberry)
    }
}

tasks.create("copy") {
    doLast {
        val archive = jarFile.archiveFile.get().asFile
        try {
            ssh("sudo rm ${archive.name}",  remote = raspberry)
        } catch (e: Exception) {
            logger.error(e.message)
        }

        scp(file(getProps("firebase_admin_file")), "/home/pi/", remote = raspberry)
        scp(archive, "/home/pi/",  remote = raspberry)
        ssh("sudo pkill -f ${archive.name}",  remote = raspberry)
        ssh("chmod +x ${archive.name}",  remote = raspberry)
    }
}


tasks {
    getByName("copy").dependsOn(shadowJar)
    getByName("deploy").dependsOn(shadowJar)
}

tasks.withType(Test::class.java) {
    useJUnitPlatform()
}


fun getProps(propName: String): String {
    val propsFile = rootProject.file("local.properties")
    return if (propsFile.exists()) {
        val props = Properties()
        props.load(FileInputStream(propsFile))
        props[propName] as String
    } else {
        ""
    }
}
