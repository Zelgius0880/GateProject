
plugins {
    kotlin("jvm")
}

val mainPackage = "com.zelgius.gate.debug"
group = mainPackage
version = "1.0"


repositories {
    mavenCentral()
    google()
    maven(url = "https://jitpack.io")
}


dependencies {
    implementation(project(":common"))

    testImplementation(kotlin("test-junit5"))
    implementation("org.junit.jupiter:junit-jupiter:5.6.2")
}