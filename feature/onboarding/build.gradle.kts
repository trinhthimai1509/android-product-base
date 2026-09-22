plugins {
    id("productbase.android.library")
    id("productbase.android.compose")
    id("productbase.android.hilt")
}

android {
    namespace = "dev.sautao.productbase.feature.onboarding"
    resourcePrefix = "pb_onboarding_"
}

dependencies {
    implementation(project(":core:designsystem"))
    // Completion is a preference, and core:datastore is the single owner of preference keys.
    implementation(project(":core:datastore"))

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
