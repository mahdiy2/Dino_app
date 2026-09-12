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

tasks.register("checkDomainBoundaries") {
    group = "verification"
    description = "Fails when domain source imports data, presentation, Android, or AndroidX packages."

    doLast {
        val forbiddenImportPattern = Regex("""^\s*import\s+.*(\.data\.|\.presentation\.|android\.|androidx\.).*$""")
        val domainRoots = listOf(
            file("androidApp/src/main/java"),
            file("androidApp/src/test/java"),
            file("composeApp/src/commonMain/kotlin"),
            file("composeApp/src/commonTest/kotlin"),
            file("composeApp/src/androidMain/kotlin"),
            file("composeApp/src/androidUnitTest/kotlin"),
        )
        val violations = domainRoots
            .filter { it.exists() }
            .flatMap { root ->
                root.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" && "${it.invariantSeparatorsPath}/".contains("/domain/") }
                    .flatMap { sourceFile ->
                        sourceFile.readLines().mapIndexedNotNull { index, line ->
                            if (forbiddenImportPattern.containsMatchIn(line)) {
                                "${sourceFile.relativeTo(rootProject.projectDir).invariantSeparatorsPath}:${index + 1}: $line"
                            } else {
                                null
                            }
                        }
                    }
                    .toList()
            }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Domain boundary violations found:\n" + violations.joinToString(separator = "\n")
            )
        }
    }
}

tasks.register("verifyKmpBaseline") {
    group = "verification"
    description = "Compiles the Android shell and the initial Compose Multiplatform module."

    dependsOn(
        "checkDomainBoundaries",
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
