package dev.sautao.productbase.demo.ui.infra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sautao.productbase.core.datastore.AppPreferences
import dev.sautao.productbase.core.notification.ReminderRequest
import dev.sautao.productbase.core.notification.ReminderScheduler
import dev.sautao.productbase.core.telemetry.Analytics
import dev.sautao.productbase.core.telemetry.AnalyticsEvent
import dev.sautao.productbase.core.telemetry.CrashReporter
import dev.sautao.productbase.demo.DemoNotifications
import dev.sautao.productbase.demo.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class InfraUiState(
    val telemetrySent: Boolean = false,
    val reminderScheduled: Boolean = false,
    /** The user turned reminders off in settings, so nothing was scheduled. */
    val remindersTurnedOff: Boolean = false,
)

/**
 * Exercises telemetry and reminders.
 *
 * Nothing here imports Firebase, WorkManager or NotificationCompat — the point of the demo is
 * that product code talks to `Analytics`, `CrashReporter` and `ReminderScheduler` and to nothing
 * underneath them.
 */
@HiltViewModel
class InfraViewModel @Inject constructor(
    private val analytics: Analytics,
    private val crashReporter: CrashReporter,
    private val reminderScheduler: ReminderScheduler,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InfraUiState())
    val uiState: StateFlow<InfraUiState> = _uiState.asStateFlow()

    fun sendTelemetry() {
        analytics.logScreenView(SCREEN_NAME)
        analytics.logEvent(
            AnalyticsEvent(
                name = "demo_action",
                params = mapOf("source" to SCREEN_NAME, "attempt" to 1L),
            ),
        )
        crashReporter.setCustomKey("last_demo_action", "telemetry")
        crashReporter.log("Demo telemetry action tapped")
        crashReporter.recordNonFatal(IllegalStateException("Demo non-fatal, not a real failure"))

        _uiState.value = _uiState.value.copy(telemetrySent = true)
    }

    /**
     * Respects the app's own notification switch.
     *
     * This is what makes the settings row real rather than decorative: the preference is stored
     * by the base, and the product is what gives it meaning. The system permission is a separate
     * gate the screen checks, and both have to allow it.
     */
    fun scheduleReminder(title: String, text: String) {
        viewModelScope.launch {
            if (!appPreferences.notificationsEnabled.first()) {
                _uiState.value = _uiState.value.copy(reminderScheduled = false, remindersTurnedOff = true)
                return@launch
            }

            reminderScheduler.schedule(
                ReminderRequest(
                    id = REMINDER_ID,
                    triggerAt = Instant.now().plus(REMINDER_DELAY_SECONDS, ChronoUnit.SECONDS),
                    channelId = DemoNotifications.REMINDER_CHANNEL_ID,
                    title = title,
                    text = text,
                    smallIconResId = R.drawable.ic_notification,
                    deepLink = DemoNotifications.REMINDER_DEEP_LINK,
                ),
            )
            _uiState.value = _uiState.value.copy(reminderScheduled = true, remindersTurnedOff = false)
        }
    }

    fun cancelReminder() {
        reminderScheduler.cancel(REMINDER_ID)
        _uiState.value = _uiState.value.copy(reminderScheduled = false)
    }

    private companion object {
        const val SCREEN_NAME = "infrastructure"
        const val REMINDER_ID = "demo-reminder"
        const val REMINDER_DELAY_SECONDS = 15L
    }
}
