import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Compose support for an application or library module.
 *
 * The Compose compiler plugin is still applied explicitly under AGP 9 —
 * `buildFeatures.compose = true` on its own fails the build.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val android = extensions.getByName("android") as CommonExtension
        android.buildFeatures.compose = true

        dependencies {
            val bom = libs.library("compose-bom")
            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))

            add("implementation", libs.library("compose-ui"))
            add("implementation", libs.library("compose-ui-graphics"))
            add("implementation", libs.library("compose-ui-tooling-preview"))
            add("implementation", libs.library("compose-material3"))

            add("debugImplementation", libs.library("compose-ui-tooling"))
            add("debugImplementation", libs.library("compose-ui-test-manifest"))
        }
    }
}
