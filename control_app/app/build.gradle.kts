plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    buildToolsVersion = Dependencies.buildToolsVersion

    defaultConfig {
        applicationId = "com.testspace"
        minSdkVersion(Dependencies.minSdkVersion)
        compileSdkVersion(Dependencies.targetSdkVersion)
        targetSdkVersion(Dependencies.targetSdkVersion)
        versionCode = 1
        versionName = "1.00"
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
                buildConfigField("String", "EXPERIMENT_NAME", "\"??\"")
            }
        }
    }

    namespace = "com.testspace"
    flavorDimensions("default")
}

dependencies {
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
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