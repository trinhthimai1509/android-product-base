package dev.sautao.productbase.core.testing

import dev.sautao.productbase.core.common.ThemeMode
import dev.sautao.productbase.core.datastore.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory [AppPreferences], the second implementation that justifies the interface. */
class FakeAppPreferences(
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
    initialDynamicColorEnabled: Boolean = true,
    initialOnboardingCompleted: Boolean = false,
    initialNotificationsEnabled: Boolean = true,
) : AppPreferences {
    private val _themeMode = MutableStateFlow(initialThemeMode)
    private val _dynamicColorEnabled = MutableStateFlow(initialDynamicColorEnabled)
    private val _onboardingCompleted = MutableStateFlow(initialOnboardingCompleted)
    private val _notificationsEnabled = MutableStateFlow(initialNotificationsEnabled)

    override val themeMode: StateFlow<ThemeMode> = _themeMode
    override val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled
    override val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted
    override val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        _themeMode.value = themeMode
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        _dynamicColorEnabled.value = enabled
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _onboardingCompleted.value = completed
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }
}
