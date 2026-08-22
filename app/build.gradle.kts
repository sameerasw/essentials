import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.ui.text.ExperimentalTextApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
}

android {
    namespace = "com.sameerasw.essentials"
    compileSdk = 37

    androidResources {
        localeFilters +=
            listOf(
                "en",
                "ach",
                "af",
                "ar",
                "bn",
                "bn-rBD",
                "ca",
                "cs",
                "da",
                "de",
                "el",
                "es",
                "fi",
                "fil",
                "fil-rPH",
                "fr",
                "he",
                "hi",
                "hi-rIN",
                "hu",
                "id",
                "in",
                "in-rID",
                "it",
                "iw",
                "ja",
                "kk",
                "kk-rKZ",
                "ko",
                "ml",
                "ml-rIN",
                "ne",
                "ne-rNP",
                "nl",
                "no",
                "pl",
                "pt",
                "pt-rBR",
                "pt-rPT",
                "ro",
                "ru",
                "si",
                "sk",
                "sk-rSK",
                "sr",
                "sv",
                "ta",
                "ta-rIN",
                "tr",
                "uk",
                "vi",
                "zh",
                "zh-rCN",
                "zh-rTW",
            )
    }

    defaultConfig {
        applicationId = "com.sameerasw.essentials"
        minSdk = 26
        targetSdk = 37
        versionCode = 61
        versionName = "17.2"

        val whatsNewCounter = 2
        buildConfigField("int", "WHATS_NEW_COUNTER", whatsNewCounter.toString())
        buildConfigField("int", "REQUIRED_WEAR_VERSION_CODE", "6")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
//        optimized dev build
//           debug {
//              isMinifyEnabled = true
//              isShrinkResources = true
//              isDebuggable = false
//              proguardFiles(
//                  getDefaultProguardFile("proguard-android-optimize.txt"),
//                  "proguard-rules.pro"
//              )
//           }
//        end

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    compileOnly(project(":stub"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    // Android 12+ SplashScreen API with backward compatibility attributes
    implementation(libs.androidx.core.splashscreen)

    // Force latest Material3 1.5.0-alpha17 for new MaterialShapes
    implementation(libs.androidx.compose.material3.v150alpha24)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.genai.schema)
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
    implementation(libs.gson.v2140)
    implementation(libs.androidx.palette)

    // Reorderable library
    implementation(libs.reorderable)

    // Volume Long Press
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.api.v1315)

    // Google Maps & Location
    implementation(libs.play.services.location)
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.wear.remote.interactions)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.gson)

    // Kotlin Reflect for dynamic sealed class serialization
    implementation(kotlin("reflect"))

    // SymSpell for word suggestions
    implementation(libs.symspellkt)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Watermark dependencies
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.compose.material.icons.extended) // Compatible with Compose BOM

    // GSMArena Parsing
    implementation(libs.jsoup)
    implementation(libs.sentry.android)
    implementation(libs.androidx.graphics.shapes)

    // AutoUpdater
    implementation(libs.autoupdater)

    // Media3 for Live Wallpaper
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.common)

    // RemoteIntent support
    implementation(libs.androidx.wear.remote.interactions.v110alpha02)

    // tandard wearable library
    implementation(libs.play.services.wearable.v1900)

    // Lottie for animations
    implementation(libs.lottie.compose)

    // ML Kit GenAI Prompt API & Structured Output Compiler
    implementation(libs.mlkit.genai.prompt)
    ksp(libs.mlkit.genai.schema.compiler)

    // AppFunctions API
    implementation(libs.androidx.appfunctions)
    ksp(libs.androidx.appfunctions.compiler)

    // QR Code Engine
    implementation(libs.zxing.core)
}
