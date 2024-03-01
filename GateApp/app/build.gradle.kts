import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("com.google.firebase.crashlytics")
    id("com.google.gms.google-services")
    id("dagger.hilt.android.plugin")
}

android {
    compileSdk = 34
    buildFeatures {
        // Enables Jetpack Compose for this module
        compose = true
        buildConfig = true
    }
    defaultConfig {
        minSdk = 24
        targetSdk = 34
        versionCode = 8
        versionName = "2.3.0"

        //testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 2) Connect JUnit 5 to the runner
        buildConfigField("double", "LATITUDE", "50.13829924386458")
        buildConfigField("double", "LONGITUDE", "5.2771781296775035")

    }

    buildTypes {

        debug {
            buildConfigField(
                "String",
                "AP_SSID",
                gradleLocalProperties(rootDir).getProperty("esp8266.ap.ssid")
            )
            buildConfigField(
                "String",
                "AP_PASSWORD",
                gradleLocalProperties(rootDir).getProperty("esp8266.ap.password")
            )
            applicationIdSuffix = ".debug"

            buildConfigField(
                "String",
                "EMAIL",
                gradleLocalProperties(rootDir).getProperty("firebase.email")
            )
            buildConfigField(
                "String",
                "PASSWORD",
                gradleLocalProperties(rootDir).getProperty("firebase.password")
            )
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField(
                "String",
                "AP_SSID",
                gradleLocalProperties(rootDir).getProperty("esp8266.ap.ssid")
            )
            buildConfigField(
                "String",
                "AP_PASSWORD",
                gradleLocalProperties(rootDir).getProperty("esp8266.ap.password")
            )

            buildConfigField(
                "String",
                "EMAIL",
                gradleLocalProperties(rootDir).getProperty("firebase.email.debug")
            )
            buildConfigField(
                "String",
                "PASSWORD",
                gradleLocalProperties(rootDir).getProperty("firebase.password.debug")
            )
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        //useIR = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE")
            excludes.add("META-INF/LICENSE.txt")
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/LICENSE-notice.md")
            excludes.add("META-INF/license.txt")
            excludes.add("META-INF/NOTICE")
            excludes.add("META-INF/NOTICE.txt")
            excludes.add("META-INF/notice.txt")
            excludes.add("META-INF/ASL2.0")
            excludes.add("META-INF/*.kotlin_module")
        }
    }


    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    namespace = "com.zelgius.gateApp"
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("com.google.android.material:material:1.11.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestRuntimeOnly("de.mannodermaus.junit5:android-test-runner:1.2.2")

// Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("com.google.truth:truth:1.4.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")

    androidTestImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    androidTestImplementation("org.junit.jupiter:junit-jupiter-params:5.9.1")
    androidTestRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
    androidTestImplementation("de.mannodermaus.junit5:android-test-core:1.2.2")
    androidTestRuntimeOnly("de.mannodermaus.junit5:android-test-runner:1.2.2")
    testImplementation("com.google.assistant.appactions:testing:1.0.0")

    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.test:rules:1.5.0")


//HILT
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    //WORKER
    val workVersion = "2.9.0"
    implementation("androidx.work:work-runtime-ktx:$workVersion")

//KTX
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")


//COMPOSE
    val composeVersion = "1.6.1"
    implementation("androidx.compose.ui:ui:$composeVersion")
// Tooling support (Previews, etc.)
    implementation("androidx.compose.ui:ui-tooling:$composeVersion")
// Foundation (Border, Background, Box, Image, Scroll, shapes, animations, etc.)
    implementation("androidx.compose.foundation:foundation:$composeVersion")
// Material Design
    implementation("androidx.compose.material:material:$composeVersion")
// Material design icons
    implementation("androidx.compose.material:material-icons-core:$composeVersion")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
// Integration with activities
    implementation("androidx.activity:activity-compose:1.8.2")
// Integration with ViewModels
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.compose.runtime:runtime-livedata:$composeVersion")

//Firebase
    implementation("com.google.firebase:firebase-firestore-ktx:24.10.2")
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("androidx.core:core-ktx:+")

    implementation("ca.rmen:lib-sunrise-sunset:1.1.1")
}
repositories {
    mavenCentral()
}