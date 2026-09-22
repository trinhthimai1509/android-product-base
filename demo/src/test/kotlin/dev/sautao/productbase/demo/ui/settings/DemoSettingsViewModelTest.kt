package dev.sautao.productbase.demo.ui.settings

import app.cash.turbine.test
import dev.sautao.productbase.core.billing.PurchaseState
import dev.sautao.productbase.core.review.ReviewOutcome
import dev.sautao.productbase.core.testing.FakeAppPreferences
import dev.sautao.productbase.core.testing.MainDispatcherRule
import dev.sautao.productbase.demo.testing.FakeAdsController
import dev.sautao.productbase.demo.testing.FakeAppReviewManager
import dev.sautao.productbase.demo.testing.FakeBillingManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DemoSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `capability state reaches the screen`() = runTest {
        val viewModel = viewModel(
            billing = FakeBillingManager(PurchaseState.Purchased),
            ads = FakeAdsController(privacyOptionsRequired = true),
            preferences = FakeAppPreferences(initialNotificationsEnabled = false),
        )

        viewModel.uiState.test {
            val state = awaitItem()

            assertEquals(PurchaseState.Purchased, state.purchaseState)
            assertTrue(state.privacyOptionsRequired)
            assertFalse(state.notificationsEnabled)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the notification switch is persisted`() = runTest {
        val preferences = FakeAppPreferences(initialNotificationsEnabled = true)
        val viewModel = viewModel(preferences = preferences)

        viewModel.uiState.test {
            assertTrue(awaitItem().notificationsEnabled)

            viewModel.setNotificationsEnabled(false)

            assertFalse(awaitItem().notificationsEnabled)
            assertFalse(preferences.notificationsEnabled.value)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restore reports a purchase Play knows about`() = runTest {
        val billing = FakeBillingManager().apply { stateAfterRefresh = PurchaseState.Purchased }
        val viewModel = viewModel(billing = billing)

        viewModel.uiState.test {
            awaitItem()
            viewModel.restorePurchases()

            assertEquals(DemoSettingsMessage.PurchaseRestored, awaitItem().message)
            assertEquals(1, billing.refreshCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restore says nothing was found only when Play actually answered`() = runTest {
        val billing = FakeBillingManager().apply { stateAfterRefresh = PurchaseState.NotPurchased }
        val viewModel = viewModel(billing = billing)

        viewModel.uiState.test {
            awaitItem()
            viewModel.restorePurchases()

            assertEquals(DemoSettingsMessage.NothingToRestore, awaitItem().message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `restore does not tell a paying user they own nothing when Play was unreachable`() = runTest {
        val billing = FakeBillingManager(PurchaseState.Unknown).apply {
            stateAfterRefresh = PurchaseState.Unknown
        }
        val viewModel = viewModel(billing = billing)

        viewModel.uiState.test {
            awaitItem()
            viewModel.restorePurchases()

            assertEquals(DemoSettingsMessage.CouldNotCheckPurchases, awaitItem().message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a review Play declined is reported, not crashed on`() = runTest {
        val review = FakeAppReviewManager(outcome = ReviewOutcome.Unavailable)
        val viewModel = viewModel(review = review)

        viewModel.uiState.test {
            awaitItem()
            viewModel.requestReview(activity = ACTIVITY)

            assertEquals(DemoSettingsMessage.ReviewUnavailable, awaitItem().message)
            assertEquals(1, review.requests)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a completed review flow says nothing at all`() = runTest {
        // Play never reveals whether a review was written, so thanking the user would be a guess.
        val viewModel = viewModel(review = FakeAppReviewManager(outcome = ReviewOutcome.Completed))

        viewModel.uiState.test {
            assertNull(awaitItem().message)
            viewModel.requestReview(activity = ACTIVITY)

            expectNoEvents()
            assertNull(viewModel.uiState.value.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `replaying onboarding clears the completion flag`() = runTest {
        val preferences = FakeAppPreferences(initialOnboardingCompleted = true)
        val viewModel = viewModel(preferences = preferences)

        viewModel.resetOnboarding()

        assertFalse(preferences.onboardingCompleted.value)
    }

    @Test
    fun `an unhandled intent is reported and can be dismissed`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitItem()

            viewModel.onActionUnhandled()
            assertEquals(DemoSettingsMessage.NothingToHandleAction, awaitItem().message)

            viewModel.dismissMessage()
            assertNull(awaitItem().message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the privacy options form is opened through core ads`() = runTest {
        val ads = FakeAdsController(privacyOptionsRequired = true)
        val viewModel = viewModel(ads = ads)

        viewModel.showPrivacyOptions(ACTIVITY)

        assertEquals(1, ads.privacyOptionsShown)
    }

    private fun viewModel(
        preferences: FakeAppPreferences = FakeAppPreferences(),
        billing: FakeBillingManager = FakeBillingManager(),
        ads: FakeAdsController = FakeAdsController(),
        review: FakeAppReviewManager = FakeAppReviewManager(),
    ) = DemoSettingsViewModel(
        appPreferences = preferences,
        billingManager = billing,
        adsController = ads,
        appReviewManager = review,
    )

    private companion object {
        /**
         * The fakes never touch it. Play's review flow and UMP's form both need a real Activity,
         * which is exactly why those two calls are the thinnest lines in the ViewModel.
         */
        val ACTIVITY = android.app.Activity()
    }
}
