import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}


val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val BASE_URL: String =
    (project.findProperty("APP_BASE_URL") as? String)
        ?: System.getenv("APP_BASE_URL")
        ?: localProperties.getProperty("APP_BASE_URL")
        ?: "http://10.0.2.2:5000/"

val AUTH_PASSWORD_REQUIRE_SYMBOL: Boolean =
    (project.findProperty("AUTH_PASSWORD_REQUIRE_SYMBOL") as? String)
        ?.toBooleanStrictOrNull()
        ?: (localProperties.getProperty("AUTH_PASSWORD_REQUIRE_SYMBOL")?.toBooleanStrictOrNull())
        ?: false


android {
    namespace = "com.dino.message"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sec.message"
        minSdk = 28
        targetSdk = 36
        versionCode = 50
        versionName = "0.9.8beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$BASE_URL\"")
            buildConfigField("boolean", "AUTH_PASSWORD_REQUIRE_SYMBOL", AUTH_PASSWORD_REQUIRE_SYMBOL.toString())
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "BASE_URL", "\"$BASE_URL\"")
            buildConfigField("boolean", "AUTH_PASSWORD_REQUIRE_SYMBOL", AUTH_PASSWORD_REQUIRE_SYMBOL.toString())
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

}

dependencies {

    implementation(project(":composeApp"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.components.resources)
    implementation(compose.components.uiToolingPreview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.kotlinx.serialization.json)

    //Ktor
    implementation (libs.ktor.client.core)
    implementation (libs.ktor.client.okhttp)

    implementation(libs.socket.io.client)


    implementation (libs.androidx.datastore.preferences)

    // Core ExoPlayer
    implementation(libs.androidx.media3.exoplayer)

    // UI for ExoPlayer
    implementation(libs.androidx.media3.ui)

    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.effect)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.video)

    // androidx.security for EncryptedSharedPreferences
    implementation(libs.androidx.security.crypto)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

}
