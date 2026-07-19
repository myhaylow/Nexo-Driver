plugins {
    id("com.android.application")
}

android {
    namespace = "com.myhaylow.nexodriver"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.myhaylow.nexodriver"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
