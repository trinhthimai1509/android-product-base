package dev.sautao.productbase.core.ads.internal

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dev.sautao.productbase.core.ads.ConsentOutcome
import dev.sautao.productbase.core.common.log.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "AdsConsent"

/**
 * The User Messaging Platform, kept entirely inside this module.
 *
 * Consent is resolved *before* the ads SDK is initialised and before any ad is requested. No
 * user is assumed eligible for personalised advertising: UMP decides, per region and per user,
 * and [canRequestAds] is simply its answer.
 *
 * Nothing here makes a legal claim. UMP is the mechanism Google requires; whether a product's
 * overall data handling is lawful is a question for its privacy policy and its developer.
 */
@Singleton
internal class UmpConsentGateway @Inject constructor(private val context: Context, private val logger: Logger) {
    private val consentInformation: ConsentInformation
        get() = UserMessagingPlatform.getConsentInformation(context)

    /** UMP's own answer, which is false until it has been asked. */
    val canRequestAds: Boolean
        get() = runCatching { consentInformation.canRequestAds() }.getOrElse {
            logger.w(TAG, "Consent status unavailable", it)
            false
        }

    val privacyOptionsRequired: Boolean
        get() = runCatching {
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        }.getOrElse { false }

    /**
     * Refreshes consent information and shows the form if the user's region requires one.
     *
     * A failure here is not fatal: [canRequestAds] stays false, ads are not requested, and the
     * app carries on without advertising rather than without functioning.
     */
    suspend fun gatherConsent(activity: Activity): ConsentOutcome {
        val updateOutcome = requestConsentInfoUpdate(activity)
        if (updateOutcome is ConsentOutcome.Failed) return updateOutcome

        return loadAndShowFormIfRequired(activity)
    }

    suspend fun showPrivacyOptions(activity: Activity): ConsentOutcome =
        suspendCancellableCoroutine { continuation ->
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                continuation.resume(
                    if (formError == null) {
                        ConsentOutcome.Completed
                    } else {
                        logger.w(TAG, "Privacy options form failed with code ${formError.errorCode}")
                        ConsentOutcome.Failed(formError.errorCode)
                    },
                )
            }
        }

    private suspend fun requestConsentInfoUpdate(activity: Activity): ConsentOutcome =
        suspendCancellableCoroutine { continuation ->
            consentInformation.requestConsentInfoUpdate(
                activity,
                // No debug geography and no test device id: forcing a European form onto a
                // developer's device is a per-developer choice, not something a shared base
                // should decide.
                ConsentRequestParameters.Builder().build(),
                { continuation.resume(ConsentOutcome.Completed) },
                { formError ->
                    logger.w(TAG, "Consent info update failed with code ${formError.errorCode}")
                    continuation.resume(ConsentOutcome.Failed(formError.errorCode))
                },
            )
        }

    private suspend fun loadAndShowFormIfRequired(activity: Activity): ConsentOutcome =
        suspendCancellableCoroutine { continuation ->
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                continuation.resume(
                    when {
                        formError != null -> {
                            logger.w(TAG, "Consent form failed with code ${formError.errorCode}")
                            ConsentOutcome.Failed(formError.errorCode)
                        }

                        // No form was needed in this region, which is a normal outcome and not a
                        // reason to withhold ads.
                        else -> ConsentOutcome.Completed
                    },
                )
            }
        }
}
