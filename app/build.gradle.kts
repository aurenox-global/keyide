plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

android {
    namespace = "com.keyide.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.keyide.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "0.10.0"
        resourceConfigurations += listOf("es", "en")

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.3.202401111512-r")
}

// Dependencias pip → se empaquetan en el APK en build-time (Chaquopy).
chaquopy {
    defaultConfig {
        pip {
            install("requests")
        }
    }
}
