import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.withGroovyBuilder
import java.io.File

data class Remote(
    val host: String,
    val user: String,
    val keyFile: String? = null,
    val password: String? = null,
)

private fun Project.configureAnt() {
    if (configurations.find { it.name == "sshAntTask" } == null) {
        val sshAntTask by configurations.creating
        dependencies {
            sshAntTask("org.apache.ant:ant-jsch:1.10.11")
            sshAntTask("jsch:jsch:0.1.29")
        }

        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "scp",
                "classname" to "org.apache.tools.ant.taskdefs.optional.ssh.Scp",
                "classpath" to sshAntTask.asPath
            )
        }
        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "ssh",
                "classname" to "org.apache.tools.ant.taskdefs.optional.ssh.SSHExec",
                "classpath" to sshAntTask.asPath
            )
        }
    } else {
        logger.warn("Configuration sshAntTask already exists")
    }
}

fun Project.scp(
    file: File,
    dst: String,
    remote: Remote = Remote(
        host = getProps("remote.host"),
        user = getProps("remote.user"),
        keyFile = getProps("rsa_key", default = File(rootDir,"id_rsa").absolutePath)
    ),
    verbose: Boolean = false
) {
    configureAnt()
    ant.withGroovyBuilder {
        "scp"(
            "todir" to "${remote.user}@${remote.host}:$dst",
            (if (remote.keyFile != null) "keyfile" else "password") to
                    (remote.keyFile ?: remote.password),
            "trust" to "yes",
            "verbose" to "$verbose",
            "file" to file
        )
    }
}

fun Project.ssh(
    vararg commands: String,
    remote: Remote = Remote(
        host = getProps("remote.host"),
        user = getProps("remote.user"),
        keyFile = getProps("rsa_key", default = File(rootDir,"id_rsa").absolutePath)
    ),
    verbose: Boolean = false
) {
    configureAnt()

    ant.withGroovyBuilder {
        "ssh"(
            "host" to remote.host,
            "username" to remote.user,
            "command" to commands.joinToString(" && "),
            (if (remote.keyFile != null) "keyfile" else "password") to
                    (remote.keyFile ?: remote.password),
            "trust" to "yes",
            "verbose" to "$verbose",
        )
    }
}