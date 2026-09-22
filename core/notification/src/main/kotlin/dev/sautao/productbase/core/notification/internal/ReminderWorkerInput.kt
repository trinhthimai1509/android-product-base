package dev.sautao.productbase.core.notification.internal

import androidx.work.Data
import dev.sautao.productbase.core.notification.ReminderRequest
import java.time.Duration
import java.time.Instant

internal const val KEY_ID = "reminder_id"
internal const val KEY_CHANNEL_ID = "channel_id"
internal const val KEY_TITLE = "title"
internal const val KEY_TEXT = "text"
internal const val KEY_SMALL_ICON = "small_icon"
internal const val KEY_DEEP_LINK = "deep_link"

/** What the worker needs in order to post the notification, once the delay has elapsed. */
internal data class ReminderContent(
    val id: String,
    val channelId: String,
    val title: String,
    val text: String,
    val smallIconResId: Int,
    val deepLink: String?,
)

internal fun ReminderRequest.toWorkerInput(): Data = Data.Builder()
    .putString(KEY_ID, id)
    .putString(KEY_CHANNEL_ID, channelId)
    .putString(KEY_TITLE, title)
    .putString(KEY_TEXT, text)
    .putInt(KEY_SMALL_ICON, smallIconResId)
    .putString(KEY_DEEP_LINK, deepLink)
    .build()

/**
 * Returns null when the stored input is unusable — which happens if a scheduled reminder
 * survives an app update that changed these keys. The worker fails instead of posting a
 * notification with blank content.
 */
internal fun Data.toReminderContent(): ReminderContent? {
    val id = getString(KEY_ID) ?: return null
    val channelId = getString(KEY_CHANNEL_ID) ?: return null
    val title = getString(KEY_TITLE) ?: return null
    val text = getString(KEY_TEXT) ?: return null
    val smallIcon = getInt(KEY_SMALL_ICON, 0)
    if (smallIcon == 0) return null
    return ReminderContent(
        id = id,
        channelId = channelId,
        title = title,
        text = text,
        smallIconResId = smallIcon,
        deepLink = getString(KEY_DEEP_LINK),
    )
}

/**
 * Delay between now and [triggerAt], never negative.
 *
 * A reminder whose time has already passed — a device that was off, a user editing an old entry
 * — fires immediately rather than being silently dropped or scheduled into the past.
 */
internal fun initialDelay(triggerAt: Instant, now: Instant): Duration {
    val delay = Duration.between(now, triggerAt)
    return if (delay.isNegative) Duration.ZERO else delay
}
