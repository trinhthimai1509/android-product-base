package dev.sautao.productbase.core.telemetry

/**
 * Product-facing analytics contract.
 *
 * Kept to the two calls every app actually makes. It is deliberately not a taxonomy of event
 * types: products name their own events, and a provider that is not Firebase must be
 * implementable without understanding any Firebase concept.
 *
 * Never pass personal or financial data, tokens, or anything else a user would not expect to
 * leave the device.
 */
interface Analytics {
    /** @param screenName a stable, non-localised identifier such as `settings`. */
    fun logScreenView(screenName: String)

    fun logEvent(event: AnalyticsEvent)
}

/**
 * @param name a stable, non-localised event name.
 * @param params values are mapped by the implementation; strings, whole numbers, decimals and
 * booleans are supported, and anything else is recorded as its string form.
 */
data class AnalyticsEvent(val name: String, val params: Map<String, Any> = emptyMap())

/** Discards everything. Used when a product ships no analytics provider. */
class NoOpAnalytics : Analytics {
    override fun logScreenView(screenName: String) = Unit

    override fun logEvent(event: AnalyticsEvent) = Unit
}
