plugins {
    id("productbase.android.library")
    id("productbase.android.compose")
}

android {
    namespace = "dev.sautao.productbase.feature.settings"
    resourcePrefix = "pb_settings_"
}

// Note what is absent, and deliberately so: no core:billing, no core:ads, no core:notification,
// no core:review, no Firebase, and no Hilt. A settings row is a value the product supplies, so
// this module never needs to know which capabilities exist — see ARCHITECTURE_PLAN.md §20.3.
dependencies {
    // api: ThemeMode appears in the row model, and it reaches consumers through the design
    // system's own api dependency on core:common.
    api(project(":core:designsystem"))
    // String.toUri() only. Present in every Android app already.
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
}
