package dev.sautao.productbase.feature.premium

import app.cash.turbine.test
import dev.sautao.productbase.core.billing.BillingError
import dev.sautao.productbase.core.billing.PurchaseOutcome
import dev.sautao.productbase.core.billing.PurchaseState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PremiumViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `exposes the price Google Play supplied, never one of its own`() = runTest {
        val viewModel = PremiumViewModel(FakeBillingManager())

        viewModel.uiState.test {
            assertEquals("€4.99", awaitItem().product?.formattedPrice)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an unavailable product leaves the paywall with nothing to sell`() = runTest {
        // What a device with no Play Store, or an unconfigured product id, looks like.
        val viewModel = PremiumViewModel(FakeBillingManager(initialProduct = null))

        viewModel.uiState.test {
            assertNull(awaitItem().product)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restore finds an existing purchase and says so`() = runTest {
        val billing = FakeBillingManager().apply { stateAfterRefresh = PurchaseState.Purchased }
        val viewModel = PremiumViewModel(billing)

        // Collected, not read: uiState is WhileSubscribed, so with no collector it never leaves
        // its initial value.
        viewModel.uiState.test {
            awaitItem()
            viewModel.restore()
            val state = expectMostRecentItem()

            assertEquals(1, billing.refreshCount)
            assertEquals(PremiumMessage.PurchaseCompleted, state.message)
            assertEquals(PurchaseState.Purchased, state.purchaseState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restore reports honestly when there is nothing to restore`() = runTest {
        val billing = FakeBillingManager().apply { stateAfterRefresh = PurchaseState.NotPurchased }
        val viewModel = PremiumViewModel(billing)

        viewModel.uiState.test {
            awaitItem()
            viewModel.restore()

            assertEquals(PremiumMessage.NothingToRestore, expectMostRecentItem().message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restore re-reads from Play rather than trusting what it already had`() = runTest {
        // The user was premium a moment ago; Play now says otherwise — a refund, or a different
        // account. The refreshed answer wins.
        val billing = FakeBillingManager(initialState = PurchaseState.Purchased).apply {
            stateAfterRefresh = PurchaseState.NotPurchased
        }
        val viewModel = PremiumViewModel(billing)

        viewModel.uiState.test {
            awaitItem()
            viewModel.restore()

            assertEquals(PurchaseState.NotPurchased, expectMostRecentItem().purchaseState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissing clears the message`() = runTest {
        val billing = FakeBillingManager().apply { stateAfterRefresh = PurchaseState.Purchased }
        val viewModel = PremiumViewModel(billing)

        viewModel.uiState.test {
            awaitItem()
            viewModel.restore()
            viewModel.dismissMessage()

            assertNull(expectMostRecentItem().message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `purchase outcomes become the right message`() {
        assertEquals(PremiumMessage.PurchaseCompleted, PurchaseOutcome.Purchased.toMessage())
        assertEquals(PremiumMessage.PurchasePending, PurchaseOutcome.Pending.toMessage())
        assertEquals(PremiumMessage.AlreadyOwned, PurchaseOutcome.AlreadyOwned.toMessage())
        // The important one: cancelling produces no message at all.
        assertNull(PurchaseOutcome.Cancelled.toMessage())
        assertEquals(
            PremiumMessage.Failed(BillingError.BillingUnavailable),
            PurchaseOutcome.Failed(BillingError.BillingUnavailable).toMessage(),
        )
    }

    @Test
    fun `an unknown purchase state is reported as unknown, not as nothing owned`() {
        // Found on device: with Play unreachable the state stays Unknown, and saying "no purchase
        // found" would tell a paying customer they own nothing when nobody actually asked.
        assertEquals(PremiumMessage.CouldNotCheck, PurchaseState.Unknown.toRestoreMessage())
        assertEquals(PremiumMessage.NothingToRestore, PurchaseState.NotPurchased.toRestoreMessage())
        assertEquals(PremiumMessage.PurchasePending, PurchaseState.Pending.toRestoreMessage())
        assertEquals(PremiumMessage.PurchaseCompleted, PurchaseState.Purchased.toRestoreMessage())
    }
}
