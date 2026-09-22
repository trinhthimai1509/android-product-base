package dev.sautao.productbase.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.sautao.productbase.core.common.ThemeMode
import dev.sautao.productbase.core.common.log.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * Preferences DataStore implementation.
 *
 * Read failures are recovered, not swallowed: an unreadable store falls back to defaults and is
 * logged, so the app keeps working and the failure is still visible.
 */
internal class DataStoreAppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val logger: Logger,
) : AppPreferences {

    private val preferences: Flow<Preferences> = dataStore.data.catch { throwable ->
        if (throwable is IOException) {
            logger.e(TAG, "Unable to read preferences, falling back to defaults", throwable)
            emit(emptyPreferences())
        } else {
            throw throwable
        }
    }

    override val themeMode: Flow<ThemeMode> = preferences
        .map { it.readThemeMode() }
        .distinctUntilChanged()

    override val dynamicColorEnabled: Flow<Boolean> = preferences
        .map { it[Keys.DYNAMIC_COLOR] ?: DEFAULT_DYNAMIC_COLOR }
        .distinctUntilChanged()

    override val onboardingCompleted: Flow<Boolean> = preferences
        .map { it[Keys.ONBOARDING_COMPLETED] ?: false }
        .distinctUntilChanged()

    override val notificationsEnabled: Flow<Boolean> = preferences
        .map { it[Keys.NOTIFICATIONS_ENABLED] ?: DEFAULT_NOTIFICATIONS_ENABLED }
        .distinctUntilChanged()

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    private fun Preferences.readThemeMode(): ThemeMode {
        val stored = this[Keys.THEME_MODE] ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(stored) }.getOrElse {
            // Reachable after a downgrade that removes an enum entry.
            logger.w(TAG, "Unknown stored theme mode, using default")
            ThemeMode.SYSTEM
        }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    private companion object {
        const val TAG = "AppPreferences"
        const val DEFAULT_DYNAMIC_COLOR = true
        const val DEFAULT_NOTIFICATIONS_ENABLED = true
    }
}
