plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}


import org.jetbrains.kotlin.gradle.dsl.JvmTarget

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }
}

android {
    namespace = "com.sameerasw.essentials"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sameerasw.essentials"
        minSdk = 26
        targetSdk = 37
        versionCode = 43
        versionName = "14.0"

        val whatsNewCounter = 1
        buildConfigField("int", "WHATS_NEW_COUNTER", whatsNewCounter.toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

//        optimized dev build

//          debug {
//             isMinifyEnabled = true
//             isShrinkResources = true
//             isDebuggable = false
//
//             proguardFiles(
//                 getDefaultProguardFile("proguard-android-optimize.txt"),
//                 "proguard-rules.pro"
//             )
//          }

        // end


        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
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

    // Force latest Material3 1.5.0-alpha17 for new MaterialShapes
    implementation("androidx.compose.material3:material3:1.5.0-alpha19")

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.appcompat)
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.material)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
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
    implementation("androidx.palette:palette:1.0.0")

    // Reorderable library
    implementation("sh.calvin.reorderable:reorderable:3.0.0")

    // Volume Long Press
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("dev.rikka.shizuku:api:13.1.5")

    // Google Maps & Location
    implementation(libs.play.services.location)
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.wear.remote.interactions)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.gson)
    
    // Kotlin Reflect for dynamic sealed class serialization
    implementation(kotlin("reflect"))

    // SymSpell for word suggestions
    implementation("com.darkrockstudios:symspellkt:3.4.0")

    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // Watermark dependencies
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.compose.material:material-icons-extended:1.7.0") // Compatible with Compose BOM

    // GSMArena Parsing
    implementation("org.jsoup:jsoup:1.15.3")
    implementation(libs.sentry.android)
    implementation(libs.androidx.graphics.shapes)

    // Media3 for Live Wallpaper
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)

    // RemoteIntent support
    implementation("androidx.wear:wear-remote-interactions:1.1.0-alpha02")

    // tandard wearable library
    implementation("com.google.android.gms:play-services-wearable:19.0.0")

}