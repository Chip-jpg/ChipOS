plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.chipos.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.chipos.launcher"
        // Fire OS 7 (3rd gen Fire TV Stick) is Android 9 = API 28. That is the tested target.
        minSdk = 28
        // Kept at 28 on purpose: Fire OS 7 is API 28, and a higher target would
        // require package-visibility permissions for the app list.
        targetSdk = 28
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // Signed with the debug key so the release APK installs straight away (sideloading only).
            // Release builds are much faster than debug builds: Compose is slow in debug.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
        compose = true
    }
    composeOptions {
        // Matches Kotlin 1.9.24.
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    lint {
        // The low targetSdk is intentional (see above); this app is sideloaded, not on Google Play.
        disable += "ExpiredTargetSdkVersion"
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    // Installs the precompiled startup profiles that ship inside the Compose libraries,
    // which makes the first launches noticeably quicker on a slow device.
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
}
