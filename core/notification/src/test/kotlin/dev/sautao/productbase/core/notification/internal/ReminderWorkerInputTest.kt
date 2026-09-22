package dev.sautao.productbase.core.notification.internal

import androidx.work.Data
import dev.sautao.productbase.core.notification.ReminderRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ReminderWorkerInputTest {
    private val request = ReminderRequest(
        id = "daily-check-in",
        triggerAt = Instant.ofEpochMilli(1_755_000_000_000),
        channelId = "reminders",
        title = "Check in",
        text = "How did today go?",
        smallIconResId = 1234,
        deepLink = "productbase://demo/reminders",
    )

    @Test
    fun `survives the round trip through work input data`() {
        val content = request.toWorkerInput().toReminderContent()

        assertEquals(
            ReminderContent(
                id = "daily-check-in",
                channelId = "reminders",
                title = "Check in",
                text = "How did today go?",
                smallIconResId = 1234,
                deepLink = "productbase://demo/reminders",
            ),
            content,
        )
    }

    @Test
    fun `keeps a null deep link null rather than inventing one`() {
        val content = request.copy(deepLink = null).toWorkerInput().toReminderContent()

        assertNull(content?.deepLink)
    }

    @Test
    fun `rejects input left over from an older version of the keys`() {
        // Work is persisted, so an app update can find data written by the previous build.
        val stale = Data.Builder().putString("title", "Check in").build()

        assertNull(stale.toReminderContent())
    }

    @Test
    fun `rejects input with no icon, which would post an invisible notification`() {
        val withoutIcon = Data.Builder()
            .putString(KEY_ID, "id")
            .putString(KEY_CHANNEL_ID, "reminders")
            .putString(KEY_TITLE, "Check in")
            .putString(KEY_TEXT, "How did today go?")
            .build()

        assertNull(withoutIcon.toReminderContent())
    }

    @Test
    fun `delays until the trigger time`() {
        val now = Instant.ofEpochMilli(1_000_000)

        assertEquals(
            Duration.ofMinutes(30),
            initialDelay(triggerAt = now.plus(Duration.ofMinutes(30)), now = now),
        )
    }

    @Test
    fun `fires immediately when the trigger time has already passed`() {
        val now = Instant.ofEpochMilli(1_000_000)

        assertEquals(
            Duration.ZERO,
            initialDelay(triggerAt = now.minus(Duration.ofHours(6)), now = now),
        )
    }

    @Test
    fun `unique work names are namespaced so they cannot collide with product work`() {
        assertEquals("productbase-reminder-daily-check-in", uniqueWorkName("daily-check-in"))
    }
}
