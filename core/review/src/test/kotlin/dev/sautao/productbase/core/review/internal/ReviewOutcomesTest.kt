package dev.sautao.productbase.core.review.internal

import dev.sautao.productbase.core.review.ReviewOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewOutcomesTest {
    @Test
    fun `a task that finished without an error completed`() {
        assertEquals(ReviewOutcome.Completed, reviewOutcome(null))
    }

    @Test
    fun `any failure is reported as unavailable rather than propagated`() {
        assertEquals(ReviewOutcome.Unavailable, reviewOutcome(IllegalStateException("no Play Store")))
    }

    @Test
    fun `a non-Play failure carries no Play error code`() {
        assertNull(IllegalStateException("no Play Store").reviewErrorCode())
        assertNull(null.reviewErrorCode())
    }
}
