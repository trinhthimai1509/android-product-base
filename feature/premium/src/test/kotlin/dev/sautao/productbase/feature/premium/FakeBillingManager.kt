package dev.sautao.productbase.feature.premium

import android.app.Activity
import dev.sautao.productbase.core.billing.BillingManager
import dev.sautao.productbase.core.billing.ProProduct
import dev.sautao.productbase.core.billing.PurchaseOutcome
import dev.sautao.productbase.core.billing.PurchaseState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The second implementation of [BillingManager], and the reason the interface exists: the
 * paywall is testable without Google Play, a signed build or a test account.
 */
class FakeBillingManager(
    initialState: PurchaseState = PurchaseState.NotPurchased,
    initialProduct: ProProduct? = ProProduct(
        productId = "pro_lifetime",
        title = "Pro",
        description = "Everything unlocked",
        formattedPrice = "€4.99",
    ),
) : BillingManager {
    private val _purchaseState = MutableStateFlow(initialState)
    override val purchaseState: StateFlow<PurchaseState> = _purchaseState

    private val _proProduct = MutableStateFlow(initialProduct)
    override val proProduct: StateFlow<ProProduct?> = _proProduct

    /** What the next [launchPurchase] returns. */
    var nextOutcome: PurchaseOutcome = PurchaseOutcome.Purchased

    /** What a refresh discovers, standing in for what Google Play would report. */
    var stateAfterRefresh: PurchaseState = PurchaseState.NotPurchased

    var refreshCount: Int = 0
        private set

    override suspend fun refreshPurchases() {
        refreshCount++
        _purchaseState.value = stateAfterRefresh
    }

    override suspend fun launchPurchase(activity: Activity): PurchaseOutcome {
        if (nextOutcome == PurchaseOutcome.Purchased) _purchaseState.value = PurchaseState.Purchased
        if (nextOutcome == PurchaseOutcome.Pending) _purchaseState.value = PurchaseState.Pending
        return nextOutcome
    }
}
