package dev.sautao.productbase.core.billing.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.sautao.productbase.core.billing.BillingManager
import dev.sautao.productbase.core.billing.internal.BillingPremiumState
import dev.sautao.productbase.core.billing.internal.PlayBillingManager
import dev.sautao.productbase.core.common.PremiumState
import javax.inject.Singleton

/**
 * The product must provide a `BillingConfig`; this module ships no product id.
 *
 * Including this module also decides what "premium" means for the app: entitlement follows the
 * Pro purchase. An app with no purchases omits the module and binds
 * [PremiumState.AlwaysFree] itself.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class BillingModule {
    @Binds
    @Singleton
    abstract fun bindBillingManager(impl: PlayBillingManager): BillingManager

    @Binds
    @Singleton
    abstract fun bindPremiumState(impl: BillingPremiumState): PremiumState
}
