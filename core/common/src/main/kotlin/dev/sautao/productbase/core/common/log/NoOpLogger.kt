package dev.sautao.productbase.core.common.log

/**
 * Discards everything. Intended for release builds.
 *
 * Errors that matter should be reported through `CrashReporter` (core:telemetry, Phase 3),
 * not left to logging.
 */
class NoOpLogger : Logger {
    override fun d(tag: String, message: String) = Unit

    override fun i(tag: String, message: String) = Unit

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = Unit

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = Unit
}
