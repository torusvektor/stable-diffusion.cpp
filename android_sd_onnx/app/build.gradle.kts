// app/build.gradle.kts
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.sdondevice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sdondevice"
        minSdk = 29
        targetSdk = 35
        ndk {
            // jen arm64
            abiFilters += listOf("arm64-v8a")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // Použij ORT build, který obsahuje QNN EP (AAR/JAR dle vašeho distribučního kanálu)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.20.0")
}
