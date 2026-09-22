package dev.sautao.productbase.core.notification

import androidx.annotation.DrawableRes
import java.time.Instant

/**
 * A single local notification to post at a future time.
 *
 * The product owns everything meaningful here: what a reminder *means*, when it should fire and
 * what it says. This module only carries it to the notification shade.
 *
 * @param id stable and product-chosen. Scheduling twice with the same id replaces the first,
 * which is what makes "the user edited the reminder" a one-line operation.
 * @param title already localised.
 * @param text already localised.
 * @param deepLink an app URI the product has an intent-filter for; null shows a notification
 * that opens nothing.
 */
data class ReminderRequest(
    val id: String,
    val triggerAt: Instant,
    val channelId: String,
    val title: String,
    val text: String,
    @param:DrawableRes val smallIconResId: Int,
    val deepLink: String? = null,
)

/**
 * Schedules local reminders.
 *
 * Timing is **approximate**. Work is deferrable, so the system may delay it — by minutes under
 * Doze, and longer for a device that has been idle a long time. That is the right trade for a
 * daily reminder and the wrong one for an alarm clock; a product that genuinely needs
 * to-the-second delivery needs AlarmManager and the policy conversation that comes with it.
 *
 * Scheduled work survives process death and reboot, so nothing needs re-registering at start-up.
 */
interface ReminderScheduler {
    /** Schedules [request], replacing any reminder already scheduled with the same id. */
    fun schedule(request: ReminderRequest)

    /** Cancels the reminder with this id. Cancelling an unknown or already-fired id is a no-op. */
    fun cancel(id: String)
}
