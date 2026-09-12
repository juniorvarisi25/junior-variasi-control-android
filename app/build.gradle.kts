plugins {
    id("com.android.application")
}

android {
    namespace = "com.juniorvariasi.control"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.juniorvariasi.control"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-test"
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
}
