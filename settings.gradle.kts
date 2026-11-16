pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.d4rken-org.porter-api")
                includeGroup("com.github.CSAbhiOnline")
            }
        }
    }
}

rootProject.name = "Essentials"
include(":app")
include(":stub")
include(":island-sdk")
include(":island-sdk-compose")
