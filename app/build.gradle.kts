plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.gapfix"
    compileSdk = 36

    compileSdkExtension = 19

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
    implementation(libs.constraintlayout)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.cardview)
    implementation(libs.fragment)
    implementation(libs.legacy.support.v4)
    implementation(libs.recyclerview)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.database)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    // Google Play Services
    implementation(libs.play.services.auth)
    implementation(libs.play.services.nearby)

    // Other Libraries
    implementation(libs.foundation)
    implementation("com.cloudinary:cloudinary-android:3.1.2")
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("io.agora.rtc:full-sdk:4.5.0")
    implementation("io.agora.rtc:chat-sdk:1.3.2")
    implementation(libs.firebase.functions)
    implementation(libs.firebase.firestore)
    implementation(libs.pdf.viewer.fragment)
    implementation(libs.ccp)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}