plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlinx.serialization)
//    alias(libs.plugins.ksp)
    id ("com.google.devtools.ksp") version libs.versions.kspVersion.get() // Updated KSP plugin version
    id ("kotlin-parcelize")
    alias(libs.plugins.compose.compiler)
}


val BASE_URL: String = project.findProperty("APP_BASE_URL") as? String ?: "http://localhost:5000/"



android {
    namespace = "com.dino.message"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sec.message"
        minSdk = 28
        targetSdk = 36
        versionCode = 29
        versionName = "0.7beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$BASE_URL\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "BASE_URL", "\"$BASE_URL\"")
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
        buildConfig = true
    }
//    composeOptions {
//        kotlinCompilerExtensionVersion = "1.5.13"
//    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

}

dependencies {


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.swiperefreshlayout)
//    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation (libs.hilt.android)
    ksp (libs.hilt.compiler)
    // For Jetpack Compose integration
    implementation (libs.androidx.hilt.navigation.compose)

    // compose-destinations
    implementation (libs.core)
    implementation(libs.bottom.sheet)
    ksp(libs.ksp)

    implementation(libs.kotlinx.serialization.json)

    //Ktor
    implementation (libs.ktor.client.core)
    implementation (libs.ktor.client.cio)
    implementation (libs.ktor.client.content.negotiation)
    implementation (libs.ktor.serialization.kotlinx.json)
    implementation (libs.ktor.client.serialization)
    implementation (libs.ktor.client.logging)
    implementation (libs.logback.classic)

    implementation(libs.socket.io.client)

//    implementation("io.ktor:ktor-client-websockets:2.3.0")

    implementation (libs.androidx.datastore.preferences)

    implementation(libs.accompanist.systemuicontroller)

    // Core ExoPlayer
    implementation(libs.androidx.media3.exoplayer)

    // UI for ExoPlayer
    implementation(libs.androidx.media3.ui)

    // Optional: Common media utilities
    implementation(libs.androidx.media3.common)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // Gson
    implementation(libs.gson)

    // androidx.security for EncryptedSharedPreferences
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.security.identity.credential)
    implementation(libs.androidx.security.app.authenticator)

    // material icons
    implementation(libs.androidx.material.icons.extended)

    // foundation
    implementation(libs.androidx.compose.foundation)

    // room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    coreLibraryDesugaring(libs.desugar.jdk.libs)


    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
}
