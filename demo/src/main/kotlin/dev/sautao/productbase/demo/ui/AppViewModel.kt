package dev.sautao.productbase.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sautao.productbase.core.common.ThemeMode
import dev.sautao.productbase.core.datastore.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application-wide appearance state.
 *
 * Immutable state in, callbacks out: every screen below is stateless and can be previewed and
 * tested without Hilt.
 */
data class AppUiState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    /**
     * Decides where navigation starts. It is read before the first frame precisely so onboarding
     * does not appear for a moment and then vanish for a returning user.
     */
    val onboardingCompleted: Boolean = false,
)

@HiltViewModel
class AppViewModel
@Inject
constructor(private val appPreferences: AppPreferences) : ViewModel() {
    val uiState: StateFlow<AppUiState> =
        combine(
            appPreferences.themeMode,
            appPreferences.dynamicColorEnabled,
            appPreferences.onboardingCompleted,
        ) { themeMode, dynamicColorEnabled, onboardingCompleted ->
            AppUiState(
                isLoading = false,
                themeMode = themeMode,
                dynamicColorEnabled = dynamicColorEnabled,
                onboardingCompleted = onboardingCompleted,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppUiState(),
        )

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { appPreferences.setThemeMode(themeMode) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setDynamicColorEnabled(enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
