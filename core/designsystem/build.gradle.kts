plugins {
    id("productbase.android.library")
    id("productbase.android.compose")
}

android {
    namespace = "dev.sautao.productbase.core.designsystem"
    // Published later as an artifact; a prefix keeps resource names collision-free.
    resourcePrefix = "pb_ds_"
}

dependencies {
    // api, not implementation: ThemeMode appears in ProductBaseTheme's signature, so every
    // consumer of the design system needs it on its compile classpath.
    api(project(":core:common"))
}
