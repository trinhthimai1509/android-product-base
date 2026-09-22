package dev.sautao.productbase.core.review.internal

import android.app.Activity
import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sautao.productbase.core.common.log.Logger
import dev.sautao.productbase.core.review.AppReviewManager
import dev.sautao.productbase.core.review.ReviewOutcome
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "Review"

/**
 * Play In-App Review behind [AppReviewManager].
 *
 * Every Play Core type stays in this file. Nothing here throws: a review prompt is a courtesy,
 * and a courtesy that can crash an app is worse than no prompt at all.
 */
@Singleton
internal class PlayReviewManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger,
) : AppReviewManager {
    // Created lazily: constructing the manager touches Play services, and a product that never
    // asks for a review should never pay for it.
    private val reviewManager by lazy { ReviewManagerFactory.create(context) }

    override suspend fun requestReview(activity: Activity): ReviewOutcome {
        val request = runCatching { reviewManager.requestReviewFlow().awaitCompletion() }
            .getOrElse { throwable ->
                // Reached on a device with no Play Store, where the factory itself gives up.
                logger.w(TAG, "Review flow could not be requested", throwable)
                return ReviewOutcome.Unavailable
            }

        val reviewInfo: ReviewInfo = request.result(logger, "requested") ?: return ReviewOutcome.Unavailable

        val launch = runCatching { reviewManager.launchReviewFlow(activity, reviewInfo).awaitCompletion() }
            .getOrElse { throwable ->
                logger.w(TAG, "Review flow could not be shown", throwable)
                return ReviewOutcome.Unavailable
            }

        // Play reports completion whether or not the dialog appeared and whether or not the user
        // wrote anything. There is no more information to be had.
        return reviewOutcome(launch.exception)
    }
}

private fun <T> Task<T>.result(logger: Logger, stage: String): T? {
    val error = exception
    if (error != null) {
        val code = error.reviewErrorCode()
        logger.w(TAG, "Play declined the review flow ($stage), code ${code ?: "none"}")
        return null
    }
    return result
}

/**
 * Waits for the task without ever throwing: the completed task is the value, so failure is
 * inspected rather than propagated.
 */
private suspend fun <T> Task<T>.awaitCompletion(): Task<T> = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { continuation.resume(it) }
}
