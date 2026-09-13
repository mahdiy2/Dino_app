plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm("desktop")

    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        getByName("androidMain").dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        getByName("iosX64Main").dependencies {
            implementation(libs.ktor.client.darwin)
        }

        getByName("iosArm64Main").dependencies {
            implementation(libs.ktor.client.darwin)
        }

        getByName("iosSimulatorArm64Main").dependencies {
            implementation(libs.ktor.client.darwin)
        }

        getByName("desktopMain").dependencies {
            implementation(libs.ktor.client.cio)
        }

        getByName("wasmJsMain").dependencies {
            implementation(libs.ktor.client.js)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.dino.message.composeapp"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }
}
