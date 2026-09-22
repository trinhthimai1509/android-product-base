package dev.sautao.productbase.core.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sautao.productbase.core.common.log.NoOpLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

/**
 * NetworkMonitor talks to the platform, so it is tested on the platform.
 *
 * These assert the lifecycle contract — emits promptly, releases its callback — rather than a
 * particular connectivity state, which a test cannot control.
 *
 * `runBlocking`, not `runTest`, on purpose: `runTest`'s virtual clock defers the cleanup
 * coroutine that these tests exist to check, which made the callback-release test fail against
 * correct code. Real platform callbacks need real dispatchers.
 */
@RunWith(AndroidJUnit4::class)
class ConnectivityManagerNetworkMonitorTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val monitor = ConnectivityManagerNetworkMonitor(context, NoOpLogger())

    @Test
    fun emitsWithoutWaitingForAConnectivityChange() = runBlocking {
        // The platform delivers no callback when nothing changes — and none at all when there is
        // no network — so a monitor that only forwarded callbacks would hang here forever.
        withTimeout(5.seconds) { monitor.isOnline.first() }
        Unit
    }

    @Test
    fun releasesItsCallbackWhenCollectionEnds() = runBlocking {
        // A process may hold only a limited number of network callbacks (100 on current
        // platforms) before registerNetworkCallback throws TooManyRequestsException. Collecting
        // far more times than that passes only if every collection unregistered on the way out.
        repeat(150) {
            withTimeout(5.seconds) { monitor.isOnline.first() }
        }
    }

    @Test
    fun supportsSeveralConcurrentCollectors() = runBlocking {
        // The flow is cold: each collector registers its own callback, and one collector ending
        // must not stop another.
        val first = launch { withTimeout(5.seconds) { monitor.isOnline.first() } }
        val second = launch { withTimeout(5.seconds) { monitor.isOnline.first() } }
        first.join()
        second.join()
    }
}
