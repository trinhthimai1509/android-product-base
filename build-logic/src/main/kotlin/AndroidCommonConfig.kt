import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Configuration shared by every Android module, application or library.
 *
 * Kotlin is compiled by AGP itself (built-in Kotlin, AGP 9+) — the `org.jetbrains.kotlin.android`
 * plugin must never be applied, and `jvmTarget` follows `compileOptions.targetCompatibility`,
 * so it needs no separate configuration.
 *
 * Note on style: AGP 9's `CommonExtension` exposes nested DSL objects as plain properties; the
 * `defaultConfig { }` / `compileOptions { }` block forms live on the concrete `ApplicationExtension`
 * and `LibraryExtension` types. Property access is therefore used throughout this file.
 */
internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    with(extension) {
        compileSdk = libs.intVersion("compileSdk")
        defaultConfig.minSdk = libs.intVersion("minSdk")
        defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17
        // Desugaring is kept deliberately at minSdk 26: not for java.time availability (native
        // since 26) but for the bundled, up-to-date timezone database. ARCHITECTURE_PLAN.md §6.
        compileOptions.isCoreLibraryDesugaringEnabled = true

        // Android framework classes in unit tests return defaults instead of throwing "Stub!".
        // What this buys: a ViewModel that only *passes* an Activity to a capability can be
        // tested off-device. What it does not excuse: testing anything that actually uses the
        // framework — that belongs in an instrumentation test, not against a hollow stub.
        testOptions.unitTests.isReturnDefaultValues = true

        lint.apply {
            abortOnError = true
            warningsAsErrors = false
            htmlReport = true
            sarifReport = true
            xmlReport = false
            // Versions are pinned deliberately and upgraded as a catalog decision, with the
            // compatibility matrix re-checked. A permanent "newer version available" warning
            // would only teach everyone to ignore lint output.
            disable += "NewerVersionAvailable"
        }
    }

    dependencies {
        add("coreLibraryDesugaring", libs.library("desugar-jdk-libs"))
        add("testImplementation", libs.library("junit"))
    }
}
