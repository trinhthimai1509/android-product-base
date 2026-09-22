package dev.sautao.productbase.demo

import android.app.Application
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp
import dev.sautao.productbase.core.notification.NotificationChannelSpec
import dev.sautao.productbase.core.notification.ensureNotificationChannels

@HiltAndroidApp
class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Channels must exist before the first notification is posted, and creating them again
        // is free, so start-up is the simplest correct place for this.
        ensureNotificationChannels(
            listOf(
                NotificationChannelSpec(
                    id = DemoNotifications.REMINDER_CHANNEL_ID,
                    name = getString(R.string.notification_channel_reminders),
                    description = getString(R.string.notification_channel_reminders_description),
                    importance = NotificationManager.IMPORTANCE_DEFAULT,
                ),
            ),
        )
    }
}
