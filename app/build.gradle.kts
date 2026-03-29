plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.gapfix"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.gapfix"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.firebase.storage)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.cardview)
    implementation(libs.cardview.v7)
    implementation(libs.firebase.database)
    implementation(libs.fragment)
    implementation(libs.legacy.support.v4)
    implementation(libs.recyclerview)
    implementation("androidx.compose.foundation:foundation:1.7.0")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.firebase.analytics)
    implementation(libs.google.firebase.analytics)
    implementation(libs.google.firebase.auth)
    implementation("com.cloudinary:cloudinary-android:3.1.2")
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("com.google.android.gms:play-services-auth:21.5.1")
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-messaging:25.0.1")
}