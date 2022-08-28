repositories {
    // The org.jetbrains.kotlin.jvm plugin requires a repository
    // where to download the Kotlin compiler dependencies from.
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")

}

plugins {
    `kotlin-dsl`
}
