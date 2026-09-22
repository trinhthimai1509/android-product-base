package dev.sautao.productbase.core.telemetry.firebase.di

import android.content.Context
import com.google.firebase.FirebaseApp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sautao.productbase.core.common.log.Logger
import dev.sautao.productbase.core.telemetry.Analytics
import dev.sautao.productbase.core.telemetry.CrashReporter
import dev.sautao.productbase.core.telemetry.NoOpAnalytics
import dev.sautao.productbase.core.telemetry.NoOpCrashReporter
import dev.sautao.productbase.core.telemetry.firebase.CrashlyticsReporter
import dev.sautao.productbase.core.telemetry.firebase.FirebaseAnalyticsClient
import javax.inject.Singleton

private const val TAG = "Telemetry"

/**
 * Binds the telemetry contracts to Firebase — but only when Firebase is actually configured.
 *
 * Without a `google-services.json` the Firebase initialiser finds no configuration and
 * [FirebaseApp.getApps] stays empty, so the no-op implementations are used instead of crashing
 * on first call. That is what lets a fresh clone with no Firebase project build *and run*, and
 * it keeps the decision to adopt Firebase a matter of dropping in a file rather than editing code.
 *
 * A product that does not want Firebase at all omits this module and binds the no-op
 * implementations from `core:telemetry` itself.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object FirebaseTelemetryModule {
    @Provides
    @Singleton
    fun provideAnalytics(
        @ApplicationContext context: Context,
        logger: Logger,
    ): Analytics = if (isFirebaseConfigured(context)) {
        FirebaseAnalyticsClient(context)
    } else {
        logger.w(TAG, "Firebase is not configured; analytics events will be discarded")
        NoOpAnalytics()
    }

    @Provides
    @Singleton
    fun provideCrashReporter(
        @ApplicationContext context: Context,
        logger: Logger,
    ): CrashReporter = if (isFirebaseConfigured(context)) {
        CrashlyticsReporter()
    } else {
        logger.w(TAG, "Firebase is not configured; crash reports will be discarded")
        NoOpCrashReporter()
    }

    private fun isFirebaseConfigured(context: Context): Boolean =
        FirebaseApp.getApps(context).isNotEmpty()
}
