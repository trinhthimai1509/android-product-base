package dev.sautao.productbase.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class InstantConvertersTest {
    private val converters = InstantConverters()

    @Test
    fun `round trips an instant`() {
        val instant = Instant.ofEpochMilli(1_755_000_000_000)

        val stored = converters.instantToEpochMilli(instant)
        assertEquals(instant, converters.epochMilliToInstant(stored))
    }

    @Test
    fun `preserves null in both directions`() {
        assertNull(converters.instantToEpochMilli(null))
        assertNull(converters.epochMilliToInstant(null))
    }

    @Test
    fun `truncates sub-millisecond precision, which is the documented storage contract`() {
        val withNanos = Instant.ofEpochSecond(1_755_000_000, 123_456_789)

        val restored = converters.epochMilliToInstant(converters.instantToEpochMilli(withNanos))

        assertEquals(Instant.ofEpochMilli(1_755_000_000_123), restored)
    }
}
