import com.diffplug.gradle.spotless.SpotlessExtension

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9 compiles Kotlin itself (built-in Kotlin) and pins KGP 2.2.10. KSP 2.3.11 —
        // required by AGP 9 — needs Kotlin 2.3.x, so the Kotlin Gradle Plugin is raised here.
        // Do not remove: without it, KSP fails with "kotlin.sourceSets DSL is not allowed".
        classpath(libs.kotlin.gradlePlugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    // Applied by a module only when it has a google-services.json. See demo/build.gradle.kts.
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.spotless)
}

// Convention plugins are code too: `spotlessCheck` and `spotlessApply` at the root also cover
// the included build, so an agent editing build-logic gets the same formatting feedback.
listOf("spotlessCheck", "spotlessApply").forEach { taskName ->
    tasks.named(taskName) {
        dependsOn(gradle.includedBuild("build-logic").task(":$taskName"))
    }
}

// Passed explicitly rather than relying on .editorconfig: Spotless does not treat that file as a
// task input, so an edit to it would silently not re-format anything.
val ktlintRules =
    mapOf(
        "ktlint_code_style" to "intellij_idea",
        "max_line_length" to "120",
        // @Composable functions are PascalCase by convention.
        "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
        // Leave signatures as written: the rule collapses multi-line parameter lists, which is
        // wrong for Compose APIs and makes every added parameter a whole-signature diff.
        "ktlint_standard_function-signature" to "disabled",
    )

allprojects {
    apply(plugin = "com.diffplug.spotless")
    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            targetExclude("**/build/**")
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
}
