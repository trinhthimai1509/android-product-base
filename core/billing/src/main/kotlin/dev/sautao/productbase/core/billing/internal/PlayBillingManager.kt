package dev.sautao.productbase.core.billing.internal

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sautao.productbase.core.billing.BillingConfig
import dev.sautao.productbase.core.billing.BillingError
import dev.sautao.productbase.core.billing.BillingManager
import dev.sautao.productbase.core.billing.ProProduct
import dev.sautao.productbase.core.billing.PurchaseOutcome
import dev.sautao.productbase.core.billing.PurchaseState
import dev.sautao.productbase.core.common.log.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Billing"

/**
 * Play Billing behind [BillingManager].
 *
 * Nothing is logged that identifies a purchase: no purchase token, no order id, no account
 * information. Response codes and the product id are enough to diagnose a problem and are not
 * personal data.
 */
@Singleton
internal class PlayBillingManager @Inject constructor(
    @ApplicationContext context: Context,
    private val config: BillingConfig,
    private val logger: Logger,
) : BillingManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Unknown)
    override val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _proProduct = MutableStateFlow<ProProduct?>(null)
    override val proProduct: StateFlow<ProProduct?> = _proProduct.asStateFlow()

    /** Completed by the purchases listener, which is the only place a flow result arrives. */
    private var pendingPurchase: CompletableDeferred<PurchaseOutcome>? = null

    /** One connection attempt at a time; several screens may ask at once. */
    private val connectionMutex = Mutex()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        scope.launch { onPurchasesUpdated(result, purchases.orEmpty()) }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesListener)
        // Required for one-time products; without it Play rejects the flow at run time.
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        // The SDK reconnects itself. Hand-written retry loops here were a source of duplicated,
        // subtly wrong backoff code before this existed.
        .enableAutoServiceReconnection()
        .build()

    override suspend fun refreshPurchases() {
        if (!ensureConnected()) return

        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        )
        if (result.billingResult.responseCode != BillingResponseCode.OK) {
            logger.w(TAG, "Purchase query failed with code ${result.billingResult.responseCode}")
            // Deliberately not NotPurchased: a failed query says nothing about ownership, and
            // guessing "not owned" would show a paywall to someone who already paid. The one
            // exception is a code that says billing does not work on this device at all.
            resolveIfBillingImpossible(result.billingResult.responseCode)
            return
        }

        applyPurchases(result.purchasesList)
    }

    override suspend fun launchPurchase(activity: Activity): PurchaseOutcome {
        if (!ensureConnected()) return PurchaseOutcome.Failed(BillingError.ServiceDisconnected)

        val productDetails = queryProProductDetails()
            ?: return PurchaseOutcome.Failed(BillingError.ProductUnavailable)

        val deferred = CompletableDeferred<PurchaseOutcome>()
        pendingPurchase = deferred

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build(),
                ),
            )
            .build()

        val launchResult = client.launchBillingFlow(activity, params)
        if (launchResult.responseCode != BillingResponseCode.OK) {
            pendingPurchase = null
            logger.w(TAG, "Purchase flow could not start, code ${launchResult.responseCode}")
            return launchResult.responseCode.toPurchaseOutcome()
        }

        return deferred.await()
    }

    private suspend fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>) {
        val outcome = when (result.responseCode) {
            BillingResponseCode.OK -> {
                applyPurchases(purchases)
                purchases.toPurchaseOutcome(config.proProductId)
            }

            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Play knows something this app does not. Re-read ownership rather than trusting
                // the local state that produced the duplicate attempt.
                refreshPurchases()
                PurchaseOutcome.AlreadyOwned
            }

            else -> result.responseCode.toPurchaseOutcome()
        }

        if (outcome !is PurchaseOutcome.Purchased && outcome !is PurchaseOutcome.Pending) {
            logger.d(TAG, "Purchase flow ended with code ${result.responseCode}")
        }

        pendingPurchase?.complete(outcome)
        pendingPurchase = null
    }

    /**
     * Leaves [PurchaseState.Unknown] only when Play has said billing cannot work here at all.
     *
     * Without this, a device with no Play Store would sit at `Unknown` forever, and everything
     * that treats unknown ownership conservatively — advertising above all — would stay off for
     * a user who was never going to buy anything. Transient failures are untouched: they keep
     * ownership unknown, which is the whole point of that state.
     */
    private fun resolveIfBillingImpossible(responseCode: Int) {
        if (responseCode.provesNoOwnership() && _purchaseState.value == PurchaseState.Unknown) {
            logger.i(TAG, "Billing is unavailable on this device; treating ownership as none")
            _purchaseState.value = PurchaseState.NotPurchased
        }
    }

    /** Updates state and acknowledges anything Play is waiting on. */
    private suspend fun applyPurchases(purchases: List<Purchase>) {
        _purchaseState.value = purchases.toPurchaseState(config.proProductId)

        purchases.needingAcknowledgement(config.proProductId).forEach { purchase ->
            val result = client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build(),
            )
            if (result.responseCode != BillingResponseCode.OK) {
                // Not fatal: Play retries are possible on the next refresh, and the user keeps
                // what they bought in the meantime.
                logger.w(TAG, "Acknowledgement failed with code ${result.responseCode}")
            }
        }
    }

    private suspend fun queryProProductDetails() = runCatching {
        val result = client.queryProductDetails(
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(config.proProductId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build(),
                    ),
                )
                .build(),
        )
        val details = result.productDetailsList?.firstOrNull()
        _proProduct.value = details?.toProProduct()
        details
    }.getOrElse {
        logger.w(TAG, "Product details query failed", it)
        null
    }

    /**
     * Connects if needed. Called before every operation, because the service can go away at any
     * time — including across the process death that follows a low-memory kill.
     */
    private suspend fun ensureConnected(): Boolean = connectionMutex.withLock {
        if (client.isReady) return@withLock true

        val connected = CompletableDeferred<Boolean>()
        client.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode != BillingResponseCode.OK) {
                        logger.w(TAG, "Billing setup failed with code ${result.responseCode}")
                        resolveIfBillingImpossible(result.responseCode)
                    }
                    connected.complete(result.responseCode == BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    // Auto-reconnection is enabled on the client; nothing to do here beyond
                    // releasing anyone waiting on this attempt.
                    connected.complete(false)
                }
            },
        )
        connected.await()
    }

    init {
        // Warms up state as soon as the singleton is created, so a screen that opens later
        // already has an answer instead of showing Unknown while it asks.
        scope.launch {
            refreshPurchases()
            queryProProductDetails()
        }
    }
}
