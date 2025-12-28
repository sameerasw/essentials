plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.sameerasw.essentials"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.sameerasw.essentials"
        minSdk = 23
        targetSdk = 36
        versionCode = 10
        versionName = "7.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    // Android 12+ SplashScreen API with backward compatibility attributes
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Force latest Material3 1.5.0-alpha10 for ToggleButton & ButtonGroup support
    implementation("androidx.compose.material3:material3:1.5.0-alpha10")

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.appcompat)
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation(libs.androidx.compose.foundation.layout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Shizuku
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // Hidden API Bypass
    implementation(libs.hiddenapibypass)

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Reorderable library
    implementation("sh.calvin.reorderable:reorderable:3.0.0")
}