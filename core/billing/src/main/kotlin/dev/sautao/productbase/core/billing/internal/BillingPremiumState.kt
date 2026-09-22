package dev.sautao.productbase.core.billing.internal

import dev.sautao.productbase.core.billing.BillingManager
import dev.sautao.productbase.core.common.PremiumState
import dev.sautao.productbase.core.common.PremiumStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Premium follows the Pro purchase, as Google Play reports it.
 *
 * This is the whole of the seam. `core:ads` sees `PremiumState`; it never sees this class, this
 * module, or Play Billing.
 */
internal class BillingPremiumState @Inject constructor(billingManager: BillingManager) : PremiumState {
    override val status: Flow<PremiumStatus> = billingManager.purchaseState.map { it.toPremiumStatus() }
}
