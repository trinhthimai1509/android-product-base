plugins {
    id("productbase.android.library")
    id("productbase.android.compose")
    id("productbase.android.hilt")
}

android {
    namespace = "dev.sautao.productbase.core.ads"
}

dependencies {
    // PremiumState. Note what is absent: no dependency on core:billing, so an ad-supported free
    // app never links the Play Billing SDK.
    api(project(":core:common"))
    // The banner is a Composable and has to sit inside the product's theme.
    implementation(project(":core:designsystem"))

    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
