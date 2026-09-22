plugins {
    id("productbase.android.library")
    id("productbase.android.hilt")
}

android {
    namespace = "dev.sautao.productbase.core.telemetry.firebase"
}

dependencies {
    // api: this module's Hilt bindings hand back core:telemetry types.
    api(project(":core:telemetry"))
    implementation(project(":core:common"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
