package dev.sautao.productbase.core.billing

/**
 * The product the app sells. Supplied by the product; this module ships no product id and no
 * catalogue.
 *
 * One id, because the current requirement is one lifetime "remove ads / Pro" purchase. When
 * subscriptions arrive, this gains a field and [BillingManager] gains a method — neither
 * requires the public API to be rewritten.
 */
data class BillingConfig(val proProductId: String)

/**
 * Whether the user owns the Pro product, according to Google Play.
 *
 * Google Play is authoritative. Nothing cached locally may stand in for this: a cache can make
 * the first frame correct, it cannot prove ownership.
 */
sealed interface PurchaseState {
    /**
     * Not determined yet — the very first moments after start-up, or while the billing service
     * is unreachable. Distinct from [NotPurchased] on purpose: showing a paying user a paywall
     * because the service was briefly down is worse than showing nothing for a moment.
     */
    data object Unknown : PurchaseState

    data object NotPurchased : PurchaseState

    /**
     * Payment is in progress out of band — cash at a kiosk, or a parent's approval. The user does
     * not own the product yet and must not be granted it.
     */
    data object Pending : PurchaseState

    data object Purchased : PurchaseState
}

/**
 * The outcome of one purchase attempt.
 *
 * Separate from [PurchaseState] because it answers a different question: state is "what does the
 * user own", outcome is "what happened when they just tapped Buy". A cancelled purchase changes
 * the outcome and not the state.
 */
sealed interface PurchaseOutcome {
    data object Purchased : PurchaseOutcome

    data object Pending : PurchaseOutcome

    /** Play reports the product as already owned; the state is refreshed to match. */
    data object AlreadyOwned : PurchaseOutcome

    /** The user backed out. Not an error, and not worth an error dialog. */
    data object Cancelled : PurchaseOutcome

    data class Failed(val error: BillingError) : PurchaseOutcome
}

/**
 * Why a billing operation failed, in terms a product can act on.
 *
 * Google's response codes are collapsed to the distinctions that change what a user should be
 * told or what the app should do next.
 */
sealed interface BillingError {
    /** No Play Store, an old one, or an unsupported country. Purchases are impossible here. */
    data object BillingUnavailable : BillingError

    /** Transient: the service dropped. Retrying later is reasonable. */
    data object ServiceDisconnected : BillingError

    data object NetworkUnavailable : BillingError

    /** The product id is not configured, or not active, in Play Console. */
    data object ProductUnavailable : BillingError

    /** A mistake in this app's integration. Should never reach a user in production. */
    data object DeveloperError : BillingError

    data class Unknown(val code: Int) : BillingError
}

/**
 * What the paywall needs in order to render.
 *
 * [formattedPrice] comes from Play, already in the user's currency and locale — never format a
 * price yourself, and never hardcode one.
 */
data class ProProduct(val productId: String, val title: String, val description: String, val formattedPrice: String)
