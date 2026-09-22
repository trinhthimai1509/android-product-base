package dev.sautao.productbase.core.billing.internal

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.Purchase
import dev.sautao.productbase.core.billing.BillingError
import dev.sautao.productbase.core.billing.PurchaseOutcome
import dev.sautao.productbase.core.billing.PurchaseState
import dev.sautao.productbase.core.common.PremiumStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val PRO = "pro_lifetime"

// Play's JSON encodes a pending purchase as 4, which does not match the value of
// Purchase.PurchaseState.PENDING (2) — the SDK translates one to the other on the way in.
// Getting this wrong makes a pending purchase look like ownership, which is why it is spelled
// out here rather than left as a literal.
private const val JSON_PENDING = 4
private const val JSON_PURCHASED = 1

/**
 * Exercised with real `Purchase` objects built from the JSON Play actually returns, rather than
 * an imitation of the SDK: the constructor is public, so the parsing and the mapping are both
 * under test.
 */
class BillingMappingTest {
    private fun purchase(
        productId: String = PRO,
        state: Int = Purchase.PurchaseState.PURCHASED,
        acknowledged: Boolean = true,
    ): Purchase = Purchase(
        """
        {
          "orderId": "GPA.0000-0000-0000-00000",
          "packageName": "dev.sautao.productbase.demo",
          "productId": "$productId",
          "purchaseTime": 1755000000000,
          "purchaseState": ${if (state == Purchase.PurchaseState.PENDING) JSON_PENDING else JSON_PURCHASED},
          "purchaseToken": "token-not-logged-anywhere",
          "acknowledged": $acknowledged
        }
        """.trimIndent(),
        "signature",
    )

    @Test
    fun `an acknowledged purchase of the product is ownership`() {
        assertEquals(PurchaseState.Purchased, listOf(purchase()).toPurchaseState(PRO))
    }

    @Test
    fun `an unacknowledged purchase is still ownership`() {
        // Play has taken the money. Acknowledgement is this app's obligation, not a condition of
        // the user owning what they bought.
        assertEquals(
            PurchaseState.Purchased,
            listOf(purchase(acknowledged = false)).toPurchaseState(PRO),
        )
    }

    @Test
    fun `a pending purchase is not ownership`() {
        assertEquals(
            PurchaseState.Pending,
            listOf(purchase(state = Purchase.PurchaseState.PENDING)).toPurchaseState(PRO),
        )
    }

    @Test
    fun `no purchases means not purchased`() {
        assertEquals(PurchaseState.NotPurchased, emptyList<Purchase>().toPurchaseState(PRO))
    }

    @Test
    fun `a purchase of some other product is not ownership of this one`() {
        assertEquals(
            PurchaseState.NotPurchased,
            listOf(purchase(productId = "some_other_product")).toPurchaseState(PRO),
        )
    }

    @Test
    fun `only unacknowledged purchased items need acknowledgement`() {
        val purchases = listOf(
            purchase(acknowledged = true),
            purchase(productId = "other", acknowledged = false),
            purchase(state = Purchase.PurchaseState.PENDING, acknowledged = false),
        )

        assertTrue(purchases.needingAcknowledgement(PRO).isEmpty())
        assertEquals(1, listOf(purchase(acknowledged = false)).needingAcknowledgement(PRO).size)
    }

    @Test
    fun `cancelling is not an error`() {
        assertEquals(PurchaseOutcome.Cancelled, BillingResponseCode.USER_CANCELED.toPurchaseOutcome())
    }

    @Test
    fun `already owned is not an error either`() {
        assertEquals(
            PurchaseOutcome.AlreadyOwned,
            BillingResponseCode.ITEM_ALREADY_OWNED.toPurchaseOutcome(),
        )
    }

    @Test
    fun `an OK response with no purchases is not a successful purchase`() {
        // Play can answer OK and hand back nothing. Reporting that as a purchase would grant the
        // product for free.
        assertEquals(
            PurchaseOutcome.Failed(BillingError.Unknown(0)),
            emptyList<Purchase>().toPurchaseOutcome(PRO),
        )
    }

    @Test
    fun `a pending result from the flow is reported as pending, not purchased`() {
        assertEquals(
            PurchaseOutcome.Pending,
            listOf(purchase(state = Purchase.PurchaseState.PENDING)).toPurchaseOutcome(PRO),
        )
    }

    @Test
    fun `response codes map to actionable errors`() {
        assertEquals(BillingError.BillingUnavailable, BillingResponseCode.BILLING_UNAVAILABLE.toBillingError())
        assertEquals(BillingError.BillingUnavailable, BillingResponseCode.FEATURE_NOT_SUPPORTED.toBillingError())
        assertEquals(BillingError.ServiceDisconnected, BillingResponseCode.SERVICE_DISCONNECTED.toBillingError())
        assertEquals(BillingError.ServiceDisconnected, BillingResponseCode.SERVICE_UNAVAILABLE.toBillingError())
        assertEquals(BillingError.ServiceDisconnected, BillingResponseCode.SERVICE_TIMEOUT.toBillingError())
        assertEquals(BillingError.NetworkUnavailable, BillingResponseCode.NETWORK_ERROR.toBillingError())
        assertEquals(BillingError.ProductUnavailable, BillingResponseCode.ITEM_UNAVAILABLE.toBillingError())
        assertEquals(BillingError.DeveloperError, BillingResponseCode.DEVELOPER_ERROR.toBillingError())
        assertEquals(BillingError.Unknown(BillingResponseCode.ERROR), BillingResponseCode.ERROR.toBillingError())
    }

    @Test
    fun `only a completed purchase is premium`() {
        assertEquals(PremiumStatus.PREMIUM, PurchaseState.Purchased.toPremiumStatus())
    }

    @Test
    fun `unknown ownership stays unknown rather than becoming free`() {
        // The regression this guards: mapping Unknown to "not premium" let a paying user see an
        // ad during the first second of a cold start.
        assertEquals(PremiumStatus.UNKNOWN, PurchaseState.Unknown.toPremiumStatus())
    }

    @Test
    fun `not purchased is free`() {
        assertEquals(PremiumStatus.FREE, PurchaseState.NotPurchased.toPremiumStatus())
    }

    @Test
    fun `a pending purchase is not yet premium`() {
        // Documented policy: the money has not moved, so the paid experience is not granted and
        // free-tier behaviour continues until Play says the payment cleared.
        assertEquals(PremiumStatus.FREE, PurchaseState.Pending.toPremiumStatus())
    }

    @Test
    fun `only permanent unavailability proves the user owns nothing`() {
        assertTrue(BillingResponseCode.BILLING_UNAVAILABLE.provesNoOwnership())
        assertTrue(BillingResponseCode.FEATURE_NOT_SUPPORTED.provesNoOwnership())

        // Transient failures must leave ownership unknown: a paying user is on the other end of
        // exactly these codes.
        assertFalse(BillingResponseCode.SERVICE_DISCONNECTED.provesNoOwnership())
        assertFalse(BillingResponseCode.SERVICE_UNAVAILABLE.provesNoOwnership())
        assertFalse(BillingResponseCode.SERVICE_TIMEOUT.provesNoOwnership())
        assertFalse(BillingResponseCode.NETWORK_ERROR.provesNoOwnership())
        assertFalse(BillingResponseCode.ERROR.provesNoOwnership())
    }
}
