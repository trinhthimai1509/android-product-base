plugins {
    id("productbase.android.library")
}

android {
    namespace = "dev.sautao.productbase.core.telemetry"
}

// No dependencies, and in particular no Firebase: this module is the boundary that lets a
// product ship analytics and crash reporting with a different provider, or with none at all.
