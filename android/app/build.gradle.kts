plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
    id("com.google.gms.google-services")
}

// Load keystore properties
import java.util.Properties
import java.io.FileInputStream

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.sudar.tnpscapp"
    // Truecaller SDK and splash screen theme require compileSdk >= 31
    compileSdk = maxOf(flutter.compileSdkVersion, 34)
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        applicationId = "com.sudar.tnpscapp"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        // Truecaller SDK requires minSdk >= 22
        minSdk = maxOf(flutter.minSdkVersion, 22)
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
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
        release {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Fallback to debug signing if keystore not configured
                signingConfig = signingConfigs.getByName("debug")
            }
            // Enable debug symbols for Google Play Console crash reporting
            ndk {
                debugSymbolLevel = "FULL"
            }
            // ✅ Enable R8/ProGuard for smaller APK and deobfuscation
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            // Fix AAB build failures on some Windows setups where symbol stripping fails.
            // Keeping debug symbols prevents the strip step from running on native libs.
            keepDebugSymbols += "**/*.so"
        }
    }
}

flutter {
    source = "../.."
}

dependencies {
    // Truecaller SDK is now managed by the truecaller_sdk Flutter plugin
    
    // Install Referrer (required for Facebook install attribution)
    implementation("com.android.installreferrer:installreferrer:2.2")
    
    // Facebook SDK (explicitly include for proper install tracking)
    implementation("com.facebook.android:facebook-android-sdk:17.0.1")
    
    // Google Play Services Auth for Phone Number Hint API (One Tap)
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    
    // Google Play Services Auth API Phone for SMS Retriever
    implementation("com.google.android.gms:play-services-auth-api-phone:18.1.0")
}
