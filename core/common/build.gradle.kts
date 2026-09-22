plugins {
    id("productbase.android.library")
}

android {
    namespace = "dev.sautao.productbase.core.common"
}

dependencies {
    // The only dependency, added in Phase 4 for PremiumState's Flow. Coroutines is a Kotlin
    // language library rather than a platform SDK, so this does not weaken the property that
    // matters: depending on core:common still drags in no Firebase, Play, Room or HTTP code.
    api(libs.kotlinx.coroutines.core)
}
