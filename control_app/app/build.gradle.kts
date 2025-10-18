import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val gStreamerModule = (rootProject.extra["gstreamerModule"] as? String) ?: ":gstreamer"

android {
    buildToolsVersion = Dependencies.buildToolsVersion

    defaultConfig {
        applicationId = "com.testspace"
        minSdkVersion(Dependencies.minSdkVersion)
        compileSdkVersion(Dependencies.targetSdkVersion)
        targetSdkVersion(Dependencies.targetSdkVersion)
        versionCode = 2
        versionName = "0.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }

        names.forEach {
            getByName(it) {
                val currentTime = SimpleDateFormat("HH:mm:ss").format(Date())
                buildConfigField("String", "EXPERIMENT_NAME", "\"$currentTime\"")
            }
        }
    }

    signingConfigs {
        getByName("debug").apply {
            storeFile = file("debug_signing.jks")
            storePassword = "debug_pass"
            keyAlias = "debug"
            keyPassword = "debug_pass"
        }
    }

    namespace = "com.testspace"
    flavorDimensions("default")
}

dependencies {
    implementation(project(gStreamerModule))
    implementation(project(":domain"))
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-exoplayer-rtsp:1.5.1")
    implementation("org.videolan.android:libvlc-all:3.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
//    implementation("com.creativa77:android_streaming_client:1.0.+")

    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.0") {
        exclude(group = "com.android.support", module = "support-annotations")
    }

    implementation("androidx.appcompat:appcompat:${Dependencies.androidxVersion}")
    testImplementation("junit:junit:4.12")

    // kotlin-related dependencies
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Dependencies.kotlinVersion}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${Dependencies.kotlinVersion}")

    // UI Tests
    androidTestImplementation("androidx.ui:ui-test:${Dependencies.androidxVersion}")
}