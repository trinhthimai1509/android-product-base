package dev.sautao.productbase.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import dev.sautao.productbase.core.common.ThemeMode
import dev.sautao.productbase.core.common.log.NoOpLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreAppPreferencesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun preferences(scope: TestScope): AppPreferences = DataStoreAppPreferences(
        dataStore =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.newFolder(), "test.preferences_pb") },
        ),
        logger = NoOpLogger(),
    )

    @Test
    fun `theme mode defaults to system`() = runTest(UnconfinedTestDispatcher()) {
        val appPreferences = preferences(this)

        assertEquals(ThemeMode.SYSTEM, appPreferences.themeMode.first())
    }

    @Test
    fun `dynamic colour defaults to enabled`() = runTest(UnconfinedTestDispatcher()) {
        val appPreferences = preferences(this)

        assertTrue(appPreferences.dynamicColorEnabled.first())
    }

    @Test
    fun `theme mode emits the stored value`() = runTest(UnconfinedTestDispatcher()) {
        val appPreferences = preferences(this)

        appPreferences.themeMode.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())

            appPreferences.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem())

            appPreferences.setThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dynamic colour emits the stored value`() = runTest(UnconfinedTestDispatcher()) {
        val appPreferences = preferences(this)

        appPreferences.dynamicColorEnabled.test {
            assertTrue(awaitItem())

            appPreferences.setDynamicColorEnabled(false)
            assertEquals(false, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onboarding starts uncompleted so the first launch shows it`() = runTest(UnconfinedTestDispatcher()) {
        val appPreferences = preferences(this)

        assertFalse(appPreferences.onboardingCompleted.first())
    }

    @Test
    fun `onboarding completion is persisted`() = runTest(UnconfinedTestDispatcher()) {
        val appPreferences = preferences(this)

        appPreferences.onboardingCompleted.test {
            assertFalse(awaitItem())

            appPreferences.setOnboardingCompleted(true)
            assertTrue(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `notifications default to enabled`() = runTest(UnconfinedTestDispatcher()) {
        val appPreferences = preferences(this)

        assertTrue(appPreferences.notificationsEnabled.first())
    }

    @Test
    fun `notification preference emits the stored value`() = runTest(UnconfinedTestDispatcher()) {
        val appPreferences = preferences(this)

        appPreferences.notificationsEnabled.test {
            assertTrue(awaitItem())

            appPreferences.setNotificationsEnabled(false)
            assertFalse(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
