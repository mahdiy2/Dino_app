plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
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
        val commonMain by getting
        val nonWebMain by creating {
            dependsOn(commonMain)
            dependencies {
                api(libs.room.runtime)
            }
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.animation)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.cryptography.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        getByName("androidMain").apply {
            dependsOn(nonWebMain)
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.cryptography.provider.jdk)
            }
        }

        val iosMain by creating {
            dependsOn(nonWebMain)
            dependencies {
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.cryptography.provider.openssl3.prebuilt)
            }
        }

        getByName("iosX64Main").apply {
            dependsOn(iosMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        getByName("iosArm64Main").apply {
            dependsOn(iosMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        getByName("iosSimulatorArm64Main").apply {
            dependsOn(iosMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        val iosTest by creating {
            dependsOn(getByName("commonTest"))
        }
        listOf("iosX64Test", "iosArm64Test", "iosSimulatorArm64Test").forEach { targetTest ->
            getByName(targetTest).dependsOn(iosTest)
        }

        getByName("desktopMain").apply {
            dependsOn(nonWebMain)
            dependencies {
                val arm64 = System.getProperty("os.arch") in setOf("aarch64", "arm64")
                val javafxPlatform = when {
                    System.getProperty("os.name").startsWith("Mac") -> if (arm64) "mac-aarch64" else "mac"
                    System.getProperty("os.name").startsWith("Windows") -> "win"
                    else -> if (arm64) "linux-aarch64" else "linux"
                }
                for (module in listOf("base", "graphics", "media", "swing")) {
                    implementation("org.openjfx:javafx-$module:21.0.8:$javafxPlatform")
                }
                implementation(libs.ktor.client.cio)
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.cryptography.provider.jdk)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        getByName("wasmJsMain").dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.cryptography.provider.webcrypto)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

compose.resources {
    packageOfResClass = "com.dino.message.composeapp.generated.resources"
}

android {
    namespace = "com.dino.message.composeapp"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
