package dev.sautao.productbase.core.ads.internal

import dev.sautao.productbase.core.ads.InterstitialPolicy
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.time.toJavaDuration

/**
 * Decides whether an interstitial may be shown now.
 *
 * Pure and deterministic apart from the clock, which is `java.time.Clock` — the standard
 * injectable seam. No wrapper type was introduced for it: `Clock.fixed` is all a test needs.
 *
 * Both conditions must hold, including for the very first interstitial of a session: a user who
 * has just opened the app has not done enough for an ad to be reasonable.
 */
internal class InterstitialFrequencyGovernor(private val policy: InterstitialPolicy, private val clock: Clock) {
    private var lastShownAt: Instant? = null
    private var qualifyingActions: Int = 0

    @Synchronized
    fun recordQualifyingAction() {
        qualifyingActions++
    }

    @Synchronized
    fun canShow(): Boolean {
        if (qualifyingActions < policy.minQualifyingActions) return false
        val previous = lastShownAt ?: return true
        return Duration.between(previous, clock.instant()) >= policy.minInterval.toJavaDuration()
    }

    /**
     * Records that one was shown, which resets both conditions.
     *
     * Called only after the ad is actually on screen — counting a failed request would let a run
     * of no-fills silence advertising entirely.
     */
    @Synchronized
    fun recordShown() {
        lastShownAt = clock.instant()
        qualifyingActions = 0
    }
}
