import org.hidetake.groovy.ssh.core.RunHandler
import org.hidetake.groovy.ssh.session.SessionHandler
import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.Service
import java.util.Properties
import java.io.FileInputStream
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("org.hidetake.ssh") version "2.10.1"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

val mainPackage = "com.zelgius.gate.debug"
group = mainPackage
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


var raspberry = remotes.create("raspberry") {
    host = "192.168.1.209"
    user = "pi"
    password = getProps("password")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.pi4j:pi4j-core:1.2")
    implementation("com.github.mhashim6:Pi4K:0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.4")
    testImplementation(kotlin("test-junit5"))
    implementation("org.junit.jupiter:junit-jupiter:5.6.2")

    //Firebase
    implementation("com.google.firebase:firebase-admin:7.1.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.11.2")
}


lateinit var jarFile: ShadowJar

tasks {
    named<ShadowJar>("shadowJar") {
        manifest {
            manifest {
                attributes(mapOf("Main-Class" to "$mainPackage.MainKt"))
            }
        }
        archiveVersion .set("1.1-SNAPSHOT")
        archiveBaseName.set("Logger")
        mergeServiceFiles()

        jarFile = this
    }
}


tasks.create("deploy") {
    logging.captureStandardOutput(LogLevel.INFO)
    doLast {
        ssh.runSessions {
            session(raspberry) {
                val archive = jarFile.archiveFile.get().asFile
                val config = Properties()
                config["StrictHostKeyChecking"] = "no"
                try {
                    execute("sudo rm ${archive.name}")
                } catch (e: Exception) {
                    logger.error(e.message)
                }

                logger.lifecycle("Deploying ...")
                put(archive, "/home/pi/")
                logger.lifecycle(execute("sudo pkill -f ${archive.name}"))
                logger.lifecycle(execute("chmod +x ${archive.name}"))
                logger.lifecycle(execute("sudo java -jar ${archive.name}"))
            }
        }
    }
}

tasks.create("copy") {
    doLast {
        ssh.runSessions {
            session(raspberry) {
                val archive = jarFile.archiveFile.get().asFile
                try {
                    execute("sudo rm ${archive.name}")
                } catch (e: Exception) {
                    logger.error(e.message)
                }

               put(archive, "/home/pi/")
                logger.lifecycle(execute("sudo pkill -f ${archive.name}"))
                logger.lifecycle(execute("chmod +x ${archive.name}"))
            }
        }
    }
}


tasks {
    getByName("copy").dependsOn(shadowJar)
    getByName("deploy").dependsOn(shadowJar)
}

tasks.withType(Test::class.java) {
    useJUnitPlatform()
}

fun Service.runSessions(action: RunHandler.() -> Unit) =
    run(delegateClosureOf(action))

fun RunHandler.session(vararg remotes: Remote, action: SessionHandler.() -> Unit) =
    session(*remotes, delegateClosureOf(action))

fun SessionHandler.put(from: Any, into: Any) =
    put(hashMapOf("from" to from, "into" to into))


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
