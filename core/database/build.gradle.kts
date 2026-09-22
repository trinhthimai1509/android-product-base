plugins {
    id("productbase.android.library")
}

android {
    namespace = "dev.sautao.productbase.core.database"
}

dependencies {
    // api: converter classes are referenced from a consumer's @TypeConverters annotation, so the
    // Room annotations have to be on the consumer's compile classpath.
    // Note there is no KSP here and no androidx.room plugin: this module declares no @Database,
    // so it has no schema to export and nothing to process. Modules that own a database apply
    // the `productbase.android.room` convention plugin instead.
    api(libs.androidx.room.runtime)

    testImplementation(libs.junit)
}
