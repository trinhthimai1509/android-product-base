package dev.sautao.productbase.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

/**
 * A channel a product wants to exist.
 *
 * @param id stable and never reused for a different purpose: a channel's importance and sound
 * are owned by the user once created, and re-creating an existing id changes nothing.
 * @param name user-visible, already localised by the product.
 */
data class NotificationChannelSpec(
    val id: String,
    val name: String,
    val description: String? = null,
    val importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
)

/**
 * Creates any channel that does not exist yet.
 *
 * Safe to call on every start, which is the intended usage — channels must exist before the
 * first notification is posted, and the platform ignores repeat creations.
 *
 * Channels have existed since API 26 and minSdk is 26, so there is no version branch here.
 */
fun Context.ensureNotificationChannels(channels: List<NotificationChannelSpec>) {
    val manager = getSystemService<NotificationManager>() ?: return
    channels.forEach { spec ->
        manager.createNotificationChannel(
            NotificationChannel(spec.id, spec.name, spec.importance).apply {
                description = spec.description
            },
        )
    }
}
