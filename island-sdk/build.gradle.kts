plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.sameerasw.island.sdk"
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        aidl = true
    }
}

dependencies {
    implementation(libs.gson)
}
