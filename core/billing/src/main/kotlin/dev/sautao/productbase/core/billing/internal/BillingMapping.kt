package dev.sautao.productbase.core.billing.internal

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import dev.sautao.productbase.core.billing.BillingError
import dev.sautao.productbase.core.billing.ProProduct
import dev.sautao.productbase.core.billing.PurchaseOutcome
import dev.sautao.productbase.core.billing.PurchaseState
import dev.sautao.productbase.core.common.PremiumStatus

// Every decision this module makes about Play's answers, as pure functions.
//
// Keeping the rules out of the callback plumbing is what makes them testable: Purchase and
// BillingResult can both be constructed directly, so these are exercised with real SDK objects
// rather than a hand-written imitation of the SDK.

/**
 * What the user owns, given everything Play reports for in-app products.
 *
 * Ownership requires a purchase of *this* product in state PURCHASED. An unacknowledged purchase
 * still counts as owned — Play has taken the money, and acknowledgement is this app's job, not a
 * condition of ownership. PENDING does not count: the money has not moved.
 */
internal fun List<Purchase>.toPurchaseState(productId: String): PurchaseState {
    val relevant = filter { productId in it.products }
    return when {
        relevant.any { it.purchaseState == Purchase.PurchaseState.PURCHASED } -> PurchaseState.Purchased
        relevant.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> PurchaseState.Pending
        else -> PurchaseState.NotPurchased
    }
}

/**
 * Purchases that must be acknowledged.
 *
 * Play refunds anything not acknowledged within three days, so this runs after every purchase
 * and every refresh — including the refresh after a process restart, which is how an
 * acknowledgement that was interrupted by a crash still happens.
 */
internal fun List<Purchase>.needingAcknowledgement(productId: String): List<Purchase> = filter {
    productId in it.products &&
        it.purchaseState == Purchase.PurchaseState.PURCHASED &&
        !it.isAcknowledged
}

/**
 * Ownership as the rest of the app sees it.
 *
 * Three decisions worth spelling out:
 *
 * - [PurchaseState.Unknown] maps to [PremiumStatus.UNKNOWN] rather than to "free". A cold start
 *   spends its first moments here, and mapping it to free is what makes a paying user see an ad
 *   flash before Play has answered.
 * - [PurchaseState.Pending] maps to [PremiumStatus.FREE]: the money has not moved. A pending
 *   purchase can sit for days at a kiosk and can still be declined or abandoned, and granting
 *   the paid experience for it hands the product away to anyone who starts a payment and never
 *   finishes. The paywall says so in its own words; ads keep running until it clears.
 * - [PurchaseState.Purchased] is the only route to [PremiumStatus.PREMIUM].
 */
internal fun PurchaseState.toPremiumStatus(): PremiumStatus = when (this) {
    PurchaseState.Unknown -> PremiumStatus.UNKNOWN
    PurchaseState.Purchased -> PremiumStatus.PREMIUM
    PurchaseState.Pending, PurchaseState.NotPurchased -> PremiumStatus.FREE
}

/**
 * Whether a Play response code proves this user owns nothing *on this device*, as opposed to
 * merely failing to answer.
 *
 * The distinction matters because ownership stays [PurchaseState.Unknown] after a failed query —
 * correctly, since a failure says nothing — and everything that treats unknown conservatively
 * (advertising, above all) would then stay switched off forever on a device where Play Billing
 * is simply not a thing: no Play Store, an unsupported country, a manufacturer without it.
 *
 * `BILLING_UNAVAILABLE` and `FEATURE_NOT_SUPPORTED` are not transient. Nothing could have been
 * bought here and nothing could be verified here, so "not purchased" is the truthful answer
 * rather than a guess. Every other code — disconnected, timed out, no network — leaves ownership
 * unknown, because those are exactly the cases where a paying user is on the other end.
 */
internal fun Int.provesNoOwnership(): Boolean = when (this) {
    BillingResponseCode.BILLING_UNAVAILABLE,
    BillingResponseCode.FEATURE_NOT_SUPPORTED,
    -> true

    else -> false
}

/** Play's response codes, collapsed to the distinctions a product can act on. */
internal fun Int.toBillingError(): BillingError = when (this) {
    BillingResponseCode.BILLING_UNAVAILABLE,
    BillingResponseCode.FEATURE_NOT_SUPPORTED,
    -> BillingError.BillingUnavailable

    BillingResponseCode.SERVICE_DISCONNECTED,
    BillingResponseCode.SERVICE_UNAVAILABLE,
    BillingResponseCode.SERVICE_TIMEOUT,
    -> BillingError.ServiceDisconnected

    BillingResponseCode.NETWORK_ERROR -> BillingError.NetworkUnavailable

    BillingResponseCode.ITEM_UNAVAILABLE -> BillingError.ProductUnavailable

    BillingResponseCode.DEVELOPER_ERROR -> BillingError.DeveloperError

    else -> BillingError.Unknown(this)
}

/**
 * The outcome of a purchase attempt, from the response code Play reported.
 *
 * USER_CANCELED is not an error: it is the user changing their mind, and a product that shows an
 * error dialog for it is broken. ITEM_ALREADY_OWNED is not an error either — it means the user
 * already has what they were trying to buy, and the caller refreshes to make the state agree.
 */
internal fun Int.toPurchaseOutcome(): PurchaseOutcome = when (this) {
    BillingResponseCode.OK -> PurchaseOutcome.Purchased
    BillingResponseCode.USER_CANCELED -> PurchaseOutcome.Cancelled
    BillingResponseCode.ITEM_ALREADY_OWNED -> PurchaseOutcome.AlreadyOwned
    else -> PurchaseOutcome.Failed(toBillingError())
}

/**
 * The outcome implied by the purchases Play handed back with an OK response.
 *
 * An OK response with no purchases is possible — Play has acknowledged the flow but produced
 * nothing to grant — and must not be reported as a successful purchase.
 */
internal fun List<Purchase>.toPurchaseOutcome(productId: String): PurchaseOutcome =
    when (toPurchaseState(productId)) {
        PurchaseState.Purchased -> PurchaseOutcome.Purchased
        PurchaseState.Pending -> PurchaseOutcome.Pending
        PurchaseState.NotPurchased, PurchaseState.Unknown -> PurchaseOutcome.Failed(BillingError.Unknown(0))
    }

/** Price and copy come from Play, already localised and in the user's currency. */
internal fun ProductDetails.toProProduct(): ProProduct? {
    val offer = oneTimePurchaseOfferDetails ?: return null
    return ProProduct(
        productId = productId,
        title = title,
        description = description,
        formattedPrice = offer.formattedPrice,
    )
}
