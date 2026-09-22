plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
}

// Kept in step with the root build's ktlint settings.
val ktlintRules =
    mapOf(
        "ktlint_code_style" to "intellij_idea",
        "max_line_length" to "120",
        "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
        "ktlint_standard_function-signature" to "disabled",
    )

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

group = "dev.sautao.productbase.buildlogic"

// JDK 21, not 17: the Gradle daemon already runs on 21, and the Homebrew openjdk@17 on this
// machine is not usable as a Gradle toolchain. See ARCHITECTURE_PLAN.md §0.2.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // compileOnly: these plugins are put on the consuming build's classpath by the root
    // `plugins { ... apply false }` block; build-logic only needs their types to compile.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.gradleCommonApi)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.androidx.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "productbase.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "productbase.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "productbase.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "productbase.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "productbase.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
    }
}
