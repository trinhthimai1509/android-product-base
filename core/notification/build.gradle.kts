plugins {
    id("productbase.android.library")
    id("productbase.android.hilt")
}

android {
    namespace = "dev.sautao.productbase.core.notification"
}

dependencies {
    implementation(project(":core:common"))

    // NotificationCompat and NotificationManagerCompat.
    implementation(libs.androidx.core.ktx)
    // api: scheduling is the module's purpose and WorkManager types surface in its behaviour
    // (work is persisted and survives process death and reboot).
    api(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
}
