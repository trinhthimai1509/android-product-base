package dev.sautao.productbase.demo.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sautao.productbase.core.ads.AdsController
import dev.sautao.productbase.core.billing.BillingManager
import dev.sautao.productbase.core.billing.PurchaseState
import dev.sautao.productbase.core.datastore.AppPreferences
import dev.sautao.productbase.core.review.AppReviewManager
import dev.sautao.productbase.core.review.ReviewOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the demo's settings screen needs to decide which rows exist.
 *
 * [purchaseState] and [privacyOptionsRequired] are the two genuinely capability-driven pieces:
 * the first comes from `core:billing`, the second from `core:ads`. A product that includes
 * neither module builds neither row, and `feature:settings` never learns that either capability
 * is a thing — see ARCHITECTURE_PLAN.md §20.3.
 */
data class DemoSettingsUiState(
    val notificationsEnabled: Boolean = true,
    val privacyOptionsRequired: Boolean = false,
    val purchaseState: PurchaseState = PurchaseState.Unknown,
    val message: DemoSettingsMessage? = null,
)

/** The few outcomes worth telling the user about. Mapped to text by the screen, never here. */
sealed interface DemoSettingsMessage {
    data object PurchaseRestored : DemoSettingsMessage

    data object NothingToRestore : DemoSettingsMessage

    /** Play could not be reached. Not the same as owning nothing, and never worded as if it were. */
    data object CouldNotCheckPurchases : DemoSettingsMessage

    data object ReviewUnavailable : DemoSettingsMessage

    /** No app on the device could take the share, or open the link. */
    data object NothingToHandleAction : DemoSettingsMessage
}

/**
 * The product-level composition Phase 5 is about.
 *
 * This class is where `core:billing`, `core:ads`, `core:review` and `core:datastore` meet, and it
 * is the *only* place they meet: `feature:settings` receives values and lambdas, and imports none
 * of them. Note also that nothing here navigates — the paywall is a route in the NavHost, so
 * there is no `feature:settings` → `feature:premium` edge to be tempted by.
 */
@HiltViewModel
class DemoSettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val billingManager: BillingManager,
    private val adsController: AdsController,
    private val appReviewManager: AppReviewManager,
) : ViewModel() {
    private val message = MutableStateFlow<DemoSettingsMessage?>(null)

    val uiState: StateFlow<DemoSettingsUiState> = combine(
        appPreferences.notificationsEnabled,
        adsController.privacyOptionsRequired,
        billingManager.purchaseState,
        message,
    ) { notificationsEnabled, privacyOptionsRequired, purchaseState, currentMessage ->
        DemoSettingsUiState(
            notificationsEnabled = notificationsEnabled,
            privacyOptionsRequired = privacyOptionsRequired,
            purchaseState = purchaseState,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DemoSettingsUiState(),
    )

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setNotificationsEnabled(enabled) }
    }

    /** Re-reads ownership from Play. "Restore" and "refresh" are the same operation. */
    fun restorePurchases() {
        viewModelScope.launch {
            billingManager.refreshPurchases()
            message.value = when (billingManager.purchaseState.value) {
                PurchaseState.Purchased -> DemoSettingsMessage.PurchaseRestored

                PurchaseState.NotPurchased -> DemoSettingsMessage.NothingToRestore

                // Pending means a payment is under way; there is nothing to restore yet, and
                // "no purchase found" would be a lie.
                PurchaseState.Pending, PurchaseState.Unknown -> DemoSettingsMessage.CouldNotCheckPurchases
            }
        }
    }

    /**
     * The product decides the moment. A settings row is a defensible one — the user went looking
     * for it — which is why this is wired here and not inside `core:review`.
     */
    fun requestReview(activity: Activity) {
        viewModelScope.launch {
            if (appReviewManager.requestReview(activity) == ReviewOutcome.Unavailable) {
                message.value = DemoSettingsMessage.ReviewUnavailable
            }
        }
    }

    fun showPrivacyOptions(activity: Activity) {
        viewModelScope.launch { adsController.showPrivacyOptions(activity) }
    }

    /** Demo-only: puts the app back to its first-launch state so onboarding can be verified. */
    fun resetOnboarding() {
        viewModelScope.launch { appPreferences.setOnboardingCompleted(false) }
    }

    /** Reported by the screen when an intent found nothing that could handle it. */
    fun onActionUnhandled() {
        message.value = DemoSettingsMessage.NothingToHandleAction
    }

    fun dismissMessage() {
        message.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
