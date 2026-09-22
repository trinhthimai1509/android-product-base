package dev.sautao.productbase.demo.testing

import android.app.Activity
import dev.sautao.productbase.core.ads.AdsController
import dev.sautao.productbase.core.ads.ConsentOutcome
import dev.sautao.productbase.core.ads.InterstitialOutcome
import dev.sautao.productbase.core.ads.RewardedOutcome
import dev.sautao.productbase.core.billing.BillingManager
import dev.sautao.productbase.core.billing.ProProduct
import dev.sautao.productbase.core.billing.PurchaseOutcome
import dev.sautao.productbase.core.billing.PurchaseState
import dev.sautao.productbase.core.review.AppReviewManager
import dev.sautao.productbase.core.review.ReviewOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/*
 * Fakes for the capabilities this product composes.
 *
 * They live in `demo` rather than in `core:testing` on purpose: `core:testing` is consumed by
 * every module's tests, and giving it a dependency on billing, ads and review would put those
 * SDKs on the test classpath of modules that deliberately do not link them. `core:testing` holds
 * only what is capability-free.
 */

class FakeBillingManager(initialState: PurchaseState = PurchaseState.NotPurchased) : BillingManager {
    private val _purchaseState = MutableStateFlow(initialState)
    override val purchaseState: StateFlow<PurchaseState> = _purchaseState

    override val proProduct: StateFlow<ProProduct?> = MutableStateFlow(null)

    /** What Google Play reports on the next refresh. */
    var stateAfterRefresh: PurchaseState = initialState

    var refreshCount: Int = 0
        private set

    override suspend fun refreshPurchases() {
        refreshCount++
        _purchaseState.value = stateAfterRefresh
    }

    override suspend fun launchPurchase(activity: Activity): PurchaseOutcome = PurchaseOutcome.Cancelled
}

class FakeAdsController(privacyOptionsRequired: Boolean = false) : AdsController {
    override val adsAvailable: Flow<Boolean> = MutableStateFlow(false)

    override val privacyOptionsRequired: StateFlow<Boolean> = MutableStateFlow(privacyOptionsRequired)

    override val bannerAdUnitId: String = "fake-banner"

    var privacyOptionsShown: Int = 0
        private set

    override suspend fun initialize(activity: Activity) = Unit

    override suspend fun showPrivacyOptions(activity: Activity): ConsentOutcome {
        privacyOptionsShown++
        return ConsentOutcome.Completed
    }

    override suspend fun showInterstitial(activity: Activity): InterstitialOutcome = InterstitialOutcome.NotAvailable

    override suspend fun showRewarded(activity: Activity): RewardedOutcome = RewardedOutcome.NotAvailable

    override fun recordQualifyingAction() = Unit
}

class FakeAppReviewManager(var outcome: ReviewOutcome = ReviewOutcome.Completed) : AppReviewManager {
    var requests: Int = 0
        private set

    override suspend fun requestReview(activity: Activity): ReviewOutcome {
        requests++
        return outcome
    }
}
