package dev.sautao.productbase.core.telemetry.firebase

import com.google.firebase.crashlytics.FirebaseCrashlytics
import dev.sautao.productbase.core.telemetry.CrashReporter

/** Firebase Crashlytics behind the [CrashReporter] contract. */
internal class CrashlyticsReporter : CrashReporter {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun recordNonFatal(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
}
