package dev.sautao.productbase.core.common.log

import android.util.Log

/** Writes to logcat. Intended for debug builds. */
class AndroidLogger : Logger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Log.w(tag, message, throwable)
    }

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Log.e(tag, message, throwable)
    }
}
