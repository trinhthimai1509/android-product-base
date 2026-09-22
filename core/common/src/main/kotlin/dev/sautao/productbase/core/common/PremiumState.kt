package dev.sautao.productbase.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Whether the user has paid to remove ads — including the honest answer "not known yet".
 *
 * This exists for one reason: it lets `core:ads` hide ads for a paying user **without depending
 * on `core:billing`**. Without the seam, every ad-supported free app would link the Play Billing
 * SDK it never uses.
 *
 * It is not an entitlement framework and must not grow into one. If a product needs tiers,
 * trials or per-feature entitlements, that belongs to the product — or to a future capability
 * module with a real requirement behind it.
 *
 * Bindings:
 * - app with no purchases: [AlwaysFree]
 * - app with a Pro purchase: the implementation `core:billing` provides, backed by Google Play
 */
interface PremiumState {
    val status: Flow<PremiumStatus>

    companion object {
        /**
         * For apps that sell nothing. [PremiumStatus.FREE] immediately, because there is nothing
         * to wait for — an app with no purchases has no unresolved ownership.
         */
        val AlwaysFree: PremiumState = object : PremiumState {
            override val status: Flow<PremiumStatus> = flowOf(PremiumStatus.FREE)
        }
    }
}

/**
 * Three states, not a boolean, because "we have not asked Google Play yet" is neither yes nor no.
 *
 * A boolean forced that third case to be guessed, and the only safe guess at start-up — false —
 * meant a paying user could see an ad flash in the first second of a cold start, before Play had
 * answered. Callers must treat [UNKNOWN] as "do not yet show anything a paying user would not
 * see"; it lasts as long as it takes Play to reply, which is normally under a second.
 */
enum class PremiumStatus {
    /** Ownership has not been established yet. Temporary; suppress paid-tier-visible behaviour. */
    UNKNOWN,

    /** The user has paid. */
    PREMIUM,

    /** The user has not paid, and free-tier behaviour — advertising, upsells — is appropriate. */
    FREE,
}
