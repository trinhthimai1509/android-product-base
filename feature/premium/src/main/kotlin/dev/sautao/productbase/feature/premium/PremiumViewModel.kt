package dev.sautao.productbase.feature.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sautao.productbase.core.billing.BillingError
import dev.sautao.productbase.core.billing.BillingManager
import dev.sautao.productbase.core.billing.ProProduct
import dev.sautao.productbase.core.billing.PurchaseState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the paywall shows.
 *
 * [message] is the outcome of the last action, if any. It is a value the screen maps to text —
 * no string resource, and no `Context`, reaches this class.
 */
data class PremiumUiState(
    val purchaseState: PurchaseState = PurchaseState.Unknown,
    val product: ProProduct? = null,
    val isWorking: Boolean = false,
    val message: PremiumMessage? = null,
)

/** The few things worth telling the user after a purchase or restore attempt. */
sealed interface PremiumMessage {
    data object PurchaseCompleted : PremiumMessage

    data object PurchasePending : PremiumMessage

    data object AlreadyOwned : PremiumMessage

    data object NothingToRestore : PremiumMessage

    /**
     * Google Play could not be reached, so ownership is still unknown.
     *
     * Distinct from [NothingToRestore] on purpose: telling a paying customer that no purchase
     * was found, when the truth is that nobody asked, is the worst message this screen can show.
     */
    data object CouldNotCheck : PremiumMessage

    data class Failed(val error: BillingError) : PremiumMessage
}

@HiltViewModel
class PremiumViewModel @Inject constructor(private val billingManager: BillingManager) : ViewModel() {
    private val isWorking = MutableStateFlow(false)
    private val message = MutableStateFlow<PremiumMessage?>(null)

    val uiState: StateFlow<PremiumUiState> = combine(
        billingManager.purchaseState,
        billingManager.proProduct,
        isWorking,
        message,
    ) { purchaseState, product, working, currentMessage ->
        PremiumUiState(
            purchaseState = purchaseState,
            product = product,
            isWorking = working,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PremiumUiState(),
    )

    fun purchase(activity: Activity) {
        if (isWorking.value) return
        isWorking.value = true
        message.value = null

        viewModelScope.launch {
            message.value = billingManager.launchPurchase(activity).toMessage()
            isWorking.value = false
        }
    }

    /**
     * Re-reads ownership from Google Play.
     *
     * "Restore" is the same operation as a routine refresh — there is nothing stored locally to
     * restore *from*, only ownership to re-read from the authority.
     */
    fun restore() {
        if (isWorking.value) return
        isWorking.value = true
        message.value = null

        viewModelScope.launch {
            billingManager.refreshPurchases()
            message.value = billingManager.purchaseState.value.toRestoreMessage()
            isWorking.value = false
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
