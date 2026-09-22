package dev.sautao.productbase.core.network

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Failures this module can produce.
 *
 * Owned here rather than in a shared hierarchy: a product that makes no HTTP calls should never
 * see these types, and adding a capability should never mean editing a type in `core:common`.
 *
 * Mapping to user-facing text is the caller's job, and so is deciding whether a failure is worth
 * reporting — this module does not depend on telemetry.
 */
sealed interface NetworkError {
    /** No usable connection, or the host could not be resolved. */
    data object NoConnection : NetworkError

    /** The connection was established but the exchange did not finish in time. */
    data object Timeout : NetworkError

    /** The server answered with a non-2xx status. */
    data class Http(val code: Int) : NetworkError

    /** The response arrived but could not be parsed into the expected shape. */
    data object Malformed : NetworkError

    /** Anything else. Worth reporting through a CrashReporter if a caller has one. */
    data class Unexpected(val cause: Throwable) : NetworkError
}

/**
 * Maps a thrown exception to a [NetworkError].
 *
 * [SocketTimeoutException] is checked before [IOException] because it is one, and
 * [UnknownHostException] likewise: order matters here and the tests pin it.
 */
fun Throwable.toNetworkError(): NetworkError = when (this) {
    is HttpException -> NetworkError.Http(code())
    is SocketTimeoutException -> NetworkError.Timeout
    is UnknownHostException -> NetworkError.NoConnection
    is SerializationException -> NetworkError.Malformed
    is IOException -> NetworkError.NoConnection
    else -> NetworkError.Unexpected(this)
}
