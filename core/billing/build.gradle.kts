plugins {
    id("productbase.android.library")
    id("productbase.android.hilt")
}

android {
    namespace = "dev.sautao.productbase.core.billing"
}

dependencies {
    api(project(":core:common"))
    implementation(libs.play.billing.ktx)

    // Purchase parses JSON in its constructor, and android.jar's org.json stub throws.
    testImplementation(libs.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
