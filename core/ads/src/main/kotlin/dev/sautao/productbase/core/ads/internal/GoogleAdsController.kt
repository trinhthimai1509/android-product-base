package dev.sautao.productbase.core.ads.internal

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sautao.productbase.core.ads.AdsConfig
import dev.sautao.productbase.core.ads.AdsController
import dev.sautao.productbase.core.ads.ConsentOutcome
import dev.sautao.productbase.core.ads.InterstitialOutcome
import dev.sautao.productbase.core.ads.RewardedOutcome
import dev.sautao.productbase.core.common.PremiumState
import dev.sautao.productbase.core.common.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "Ads"

/**
 * Google Mobile Ads behind [AdsController].
 *
 * Every SDK type stays inside this class and the consent gateway beside it.
 */
@Singleton
internal class GoogleAdsController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: AdsConfig,
    private val consent: UmpConsentGateway,
    premiumState: PremiumState,
    private val logger: Logger,
) : AdsController {
    private val governor = InterstitialFrequencyGovernor(config.interstitialPolicy, Clock.systemUTC())

    private val consentResolved = MutableStateFlow(false)
    private val _privacyOptionsRequired = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private val initialisationMutex = Mutex()
    private var initialised = false

    /**
     * Test ad units in a non-debuggable build would serve test ads to real users — an AdMob
     * policy violation and zero revenue. Advertising is disabled rather than allowed to ship
     * misconfigured, and the mistake is logged loudly.
     */
    private val misconfiguredForRelease: Boolean = run {
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val wrong = !debuggable && config.usesTestAdUnits
        if (wrong) {
            logger.e(TAG, "Test ad units in a non-debuggable build: advertising disabled")
        }
        wrong
    }

    override val bannerAdUnitId: String get() = config.bannerAdUnitId

    override val adsAvailable: Flow<Boolean> =
        combine(premiumState.status, consentResolved) { premiumStatus, consentOk ->
            adsAvailable(
                adsEnabled = config.adsEnabled,
                misconfiguredForRelease = misconfiguredForRelease,
                consentAllowsRequests = consentOk,
                premiumStatus = premiumStatus,
            )
        }.distinctUntilChanged()

    override suspend fun initialize(activity: Activity) {
        if (!config.adsEnabled || misconfiguredForRelease) return

        initialisationMutex.withLock {
            // Consent first, always: the SDK must not be initialised, and no ad requested, until
            // UMP has said whether this user may be asked for ads at all.
            consent.gatherConsent(activity)
            _privacyOptionsRequired.value = consent.privacyOptionsRequired
            consentResolved.value = consent.canRequestAds

            if (!consent.canRequestAds || initialised) return@withLock

            withContext(Dispatchers.IO) { MobileAds.initialize(context) {} }
            initialised = true
        }
    }

    override suspend fun showPrivacyOptions(activity: Activity): ConsentOutcome {
        val outcome = consent.showPrivacyOptions(activity)
        // The user may have just withdrawn consent, which takes ads away immediately.
        consentResolved.value = consent.canRequestAds
        _privacyOptionsRequired.value = consent.privacyOptionsRequired
        return outcome
    }

    override fun recordQualifyingAction() = governor.recordQualifyingAction()

    override suspend fun showInterstitial(activity: Activity): InterstitialOutcome {
        if (!adsAvailable.first()) return InterstitialOutcome.NotAvailable
        if (!governor.canShow()) return InterstitialOutcome.Throttled

        val ad = loadInterstitial() ?: return InterstitialOutcome.Failed(lastLoadErrorCode)

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        continuation.resume(InterstitialOutcome.Shown)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        logger.w(TAG, "Interstitial failed to show, code ${error.code}")
                        continuation.resume(InterstitialOutcome.Failed(error.code))
                    }

                    override fun onAdShowedFullScreenContent() {
                        // Recorded when it is genuinely on screen, so a run of failed loads
                        // cannot silence advertising for the rest of the session.
                        governor.recordShown()
                    }
                }
                ad.show(activity)
            }
        }
    }

    override suspend fun showRewarded(activity: Activity): RewardedOutcome {
        if (!adsAvailable.first()) return RewardedOutcome.NotAvailable

        val ad = loadRewarded() ?: return RewardedOutcome.Failed(lastLoadErrorCode)

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                var earned: RewardedOutcome.Earned? = null

                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        // Resolved on dismissal, not on the reward callback: the reward arrives
                        // first, and a product must not act on it while the ad is still on screen.
                        continuation.resume(earned ?: RewardedOutcome.Dismissed)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        logger.w(TAG, "Rewarded ad failed to show, code ${error.code}")
                        continuation.resume(RewardedOutcome.Failed(error.code))
                    }
                }

                ad.show(activity) { rewardItem ->
                    earned = RewardedOutcome.Earned(rewardItem.amount, rewardItem.type)
                }
            }
        }
    }

    private var lastLoadErrorCode: Int = 0

    private suspend fun loadInterstitial(): InterstitialAd? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            InterstitialAd.load(
                context,
                config.interstitialAdUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) = continuation.resume(ad)

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        logger.d(TAG, "Interstitial load failed, code ${error.code}")
                        lastLoadErrorCode = error.code
                        continuation.resume(null)
                    }
                },
            )
        }
    }

    private suspend fun loadRewarded(): RewardedAd? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            RewardedAd.load(
                context,
                config.rewardedAdUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) = continuation.resume(ad)

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        logger.d(TAG, "Rewarded load failed, code ${error.code}")
                        lastLoadErrorCode = error.code
                        continuation.resume(null)
                    }
                },
            )
        }
    }
}
