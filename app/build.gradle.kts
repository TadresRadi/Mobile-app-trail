plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") // High-performance processor replacing deprecated kapt
}

android {
    namespace = "com.fintech.vfcgateway"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fintech.vfcgateway"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true // Safely links XML layout widgets without findViewById boilerplate
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // 1. Room Database (Saves SMS records locally first so they are never lost if internet is offline)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // 2. WorkManager (Guarantees webhook uploads even after phone restarts or crashes)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // 3. Retrofit & OkHttp (Modern, standard networking tools to talk to Django)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 4. Coroutines (Enables smooth database and network calls in the background)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}