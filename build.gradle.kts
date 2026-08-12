// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}

tasks.register("verifyKmpBaseline") {
    group = "verification"
    description = "Compiles the Android shell and the initial Compose Multiplatform module."

    dependsOn(
        ":androidApp:compileDebugKotlin",
        ":composeApp:compileKotlinMetadata",
        ":composeApp:compileDebugKotlinAndroid",
        ":composeApp:compileKotlinIosX64",
        ":composeApp:compileKotlinIosArm64",
        ":composeApp:compileKotlinIosSimulatorArm64",
        ":composeApp:compileKotlinDesktop",
        ":composeApp:compileKotlinWasmJs",
        ":composeApp:compileCommonMainKotlinMetadata",
        ":composeApp:compileDebugUnitTestKotlinAndroid",
        ":composeApp:compileTestKotlinDesktop",
        ":composeApp:desktopTest",
        ":composeApp:compileTestKotlinWasmJs",
        ":composeApp:compileTestKotlinIosX64",
        ":composeApp:compileTestKotlinIosArm64",
        ":composeApp:compileTestKotlinIosSimulatorArm64",
    )
}
