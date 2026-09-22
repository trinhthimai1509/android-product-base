package dev.sautao.productbase.feature.onboarding

import app.cash.turbine.test
import dev.sautao.productbase.core.testing.FakeAppPreferences
import dev.sautao.productbase.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val PAGE_COUNT = 3

class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `a fresh install has not completed onboarding`() = runTest {
        val preferences = FakeAppPreferences()
        val viewModel = OnboardingViewModel(preferences)

        viewModel.uiState.test {
            val initial = awaitItem()

            assertEquals(0, initial.pageIndex)
            assertFalse(initial.isCompleted)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a returning user has completed onboarding`() = runTest {
        val viewModel = OnboardingViewModel(FakeAppPreferences(initialOnboardingCompleted = true))

        viewModel.uiState.test {
            assertTrue(awaitItem().isCompleted)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `next advances one page at a time`() = runTest {
        val viewModel = OnboardingViewModel(FakeAppPreferences())

        viewModel.uiState.test {
            assertEquals(0, awaitItem().pageIndex)

            viewModel.next(PAGE_COUNT)
            assertEquals(1, awaitItem().pageIndex)

            viewModel.next(PAGE_COUNT)
            assertEquals(2, awaitItem().pageIndex)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `next on the last page finishes onboarding`() = runTest {
        val preferences = FakeAppPreferences()
        val viewModel = OnboardingViewModel(preferences)

        viewModel.uiState.test {
            awaitItem()
            viewModel.goToPage(PAGE_COUNT - 1)
            assertEquals(PAGE_COUNT - 1, awaitItem().pageIndex)

            viewModel.next(PAGE_COUNT)

            // Completion comes back from storage, not from an optimistic local flag.
            assertTrue(awaitItem().isCompleted)
            assertTrue(preferences.onboardingCompleted.value)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `skipping completes onboarding and is persisted`() = runTest {
        // A skipped onboarding is a completed one: the user has said they do not want the tour,
        // and showing it again on the next launch ignores that.
        val preferences = FakeAppPreferences()
        val viewModel = OnboardingViewModel(preferences)

        viewModel.uiState.test {
            assertFalse(awaitItem().isCompleted)

            viewModel.skip()

            assertTrue(awaitItem().isCompleted)
            assertTrue(preferences.onboardingCompleted.value)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `finishing completes onboarding and is persisted`() = runTest {
        val preferences = FakeAppPreferences()
        val viewModel = OnboardingViewModel(preferences)

        viewModel.uiState.test {
            assertFalse(awaitItem().isCompleted)

            viewModel.finish()

            assertTrue(awaitItem().isCompleted)
            assertTrue(preferences.onboardingCompleted.value)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a swipe keeps the observable state in step`() = runTest {
        val viewModel = OnboardingViewModel(FakeAppPreferences())

        viewModel.uiState.test {
            awaitItem()

            viewModel.goToPage(2)
            assertEquals(2, awaitItem().pageIndex)

            // Defensive: a pager cannot report a negative page, but state that can go negative
            // would index a list.
            viewModel.goToPage(-1)
            assertEquals(0, awaitItem().pageIndex)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
