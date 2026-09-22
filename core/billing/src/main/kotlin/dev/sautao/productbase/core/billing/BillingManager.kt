package dev.sautao.productbase.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * The product-facing billing API.
 *
 * No Play Billing type appears here or anywhere outside this module: not `BillingClient`, not
 * `Purchase`, not `ProductDetails`, not `BillingResult`. Product code that talks to this
 * interface keeps working if the billing implementation is replaced or faked.
 *
 * There is no repository, no use case and no manager-of-managers around it. This *is* the
 * abstraction.
 */
interface BillingManager {
    /** Current ownership according to Google Play. Starts at [PurchaseState.Unknown]. */
    val purchaseState: StateFlow<PurchaseState>

    /**
     * The Pro product's details, or null while they are loading or if Play could not supply
     * them — an unconfigured product id, or no Play Store on the device.
     */
    val proProduct: StateFlow<ProProduct?>

    /**
     * Re-queries Play for what the user owns and updates [purchaseState].
     *
     * This is both "restore purchases" and the routine refresh to run on start-up and on resume.
     * They are the same operation: there is nothing to restore *to*, only ownership to re-read.
     */
    suspend fun refreshPurchases()

    /**
     * Launches Play's purchase flow and suspends until it resolves.
     *
     * The returned outcome describes this attempt; [purchaseState] remains the authority on what
     * the user owns.
     */
    suspend fun launchPurchase(activity: Activity): PurchaseOutcome
}
