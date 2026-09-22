package dev.sautao.productbase.core.ads

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The product-facing advertising API.
 *
 * No Google Mobile Ads or UMP type appears here or anywhere outside this module. Product code
 * decides *when* an ad is appropriate; this module decides whether it is allowed and how to
 * ask for it.
 */
interface AdsController {
    /**
     * Whether an ad may be shown at all: advertising is enabled, consent allows a request, and
     * the user has not paid to remove ads.
     */
    val adsAvailable: Flow<Boolean>

    /**
     * True when the user's region requires an ongoing way to change their consent choice. A
     * product that shows ads must offer [showPrivacyOptions] somewhere reachable — usually
     * settings — whenever this is true.
     */
    val privacyOptionsRequired: StateFlow<Boolean>

    /** The unit the banner Composable should request. */
    val bannerAdUnitId: String

    /**
     * Resolves consent and then initialises the ads SDK. Call once from the first Activity.
     *
     * Safe to call again; safe to fail. If consent cannot be resolved, ads stay unavailable and
     * the app carries on.
     */
    suspend fun initialize(activity: Activity)

    /** Shows the consent form again so the user can change their choice. */
    suspend fun showPrivacyOptions(activity: Activity): ConsentOutcome

    /**
     * Shows an interstitial if one is allowed *and* the frequency policy permits it.
     *
     * The product chooses the moment — after a task completes, never mid-task and never on a
     * back press.
     */
    suspend fun showInterstitial(activity: Activity): InterstitialOutcome

    /**
     * Shows a rewarded ad and reports whether the reward was earned.
     *
     * What the reward *means* is the product's business: this module never grants anything.
     */
    suspend fun showRewarded(activity: Activity): RewardedOutcome

    /**
     * Tells the frequency policy that the user completed something worth counting.
     *
     * The product decides what counts — a saved entry, a finished calculation — because only the
     * product knows which actions represent real use.
     */
    fun recordQualifyingAction()
}

sealed interface InterstitialOutcome {
    data object Shown : InterstitialOutcome

    /** Ads are off for this user: premium, disabled, or consent not given. */
    data object NotAvailable : InterstitialOutcome

    /** Allowed, but too soon — the frequency policy said no. Not an error. */
    data object Throttled : InterstitialOutcome

    /** Requested but nothing came back in time; no fill, or a network problem. */
    data class Failed(val code: Int) : InterstitialOutcome
}

sealed interface RewardedOutcome {
    /**
     * The user watched enough to earn the reward. [amount] and [type] are what the ad unit was
     * configured with in AdMob; granting anything is the product's decision.
     */
    data class Earned(val amount: Int, val type: String) : RewardedOutcome

    /** The user closed the ad without earning. */
    data object Dismissed : RewardedOutcome

    data object NotAvailable : RewardedOutcome

    data class Failed(val code: Int) : RewardedOutcome
}

/** The result of a consent interaction. Deliberately coarse: a product cannot act on more. */
sealed interface ConsentOutcome {
    data object Completed : ConsentOutcome

    data object NotRequired : ConsentOutcome

    data class Failed(val code: Int) : ConsentOutcome
}
