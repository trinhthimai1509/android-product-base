package dev.sautao.productbase.demo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.sautao.productbase.core.ads.AdsConfig
import dev.sautao.productbase.core.billing.BillingConfig
import dev.sautao.productbase.core.common.log.AndroidLogger
import dev.sautao.productbase.core.common.log.Logger
import dev.sautao.productbase.core.common.log.NoOpLogger
import dev.sautao.productbase.core.network.NetworkConfig
import dev.sautao.productbase.demo.BuildConfig
import javax.inject.Singleton

/**
 * Product-level wiring: the decisions a capability module refuses to make for you.
 *
 * This is the only place in the app that reads `BuildConfig`.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {
    /** Release builds get the no-op logger, so no module needs its own build-type check. */
    @Provides
    @Singleton
    fun provideLogger(): Logger = if (BuildConfig.DEBUG) AndroidLogger() else NoOpLogger()

    /**
     * The demo has no AdMob account, so it always uses Google's test units. A product supplies
     * its real ids here, per build type, from BuildConfig fields it sets in Gradle.
     */
    @Provides
    @Singleton
    fun provideAdsConfig(): AdsConfig = AdsConfig.testAds()

    /**
     * The product id as configured in Play Console. The demo is sideloaded and has no Play
     * product, which is the "billing unavailable" path the paywall has to handle gracefully.
     */
    @Provides
    @Singleton
    fun provideBillingConfig(): BillingConfig = BillingConfig(proProductId = "pro_lifetime")

    /**
     * A public request-and-echo endpoint, used only so the demo has something real to call.
     * A product points this at its own API, usually per build type.
     *
     * HTTP logging is tied to the debug build and nothing else: request logs routinely contain
     * tokens and personal data.
     */
    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig(
        baseUrl = "https://httpbin.org/",
        enableHttpLogging = BuildConfig.DEBUG,
    )
}
