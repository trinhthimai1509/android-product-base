package dev.sautao.productbase.core.ads.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sautao.productbase.core.ads.AdsController
import dev.sautao.productbase.core.ads.internal.GoogleAdsController
import dev.sautao.productbase.core.ads.internal.UmpConsentGateway
import dev.sautao.productbase.core.common.log.Logger
import javax.inject.Singleton

/**
 * The product must provide an `AdsConfig` and a `PremiumState`.
 *
 * `PremiumState` comes from `core:billing` in a paid app and from `PremiumState.AlwaysFree` in a
 * free one — which is the entire reason this module does not depend on billing.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AdsProvidesModule {
    @Provides
    @Singleton
    fun provideConsentGateway(
        @ApplicationContext context: Context,
        logger: Logger,
    ): UmpConsentGateway = UmpConsentGateway(context, logger)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AdsBindsModule {
    @Binds
    @Singleton
    abstract fun bindAdsController(impl: GoogleAdsController): AdsController
}
