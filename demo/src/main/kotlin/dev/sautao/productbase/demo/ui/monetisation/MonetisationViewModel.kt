package dev.sautao.productbase.demo.ui.monetisation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sautao.productbase.core.ads.AdsController
import dev.sautao.productbase.core.ads.InterstitialOutcome
import dev.sautao.productbase.core.ads.RewardedOutcome
import dev.sautao.productbase.core.common.PremiumState
import dev.sautao.productbase.core.common.PremiumStatus
import dev.sautao.productbase.core.telemetry.Analytics
import dev.sautao.productbase.core.telemetry.AnalyticsEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonetisationUiState(
    val adsAvailable: Boolean = false,
    val premiumStatus: PremiumStatus = PremiumStatus.UNKNOWN,
    val privacyOptionsRequired: Boolean = false,
    val qualifyingActions: Int = 0,
    val lastInterstitial: InterstitialOutcome? = null,
    val lastRewarded: RewardedOutcome? = null,
    /** Granted by the product, never by core:ads — this is the reward's *meaning*. */
    val hints: Int = 0,
)

/**
 * The demo's monetisation surface.
 *
 * Note what this class imports: `AdsController`, `PremiumState`, `Analytics`. No Google Mobile
 * Ads type, no UMP type, no Play Billing type. That is the whole point of Phase 4.
 */
@HiltViewModel
class MonetisationViewModel @Inject constructor(
    /** Exposed for the banner Composable, which needs the controller itself. */
    val adsController: AdsController,
    private val analytics: Analytics,
    premiumState: PremiumState,
) : ViewModel() {
    private val local = MutableStateFlow(MonetisationUiState())

    val uiState: StateFlow<MonetisationUiState> = combine(
        adsController.adsAvailable,
        premiumState.status,
        adsController.privacyOptionsRequired,
        local,
    ) { adsAvailable, premiumStatus, privacyRequired, localState ->
        localState.copy(
            adsAvailable = adsAvailable,
            premiumStatus = premiumStatus,
            privacyOptionsRequired = privacyRequired,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MonetisationUiState(),
    )

    /** Stands in for whatever a real product counts as meaningful use. */
    fun recordQualifyingAction() {
        adsController.recordQualifyingAction()
        local.value = local.value.copy(qualifyingActions = local.value.qualifyingActions + 1)
    }

    fun showInterstitial(activity: Activity) {
        viewModelScope.launch {
            val outcome = adsController.showInterstitial(activity)
            if (outcome is InterstitialOutcome.Shown) {
                local.value = local.value.copy(qualifyingActions = 0)
            }
            local.value = local.value.copy(lastInterstitial = outcome)
            analytics.logEvent(
                AnalyticsEvent(
                    name = "ad_interstitial_result",
                    params = mapOf("outcome" to outcome.name()),
                ),
            )
        }
    }

    fun showRewarded(activity: Activity) {
        viewModelScope.launch {
            val outcome = adsController.showRewarded(activity)
            local.value = local.value.copy(
                lastRewarded = outcome,
                // The product decides what a reward is worth. core:ads only reported that one
                // was earned; granting a hint is this app's business rule.
                hints = if (outcome is RewardedOutcome.Earned) {
                    local.value.hints + outcome.amount
                } else {
                    local.value.hints
                },
            )
            analytics.logEvent(
                AnalyticsEvent(
                    name = "ad_rewarded_result",
                    params = mapOf("earned" to (outcome is RewardedOutcome.Earned)),
                ),
            )
        }
    }

    fun showPrivacyOptions(activity: Activity) {
        viewModelScope.launch { adsController.showPrivacyOptions(activity) }
    }

    fun onPaywallViewed() {
        analytics.logScreenView("premium")
        analytics.logEvent(AnalyticsEvent(name = "paywall_viewed"))
    }

    private fun InterstitialOutcome.name(): String = when (this) {
        InterstitialOutcome.Shown -> "shown"
        InterstitialOutcome.NotAvailable -> "not_available"
        InterstitialOutcome.Throttled -> "throttled"
        is InterstitialOutcome.Failed -> "failed"
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
