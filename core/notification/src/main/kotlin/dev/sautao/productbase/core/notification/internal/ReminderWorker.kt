package dev.sautao.productbase.core.notification.internal

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sautao.productbase.core.notification.canPostNotifications

/**
 * Posts one reminder notification.
 *
 * Deliberately a plain [CoroutineWorker] with no injected dependencies: it needs a context and
 * its input data and nothing else. That keeps `hilt-work`, a custom `WorkerFactory` and the
 * manifest surgery to disable WorkManager's default initialiser out of every product built on
 * this base.
 */
internal class ReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    // canPostNotifications() below is exactly the check lint asks for; it cannot follow the call
    // through NotificationManagerCompat, so the guard is stated here instead.
    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val content = inputData.toReminderContent() ?: return Result.failure()

        // The user may have revoked the permission, or turned notifications off, between
        // scheduling and firing. Nothing to retry: succeed quietly rather than re-running.
        if (!applicationContext.canPostNotifications()) return Result.success()

        val notification = NotificationCompat.Builder(applicationContext, content.channelId)
            .setSmallIcon(content.smallIconResId)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.text))
            .setAutoCancel(true)
            .apply { content.deepLink?.let { setContentIntent(pendingIntentFor(it, content.id)) } }
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(content.id.hashCode(), notification)

        return Result.success()
    }

    private fun pendingIntentFor(deepLink: String, reminderId: String): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, deepLink.toUri()).apply {
            // Keeps the intent inside this app: an implicit VIEW intent could otherwise be
            // picked up by another installed app.
            setPackage(applicationContext.packageName)
        }
        return PendingIntent.getActivity(
            applicationContext,
            reminderId.hashCode(),
            intent,
            // IMMUTABLE is required from API 31 and correct everywhere: nothing outside this app
            // has any business rewriting the intent. UPDATE_CURRENT keeps a re-scheduled
            // reminder's deep link current instead of reusing a stale one.
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
