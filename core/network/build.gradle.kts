plugins {
    id("productbase.android.library")
    id("productbase.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.sautao.productbase.core.network"
}

dependencies {
    // Logger only — NetworkMonitor reports a platform refusal rather than swallowing it.
    implementation(project(":core:common"))

    // api: NetworkConfig hands back Retrofit/OkHttp/Json instances, and products build their own
    // service interfaces against Retrofit.
    api(platform(libs.okhttp.bom))
    api(libs.okhttp)
    api(libs.retrofit)
    api(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit.converter.kotlinx.serialization)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
}
