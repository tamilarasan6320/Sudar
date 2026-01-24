pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack for additional third-party libs
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SudarAndroidNative"
include(":app")
