package dev.sautao.productbase.core.ads.internal

import dev.sautao.productbase.core.ads.InterstitialPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.minutes

/**
 * A mutable clock, which is all the "time abstraction" this needed — see the decision not to
 * introduce a TimeProvider.
 */
private class TestClock(var now: Instant = Instant.ofEpochSecond(1_000_000)) : Clock() {
    override fun getZone() = ZoneOffset.UTC
    override fun withZone(zone: java.time.ZoneId?): Clock = this
    override fun instant(): Instant = now
    fun advance(minutes: Long) {
        now = now.plusSeconds(minutes * 60)
    }
}

class InterstitialFrequencyGovernorTest {
    private val clock = TestClock()
    private val policy = InterstitialPolicy(minInterval = 3.minutes, minQualifyingActions = 3)
    private val governor = InterstitialFrequencyGovernor(policy, clock)

    private fun doActions(count: Int) = repeat(count) { governor.recordQualifyingAction() }

    @Test
    fun `refuses before the user has done anything`() {
        // A user who just opened the app has not earned an interstitial.
        assertFalse(governor.canShow())
    }

    @Test
    fun `refuses until the action threshold is reached`() {
        doActions(2)
        assertFalse(governor.canShow())

        governor.recordQualifyingAction()
        assertTrue(governor.canShow())
    }

    @Test
    fun `allows the first interstitial without waiting for the interval`() {
        // There is no previous ad to be too close to.
        doActions(3)
        assertTrue(governor.canShow())
    }

    @Test
    fun `refuses immediately after showing one, however many actions follow`() {
        doActions(3)
        governor.recordShown()

        doActions(10)
        assertFalse(governor.canShow())
    }

    @Test
    fun `refuses while the interval has not elapsed`() {
        doActions(3)
        governor.recordShown()
        doActions(3)

        clock.advance(minutes = 2)
        assertFalse(governor.canShow())
    }

    @Test
    fun `allows once both the interval and the actions are satisfied`() {
        doActions(3)
        governor.recordShown()
        doActions(3)

        clock.advance(minutes = 3)
        assertTrue(governor.canShow())
    }

    @Test
    fun `time alone is not enough`() {
        doActions(3)
        governor.recordShown()

        clock.advance(minutes = 60)
        assertFalse(governor.canShow())

        doActions(3)
        assertTrue(governor.canShow())
    }

    @Test
    fun `a failed show does not consume the allowance`() {
        // recordShown is only called once an ad is genuinely on screen, so a run of no-fills
        // cannot silence advertising for the rest of the session.
        doActions(3)
        assertTrue(governor.canShow())
        assertTrue(governor.canShow())
    }
}
