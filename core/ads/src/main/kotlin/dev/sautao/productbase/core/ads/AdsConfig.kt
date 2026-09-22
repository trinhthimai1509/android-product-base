package dev.sautao.productbase.core.ads

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Google's official test ad units.
 *
 * These are public, documented values, not secrets. They exist so a debug build never requests a
 * real ad — which is both an AdMob policy requirement and the only way to develop without
 * poisoning a real account's metrics.
 */
object TestAdUnits {
    const val APPLICATION_ID = "ca-app-pub-3940256099942544~3347511713"
    const val BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"

    internal val ALL = setOf(APPLICATION_ID, BANNER, INTERSTITIAL, REWARDED)
}

/**
 * How often an interstitial may be shown.
 *
 * Both conditions must hold, and the defaults are deliberately quiet. An interstitial that
 * appears too often costs more in uninstalls and one-star reviews than it earns.
 *
 * This is the whole of the ad policy. There is no remote configuration, no per-user tuning and
 * no session model — a product that needs different numbers passes different numbers.
 */
data class InterstitialPolicy(val minInterval: Duration = 3.minutes, val minQualifyingActions: Int = 3)

/**
 * The product's advertising configuration.
 *
 * Ad unit ids are product-specific and are never defaulted to anything real: the base ships only
 * [TestAdUnits], and production values are supplied by the product from its build configuration.
 *
 * @param adsEnabled a product-level switch, independent of premium and consent. Turning it off
 * disables advertising entirely — useful for a free variant, a review build, or an app that
 * simply does not advertise.
 */
data class AdsConfig(
    val bannerAdUnitId: String,
    val interstitialAdUnitId: String,
    val rewardedAdUnitId: String,
    val adsEnabled: Boolean = true,
    val interstitialPolicy: InterstitialPolicy = InterstitialPolicy(),
) {
    internal val usesTestAdUnits: Boolean
        get() = listOf(bannerAdUnitId, interstitialAdUnitId, rewardedAdUnitId)
            .any { it in TestAdUnits.ALL }

    companion object {
        /** Every unit set to Google's test ids. For debug builds and the demo shell. */
        fun testAds(
            adsEnabled: Boolean = true,
            interstitialPolicy: InterstitialPolicy = InterstitialPolicy(),
        ): AdsConfig = AdsConfig(
            bannerAdUnitId = TestAdUnits.BANNER,
            interstitialAdUnitId = TestAdUnits.INTERSTITIAL,
            rewardedAdUnitId = TestAdUnits.REWARDED,
            adsEnabled = adsEnabled,
            interstitialPolicy = interstitialPolicy,
        )
    }
}
