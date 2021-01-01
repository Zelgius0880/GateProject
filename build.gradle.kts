

buildscript {
    repositories {
        google()
        jcenter()
    }
    val kotlinVersion =  "1.4.21"

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.google.firebase:firebase-plugins:2.0.0")
        classpath ("com.github.jengelman.gradle.plugins:shadow:6.1.0")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}