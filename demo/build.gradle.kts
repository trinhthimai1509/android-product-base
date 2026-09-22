// Google's published test application id. Public, not a secret, and never a production value.
val testAdmobApplicationId = "ca-app-pub-3940256099942544~3347511713"

plugins {
    id("productbase.android.application")
    id("productbase.android.compose")
    id("productbase.android.hilt")
    id("productbase.android.room")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.sautao.productbase.demo"

    defaultConfig {
        applicationId = "dev.sautao.productbase.demo"
        versionCode = 1
        versionName = "0.1.0"
    }

    // Off by default in AGP 9; enabled per module that needs it.
    buildFeatures {
        buildConfig = true
    }

    // core:ads declares the AdMob application id as a manifest placeholder, so every build type
    // must name one or the build fails. This is the pattern a real product follows: the debug id
    // is Google's public test value, and the release id comes from outside the repository.
    //
    // The demo has no AdMob account, so release falls back to the test id — and core:ads then
    // refuses to serve anything, because test units in a non-debuggable build are an AdMob policy
    // violation. A real product sets the property and the fallback never applies.
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["admobApplicationId"] = testAdmobApplicationId
        }
        getByName("release") {
            manifestPlaceholders["admobApplicationId"] =
                providers.gradleProperty("productbase.admob.applicationId").getOrElse(testAdmobApplicationId)
        }
    }
}

// Firebase is opt-in by dropping in a file, not by editing code. Without google-services.json
// the plugins are not applied, the build works, and core:telemetry-firebase falls back to its
// no-op implementations at runtime. google-services.json is git-ignored and must stay that way.
val googleServicesConfig = file("google-services.json")
if (googleServicesConfig.exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
} else {
    logger.lifecycle("demo: no google-services.json — building without Firebase.")
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:datastore"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:notification"))
    implementation(project(":core:telemetry"))
    implementation(project(":core:telemetry-firebase"))
    implementation(project(":core:ads"))
    implementation(project(":core:billing"))
    implementation(project(":core:review"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:premium"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.compose.material.icons.core)

    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
}
