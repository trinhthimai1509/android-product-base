package dev.sautao.productbase.core.common.log

/**
 * Debug logging that can be switched off in release builds.
 *
 * The product decides which implementation is bound — typically [AndroidLogger] in debug and
 * [NoOpLogger] in release — so no module has to guard its own log calls with a build check.
 *
 * Never log personal information, financial data, authentication tokens, purchase tokens or
 * any other secret. Release logging is minimised, not private: logcat is readable on device.
 */
interface Logger {
    fun d(tag: String, message: String)

    fun i(tag: String, message: String)

    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}
