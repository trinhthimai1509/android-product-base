package dev.sautao.productbase.core.ads.internal

import dev.sautao.productbase.core.common.PremiumStatus

/**
 * Whether an ad may be shown, as a pure function of the four things that decide it.
 *
 * Extracted from the controller so the rule can be tested without the Mobile Ads SDK, a Context
 * or a device. Every condition is a veto — this is the one place where "should this user see an
 * ad" is answered, and getting it wrong either annoys a paying customer or breaks AdMob policy.
 *
 * [PremiumStatus.UNKNOWN] is a veto too, and that is the point: during a cold start, ownership
 * is unknown until Google Play answers. Showing an ad in that window means a user who paid to
 * remove ads sees one every time they open the app, which is the single most effective way to
 * turn a paying customer into a refund request. Suppressing for the fraction of a second it
 * takes Play to reply costs an impression; the alternative costs the customer.
 */
internal fun adsAvailable(
    adsEnabled: Boolean,
    misconfiguredForRelease: Boolean,
    consentAllowsRequests: Boolean,
    premiumStatus: PremiumStatus,
): Boolean = adsEnabled &&
    !misconfiguredForRelease &&
    consentAllowsRequests &&
    premiumStatus == PremiumStatus.FREE
