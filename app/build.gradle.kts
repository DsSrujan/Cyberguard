import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // ✅ Added the required plugin for Jetpack Compose
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.aifraudguard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.aifraudguard"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Load API keys from properties file
        val apiKeysFile = rootProject.file("apikeys.properties")
        if (apiKeysFile.exists()) {
            val apiKeys = Properties()
            apiKeys.load(FileInputStream(apiKeysFile))
            buildConfigField("String", "NEWS_API_KEY", "\"${apiKeys.getProperty("NEWS_API_KEY", "")}\"")
            buildConfigField("String", "GEMINI_API_KEY", "\"${apiKeys.getProperty("GEMINI_API_KEY", "")}\"")
        } else {
            buildConfigField("String", "NEWS_API_KEY", "\"\"")
            buildConfigField("String", "GEMINI_API_KEY", "\"\"")
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    // ❌ The old composeOptions block has been removed as it conflicts
    // with the new kotlin.compose plugin.
}

dependencies {

    // Provides AppCompatActivity, themes, and backwards compatibility
    implementation("androidx.appcompat:appcompat:1.6.1")
    // Provides modern Material Design components
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // CircleImageView for profile photos
    implementation("de.hdodenhof:circleimageview:3.1.0")
    
    // Glide for loading images
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // Firebase Authentication (for Google Sign-In and Email/Password)
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    
    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Retrofit2 for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // ViewPager2 for swipeable pages
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // --- Existing Dependencies (Correct for Compose) ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // --- Testing Dependencies ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}