plugins {
    id("productbase.android.library")
    id("productbase.android.compose")
    id("productbase.android.hilt")
}

android {
    namespace = "dev.sautao.productbase.feature.premium"
    resourcePrefix = "pb_premium_"
}

dependencies {
    implementation(project(":core:designsystem"))
    // The base-owned billing API only. No Play Billing dependency appears here, and none can:
    // core:billing keeps the SDK to itself.
    implementation(project(":core:billing"))

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
