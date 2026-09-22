package dev.sautao.productbase.core.network

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The product's HTTP configuration. Supplied by the product through Hilt; this module never
 * decides a base URL or reads `BuildConfig` itself.
 *
 * @param enableHttpLogging bodies and headers are logged when true. Wire this to `BuildConfig.DEBUG`
 * and nothing else — request logs routinely contain tokens and personal data.
 */
data class NetworkConfig(
    val baseUrl: String,
    val connectTimeout: Duration = 15.seconds,
    val readTimeout: Duration = 20.seconds,
    val writeTimeout: Duration = 20.seconds,
    val enableHttpLogging: Boolean = false,
)
