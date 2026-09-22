package dev.sautao.productbase.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dev.sautao.productbase.core.common.log.Logger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether the device currently has a usable internet connection.
 *
 * "Usable" means at least one network that reports both `INTERNET` and `VALIDATED`, so a Wi-Fi
 * network sitting behind an unaccepted captive portal counts as offline — which is what a user
 * means by "I have no internet".
 *
 * This is a hint for the UI, never a precondition for making a request: the only reliable way to
 * know a request will succeed is to make it. Treat `false` as "explain why the screen is empty",
 * not as "refuse to try".
 */
interface NetworkMonitor {
    /**
     * Cold flow. Each collector registers its own platform callback and unregisters when the
     * collection ends, so collection must be tied to a lifecycle-aware scope.
     *
     * Emits the current state immediately, then on every change, with duplicates removed.
     */
    val isOnline: Flow<Boolean>
}

private const val TAG = "NetworkMonitor"

internal class ConnectivityManagerNetworkMonitor(private val context: Context, private val logger: Logger) :
    NetworkMonitor {
    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        if (connectivityManager == null) {
            // No ConnectivityManager at all (heavily modified or restricted environment).
            // Assume online: a wrong "offline" banner blocks the user, a wrong "online" only
            // means the request fails and is reported normally.
            logger.w(TAG, "ConnectivityManager unavailable, assuming online")
            trySend(true)
            close()
            return@callbackFlow
        }

        // Callbacks arrive on a single ConnectivityManager thread, so this set needs no locking.
        val usableNetworks = mutableSetOf<Network>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (capabilities.isUsable()) usableNetworks += network else usableNetworks -= network
                trySend(usableNetworks.isNotEmpty())
            }

            override fun onLost(network: Network) {
                usableNetworks -= network
                trySend(usableNetworks.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // Emitted before registering: with no network at all the platform delivers no callback,
        // and a flow that never emits would leave a screen stuck on its initial state.
        trySend(connectivityManager.isCurrentlyOnline())

        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (securityException: SecurityException) {
            // Some restricted profiles and OEM builds refuse the callback despite the manifest
            // permission. Degrade to the optimistic answer rather than crashing the collector.
            logger.w(TAG, "Connectivity callback refused, assuming online", securityException)
            trySend(true)
            close()
            return@callbackFlow
        }

        awaitClose {
            // Registrations are a limited process-wide resource: leaking them eventually throws
            // "Too many NetworkRequests" and takes down the app.
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
                .onFailure { logger.w(TAG, "Connectivity callback was already unregistered") }
        }
    }.distinctUntilChanged()

    private fun ConnectivityManager.isCurrentlyOnline(): Boolean {
        val activeNetwork = activeNetwork ?: return false
        return getNetworkCapabilities(activeNetwork)?.isUsable() == true
    }

    private fun NetworkCapabilities.isUsable(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
