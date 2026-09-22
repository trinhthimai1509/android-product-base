package dev.sautao.productbase.core.notification.internal

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.sautao.productbase.core.notification.ReminderRequest
import dev.sautao.productbase.core.notification.ReminderScheduler
import java.time.Instant
import javax.inject.Inject

/** Prefix keeps reminder work from colliding with a product's own unique work names. */
private const val WORK_NAME_PREFIX = "productbase-reminder-"

internal fun uniqueWorkName(reminderId: String): String = WORK_NAME_PREFIX + reminderId

internal class WorkManagerReminderScheduler @Inject constructor(private val workManager: WorkManager) :
    ReminderScheduler {
    override fun schedule(request: ReminderRequest) {
        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(initialDelay(request.triggerAt, Instant.now()))
            .setInputData(request.toWorkerInput())
            .build()

        // REPLACE, not KEEP: re-scheduling the same id means the reminder changed, and the new
        // time and text are the ones that should fire. KEEP would silently ignore the edit.
        workManager.enqueueUniqueWork(uniqueWorkName(request.id), ExistingWorkPolicy.REPLACE, work)
    }

    override fun cancel(id: String) {
        workManager.cancelUniqueWork(uniqueWorkName(id))
    }
}
