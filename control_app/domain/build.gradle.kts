
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version Dependencies.kotlinVersion
    id("org.jetbrains.kotlin.plugin.compose") version Dependencies.kotlinVersion
}

android {
    namespace = "com.rc.playgrounds.domain"
    compileSdk = Dependencies.targetSdkVersion
    buildToolsVersion = Dependencies.buildToolsVersion

    defaultConfig {
        minSdk = Dependencies.minSdkVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Dependencies.kotlinxSerializationJson}")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.2")

    implementation(platform("androidx.compose:compose-bom:${Dependencies.composeBomVersion}"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3")
    testImplementation(Dependencies.kotlinMockito)
    testImplementation(Dependencies.mockitoCore)
    debugImplementation("androidx.compose.ui:ui-tooling")


    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}