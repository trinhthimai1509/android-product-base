package dev.sautao.productbase.core.review

import android.app.Activity

/**
 * Asks Google Play to show its in-app review dialog.
 *
 * The entire API is one call, because there is only one thing Play lets an app do. In particular
 * this module does **not** decide *when* to ask: there is no counter, no engagement score, no
 * "after the third session" rule and no remote policy. Only the product knows which moment is a
 * good one — after a task the user finished successfully, never after an error and never during
 * one — and asking at the wrong moment is how apps collect one-star reviews.
 *
 * Play itself is the final authority: it applies its own quota, it may show nothing at all, and
 * it never reports whether a review was written. [ReviewOutcome] is therefore about the *request*,
 * not about the review.
 */
interface AppReviewManager {
    /**
     * Requests the review flow and, if Play supplies one, shows it.
     *
     * Suspends until the flow finishes. Never throws: a device without Play, an exhausted quota
     * and an internal Play failure all come back as [ReviewOutcome.Unavailable].
     */
    suspend fun requestReview(activity: Activity): ReviewOutcome
}

/**
 * What happened to the request.
 *
 * Deliberately coarse. Play's error codes describe why *Play* could not help, and there is
 * nothing a product can usefully do differently for each one — so the detail is logged rather
 * than turned into API surface a caller would have to handle.
 */
sealed interface ReviewOutcome {
    /**
     * The flow completed. This does **not** mean a dialog appeared or that a review was left:
     * Play never says. A product must not reward, thank or re-prompt based on this.
     */
    data object Completed : ReviewOutcome

    /** Play could not supply a review flow: no Play Store, quota reached, or an internal error. */
    data object Unavailable : ReviewOutcome
}
