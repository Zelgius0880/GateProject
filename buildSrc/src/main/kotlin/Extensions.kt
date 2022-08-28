import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.extra
import java.io.File
import java.io.FileInputStream
import java.util.*


fun Project.getProps(propName: String, project: Project = rootProject, default: String = ""): String {
    val propsFile = project.file("local.properties")
    return if (propsFile.exists()) {
        val props = Properties()
        props.load(FileInputStream(propsFile))
        (props[propName] as String?)?: default
    } else {
        default
    }
}

fun Project.setupJavaDeployTasks(file: File, shadowJar: TaskProvider<*>) {
    val copy = tasks.create("copy") {
        doLast {
            scp(file, "/home/pi")
            ssh("chmod +x ${file.name}", "sudo pkill -f ${file.name}")
        }
    }.also {
        it.dependsOn(shadowJar)
    }

    tasks.create("deploy") {
        doLast {
            ssh( "sudo java -jar ${file.name}")
        }
    }.dependsOn(copy)

}