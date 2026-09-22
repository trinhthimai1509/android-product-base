plugins {
    id("productbase.android.library")
}

android {
    namespace = "dev.sautao.productbase.core.testing"
}

// Consumed with `testImplementation(project(":core:testing"))`. Everything here is `api` because
// a consumer writing a test uses these types directly.
dependencies {
    api(project(":core:datastore"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
