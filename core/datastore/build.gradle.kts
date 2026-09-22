plugins {
    id("productbase.android.library")
    id("productbase.android.hilt")
}

android {
    namespace = "dev.sautao.productbase.core.datastore"
}

dependencies {
    // api: ThemeMode appears in AppPreferences' signature.
    api(project(":core:common"))
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
