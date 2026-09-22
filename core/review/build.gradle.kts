plugins {
    id("productbase.android.library")
    id("productbase.android.hilt")
}

android {
    namespace = "dev.sautao.productbase.core.review"
}

dependencies {
    // Logger only: a review request that Play refuses is reported, not swallowed silently.
    implementation(project(":core:common"))

    // The Play In-App Review SDK, kept to this module. No product code can name a Play Core type.
    implementation(libs.play.review)

    testImplementation(libs.kotlinx.coroutines.test)
}
