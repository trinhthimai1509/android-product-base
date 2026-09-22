package dev.sautao.productbase.demo.ui

import app.cash.turbine.test
import dev.sautao.productbase.core.common.ThemeMode
import dev.sautao.productbase.core.testing.FakeAppPreferences
import dev.sautao.productbase.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `state is loading while nothing is subscribed`() {
        val viewModel = AppViewModel(FakeAppPreferences())

        // WhileSubscribed: with no collector the upstream never starts, so the initial value stands.
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `stored preferences are exposed once collected`() = runTest {
        val viewModel =
            AppViewModel(
                FakeAppPreferences(
                    initialThemeMode = ThemeMode.DARK,
                    initialDynamicColorEnabled = false,
                ),
            )

        viewModel.uiState.test {
            val loaded = awaitItem()

            assertFalse(loaded.isLoading)
            assertEquals(ThemeMode.DARK, loaded.themeMode)
            assertFalse(loaded.dynamicColorEnabled)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `theme mode change is reflected in state`() = runTest {
        val viewModel = AppViewModel(FakeAppPreferences(initialThemeMode = ThemeMode.SYSTEM))

        viewModel.uiState.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)

            viewModel.setThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, awaitItem().themeMode)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onboarding completion decides where the app starts`() = runTest {
        val viewModel = AppViewModel(FakeAppPreferences(initialOnboardingCompleted = true))

        viewModel.uiState.test {
            assertTrue(awaitItem().onboardingCompleted)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a fresh install starts with onboarding pending`() = runTest {
        val viewModel = AppViewModel(FakeAppPreferences())

        viewModel.uiState.test {
            assertFalse(awaitItem().onboardingCompleted)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dynamic colour change is reflected in state`() = runTest {
        val viewModel = AppViewModel(FakeAppPreferences(initialDynamicColorEnabled = true))

        viewModel.uiState.test {
            assertTrue(awaitItem().dynamicColorEnabled)

            viewModel.setDynamicColorEnabled(false)
            assertFalse(awaitItem().dynamicColorEnabled)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
