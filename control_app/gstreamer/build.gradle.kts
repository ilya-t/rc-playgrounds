import java.util.Properties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rc.playgrounds.gstreamer"
    compileSdk = Dependencies.targetSdkVersion
    buildToolsVersion = Dependencies.buildToolsVersion

    defaultConfig {
        minSdk = Dependencies.minSdkVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            ndkBuild {
                val gstRoot: String? = if (project.rootProject.file("local.properties").exists()) {
                    val properties = Properties().apply {
                        load(project.rootProject.file("local.properties").inputStream())
                    }
                    properties.getProperty("gstAndroidRoot")
                } else {
                    System.getenv("GSTREAMER_ROOT_ANDROID")
                }


                if (gstRoot == null)
                    throw GradleException("""
                        GSTREAMER_ROOT_ANDROID must be set in env, or 'gstAndroidRoot' must be defined in your local.properties.
                        Property value example: /path/to/gstreamer-1.0-android-universal-1.24.12
                    """.trimIndent()
                        )

                arguments(
                    "NDK_APPLICATION_MK=jni/Application.mk",
                    "GSTREAMER_JAVA_SRC_DIR=src",
                    "GSTREAMER_ROOT_ANDROID=$gstRoot",
                    "GSTREAMER_ASSETS_DIR=src/assets",
                )
                targets("rtsp-example")

                // All archs except MIPS and MIPS64 are supported
                abiFilters ("armeabi-v7a", "arm64-v8a", "x86")
                // x86_64 abis disabled because of https://bugzilla.gnome.org/show_bug.cgi?id=795454
            }
        }
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    externalNativeBuild {
        ndkBuild {
            path("jni/Android.mk")
        }
    }
}

afterEvaluate {
    tasks.matching { it.name == "compileDebugJavaWithJavac" }.configureEach {
        dependsOn("externalNativeBuildDebug")
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.10.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}