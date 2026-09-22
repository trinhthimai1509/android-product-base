package dev.sautao.productbase.core.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * A banner that shows nothing at all when ads are unavailable.
 *
 * "Nothing at all" is deliberate: no view is created, no request is made, and no empty strip is
 * left behind. A user who paid to remove ads sees the layout they paid for.
 *
 * The [AdView] is paused with the host lifecycle and destroyed when the Composable leaves the
 * composition — the two things this kind of code usually forgets, and the reason a banner is
 * worth wrapping at all.
 */
@Composable
fun AppBannerAd(
    adsController: AdsController,
    modifier: Modifier = Modifier,
) {
    val adsAvailable by adsController.adsAvailable.collectAsStateWithLifecycle(initialValue = false)

    // Previews and screenshot tests must not reach the SDK or the network.
    if (!adsAvailable || LocalInspectionMode.current) return

    val context = LocalContext.current
    val adUnitId = adsController.bannerAdUnitId
    // The adaptive banner is sized from the container it will actually sit in, in dp.
    val containerWidthPx = LocalWindowInfo.current.containerSize.width
    val widthDp = with(LocalDensity.current) { containerWidthPx.toDp() }.value.toInt()

    val adView = remember(adUnitId, widthDp) {
        AdView(context).apply {
            setAdUnitId(adUnitId)
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            loadAd(AdRequest.Builder().build())
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                Lifecycle.Event.ON_RESUME -> adView.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adView },
    )
}
