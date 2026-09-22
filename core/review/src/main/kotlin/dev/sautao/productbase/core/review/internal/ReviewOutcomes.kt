package dev.sautao.productbase.core.review.internal

import com.google.android.play.core.review.ReviewException
import dev.sautao.productbase.core.review.ReviewOutcome

/**
 * How a finished Play task becomes an outcome.
 *
 * The only distinction that survives is "the flow ran" versus "Play could not help", because
 * that is the only distinction a product can act on. Everything Play says about *why* is logged.
 */
internal fun reviewOutcome(error: Throwable?): ReviewOutcome =
    if (error == null) ReviewOutcome.Completed else ReviewOutcome.Unavailable

/** Play's own code when it has one; null for anything else, including no Play Store at all. */
internal fun Throwable?.reviewErrorCode(): Int? = (this as? ReviewException)?.errorCode
