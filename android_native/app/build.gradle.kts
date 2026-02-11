import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}


val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.sudar.tnpscapp.nativeapp"
    compileSdk = 35

    defaultConfig {
        // Same applicationId as Flutter for Play Store update
        applicationId = "com.sudar.tnpscapp"
        minSdk = 22
        targetSdk = 35
        // Flutter is +31, native must be >= 32
        versionCode = 43
        versionName = "7.0.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            // Production (real) API only
            buildConfigField("String", "BASE_URL", "\"https://sudartnpscapp.in/api/\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://sudartnpscapp.in/api/\"")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Enable debug symbols for crash reporting
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols += "**/*.so"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Coroutines + Storage
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DI
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-messaging")

    // OneSignal Push Notifications
    implementation("com.onesignal:OneSignal:5.1.21")

    // Facebook SDK (App Events + Install Attribution)
    implementation("com.facebook.android:facebook-android-sdk:17.0.1")

    // Truecaller SDK
    implementation("com.truecaller.android.sdk:truecaller-sdk:3.2.1")

    // Google Play Services for Phone Hint + SMS Retriever + Credentials
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.1.0")
    implementation("com.google.android.gms:play-services-identity:18.1.0")

    // Google Play Install Referrer (capture /r/CODE -> Play Store referrer)
    implementation("com.android.installreferrer:installreferrer:2.2")

    // Razorpay Payments
    implementation("com.razorpay:checkout:1.6.40")

    // In-app Update
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Media3 (ExoPlayer) for video playback
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
