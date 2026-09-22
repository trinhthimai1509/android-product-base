package dev.sautao.productbase.core.ads.internal

import dev.sautao.productbase.core.ads.AdsConfig
import dev.sautao.productbase.core.ads.TestAdUnits
import dev.sautao.productbase.core.common.PremiumStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsAvailabilityTest {
    @Test
    fun `a consenting free user with ads enabled may see ads`() {
        assertTrue(
            adsAvailable(
                adsEnabled = true,
                misconfiguredForRelease = false,
                consentAllowsRequests = true,
                premiumStatus = PremiumStatus.FREE,
            ),
        )
    }

    @Test
    fun `premium suppresses ads`() {
        assertFalse(
            adsAvailable(
                adsEnabled = true,
                misconfiguredForRelease = false,
                consentAllowsRequests = true,
                premiumStatus = PremiumStatus.PREMIUM,
            ),
        )
    }

    @Test
    fun `unknown ownership suppresses ads until Play answers`() {
        // The cold-start case: billing is still querying. A previously paying user must not see
        // an ad in the meantime.
        assertFalse(
            adsAvailable(
                adsEnabled = true,
                misconfiguredForRelease = false,
                consentAllowsRequests = true,
                premiumStatus = PremiumStatus.UNKNOWN,
            ),
        )
    }

    @Test
    fun `no consent means no ads, whatever else is true`() {
        // UMP decides. A user who has not been asked, or who declined, is not eligible.
        assertFalse(
            adsAvailable(
                adsEnabled = true,
                misconfiguredForRelease = false,
                consentAllowsRequests = false,
                premiumStatus = PremiumStatus.FREE,
            ),
        )
    }

    @Test
    fun `the product switch turns advertising off entirely`() {
        assertFalse(
            adsAvailable(
                adsEnabled = false,
                misconfiguredForRelease = false,
                consentAllowsRequests = true,
                premiumStatus = PremiumStatus.FREE,
            ),
        )
    }

    @Test
    fun `a release build configured with test units serves nothing`() {
        assertFalse(
            adsAvailable(
                adsEnabled = true,
                misconfiguredForRelease = true,
                consentAllowsRequests = true,
                premiumStatus = PremiumStatus.FREE,
            ),
        )
    }

    @Test
    fun `test ad units are recognised wherever they appear`() {
        assertTrue(AdsConfig.testAds().usesTestAdUnits)
        assertTrue(
            AdsConfig(
                bannerAdUnitId = "ca-app-pub-real/1",
                interstitialAdUnitId = TestAdUnits.INTERSTITIAL,
                rewardedAdUnitId = "ca-app-pub-real/3",
            ).usesTestAdUnits,
        )
        assertFalse(
            AdsConfig(
                bannerAdUnitId = "ca-app-pub-real/1",
                interstitialAdUnitId = "ca-app-pub-real/2",
                rewardedAdUnitId = "ca-app-pub-real/3",
            ).usesTestAdUnits,
        )
    }
}
