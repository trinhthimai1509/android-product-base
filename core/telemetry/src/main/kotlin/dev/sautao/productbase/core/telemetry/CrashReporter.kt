package dev.sautao.productbase.core.telemetry

/**
 * Product-facing crash-reporting contract.
 *
 * Fatal crashes are captured by the provider automatically; this interface exists for the three
 * things application code has to do explicitly.
 *
 * Never log personal information, financial data, authentication tokens or purchase tokens:
 * breadcrumbs and custom keys are uploaded with the report.
 */
interface CrashReporter {
    /** Reports a handled failure that should still be visible in aggregate. */
    fun recordNonFatal(throwable: Throwable)

    /** Adds a breadcrumb to the next report. */
    fun log(message: String)

    /** Attaches a value to every subsequent report — build flavour, feature flag state, etc. */
    fun setCustomKey(key: String, value: String)
}

/** Discards everything. Used when a product ships no crash-reporting provider. */
class NoOpCrashReporter : CrashReporter {
    override fun recordNonFatal(throwable: Throwable) = Unit

    override fun log(message: String) = Unit

    override fun setCustomKey(key: String, value: String) = Unit
}
