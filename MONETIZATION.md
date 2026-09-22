# Monetization

Ads (`core:ads`), purchases (`core:billing`), the paywall (`feature:premium`), and the rules that
connect them.

Two things to internalise before anything else:

1. **The product decides *when*; the Base decides *whether*.** You choose the moment for an
   interstitial or a review prompt. The Base enforces consent, frequency, entitlement and policy.
2. **Google Play is the only source of ownership.** Nothing is cached, and no local flag stands in
   for a purchase.

---

## AdsConfig

```kotlin
data class AdsConfig(
    val bannerAdUnitId: String,
    val interstitialAdUnitId: String,
    val rewardedAdUnitId: String,
    val adsEnabled: Boolean = true,
    val interstitialPolicy: InterstitialPolicy = InterstitialPolicy(),
)

data class InterstitialPolicy(
    val minInterval: Duration = 3.minutes,
    val minQualifyingActions: Int = 3,
)
```

`adsEnabled` is a product switch independent of consent and premium — useful for a free variant, a
review build, or an app that simply stops advertising.

`InterstitialPolicy` is the entire ad policy. Both conditions must hold before an interstitial
shows, and the defaults are deliberately quiet: an interstitial that appears too often costs more
in uninstalls and one-star reviews than it earns. A product that wants different numbers passes
different numbers. There is no remote configuration and no per-user tuning.

Tell the governor what counts as real use — only you know:

```kotlin
adsController.recordQualifyingAction()      // a saved entry, a finished calculation
```

### Debug test ads

```kotlin
@Provides @Singleton fun provideAdsConfig(): AdsConfig = AdsConfig.testAds()
```

`AdsConfig.testAds()` fills every slot with Google's published test ids. These are public,
documented values, not secrets. A debug build must never request a real ad: it is an AdMob policy
requirement and the only way to develop without poisoning a real account's metrics.

```kotlin
object TestAdUnits {
    const val APPLICATION_ID = "ca-app-pub-3940256099942544~3347511713"
    const val BANNER         = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL   = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED       = "ca-app-pub-3940256099942544/5224354917"
}
```

### Production ids

Ad unit ids are product-specific and are never defaulted to anything real. Supply them per build
type, from outside the repository:

```kotlin
// app/build.gradle.kts
// Google's published test values, repeated here because a Gradle script cannot see Kotlin
// source: TestAdUnits is a class in core:ads, not something a build file can reference.
val testAdmobApplicationId = "ca-app-pub-3940256099942544~3347511713"
val testBannerAdUnitId = "ca-app-pub-3940256099942544/6300978111"

android {
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["admobApplicationId"] = testAdmobApplicationId
            buildConfigField("String", "AD_UNIT_BANNER", "\"$testBannerAdUnitId\"")
            // …and the same for AD_UNIT_INTERSTITIAL and AD_UNIT_REWARDED
        }
        getByName("release") {
            manifestPlaceholders["admobApplicationId"] =
                providers.gradleProperty("tracker.admob.applicationId").get()
            buildConfigField(
                "String",
                "AD_UNIT_BANNER",
                "\"${providers.gradleProperty("tracker.admob.banner").get()}\"",
            )
        }
    }
}
```

Keep the real ids out of the repository: put them in `~/.gradle/gradle.properties` or supply them
from CI with `-Ptracker.admob.applicationId=…`.

```kotlin
@Provides @Singleton
fun provideAdsConfig(): AdsConfig = if (BuildConfig.DEBUG) {
    AdsConfig.testAds()
} else {
    AdsConfig(
        bannerAdUnitId = BuildConfig.AD_UNIT_BANNER,
        interstitialAdUnitId = BuildConfig.AD_UNIT_INTERSTITIAL,
        rewardedAdUnitId = BuildConfig.AD_UNIT_REWARDED,
    )
}
```

**Two safety nets you cannot switch off.**

1. `core:ads` declares the AdMob application id as a **manifest placeholder**, so a product that
   includes the module must name one per build type or the build fails.
2. At runtime, test ad units in a **non-debuggable** build disable advertising entirely and log an
   error. Shipping test ads to real users therefore takes two deliberate mistakes rather than one
   oversight.

---

## UMP consent

Consent is resolved **before** the Mobile Ads SDK is initialised and before any ad is requested.
One call does both, from your first Activity:

```kotlin
@Inject lateinit var adsController: AdsController
// …
LaunchedEffect(Unit) { adsController.initialize(this@MainActivity) }
```

Safe to call again; safe to fail. If consent cannot be resolved, ads stay unavailable and the app
carries on without advertising rather than without functioning.

**Privacy options.** Some regions require an ongoing way to change the choice. The Base tells you
whether that applies; you decide where the entry point lives:

```kotlin
val required by adsController.privacyOptionsRequired.collectAsStateWithLifecycle()
if (required) {
    SettingsRow.Action(stringResource(R.string.privacy_options)) {
        activity?.let { viewModel.showPrivacyOptions(it) }   // suspend → ConsentOutcome
    }
}
```

`feature:settings` never sees a UMP type. It gets a boolean and a lambda.

No debug geography and no test device id are configured: forcing a European consent form onto a
developer's device is a per-developer choice, not something a shared base should decide. To test
the consent UI, use AdMob's debug settings in your own build.

Nothing here makes a legal claim. UMP is the mechanism Google requires; whether your data handling
is lawful is a question for your privacy policy and your lawyer.

---

## BillingConfig

```kotlin
data class BillingConfig(val proProductId: String)

@Provides @Singleton
fun provideBillingConfig() = BillingConfig(proProductId = "pro_lifetime")
```

One id, because the supported shape is one lifetime "remove ads / Pro" purchase. The Base ships no
product id and no catalogue. Subscriptions are **not** supported.

Including `core:billing` is what makes "premium" mean "owns the Pro product": the module provides
the `PremiumState` binding. Do not provide your own as well — that is a duplicate-binding error.

<a id="free-app"></a>
### An app that sells nothing

Omit `core:billing` entirely and bind:

```kotlin
@Provides @Singleton fun providePremiumState(): PremiumState = PremiumState.AlwaysFree
```

That is the whole reason `core:ads` does not depend on `core:billing`: an ad-supported free app
never links the Play Billing SDK.

---

## Premium integration

`feature:premium` is a paywall *mechanism*. You supply every word:

```kotlin
PremiumContent(
    title = stringResource(R.string.premium_title),
    subtitle = stringResource(R.string.premium_subtitle),
    benefits = listOf(
        stringResource(R.string.premium_benefit_ads),
        stringResource(R.string.premium_benefit_support),
    ),
    footnote = stringResource(R.string.premium_footnote),   // e.g. "one-time payment"
)
```

The **price comes from Google Play**, already in the user's currency and locale. Never format a
price yourself and never hardcode one. Until Play supplies it, `uiState.product` is null and the
purchase button is disabled — a button that cannot work is worse than one that is visibly waiting.

Navigation to the paywall is yours (`navController.navigate(PremiumRoute)`); there is no
`feature:settings` → `feature:premium` dependency.

---

## Purchase and restore semantics

```kotlin
sealed interface PurchaseState  { Unknown; NotPurchased; Pending; Purchased }
sealed interface PurchaseOutcome { Purchased; Pending; AlreadyOwned; Cancelled; Failed(error) }
```

**State answers "what does the user own"; outcome answers "what happened when they tapped Buy".**
A cancelled purchase changes the outcome and not the state.

- `Cancelled` is not an error. The user changed their mind; an error dialog for it is a bug.
- `AlreadyOwned` triggers a refresh so state catches up with what Play knows.
- An unacknowledged purchase still counts as ownership — Play took the money, and acknowledgement
  is the app's obligation, handled automatically after every purchase and every refresh.

**Restore is a refresh.** `refreshPurchases()` re-reads ownership from Play; there is nothing
stored locally to restore *from*. Both `feature:premium` and your own settings row should call it.

The message that matters most is the failure one:

| Result of restore | Say |
|---|---|
| `Purchased` | Your purchase was restored |
| `NotPurchased` | No previous purchase found |
| `Unknown` (query failed) | **"Could not reach Google Play, so your purchases could not be checked"** |

Telling a paying customer they own nothing, when the truth is that nobody could ask, is the worst
message a purchase screen can produce. `PremiumMessage.CouldNotCheck` exists for exactly that.

---

## UNKNOWN / FREE / PREMIUM behaviour

`core:ads` observes `PremiumState.status`, and ownership maps to it like this:

| `PurchaseState` | `PremiumStatus` | Ads | Rationale |
|---|---|---|---|
| `Unknown` | `UNKNOWN` | **suppressed** | Nobody has answered yet. A cold start spends its first moments here; showing an ad means a user who paid to remove ads sees one every launch. |
| `NotPurchased` | `FREE` | allowed | |
| `Pending` | `FREE` | allowed | The money has not moved. A pending purchase can sit for days and can still be declined; granting the paid experience for it gives the product away. The paywall says a payment is being processed. |
| `Purchased` | `PREMIUM` | suppressed | |

An ad is shown only when **all** of these hold: `adsEnabled`, ad units are not test units in a
non-debuggable build, UMP allows requests, and status is exactly `FREE`.

`AppBannerAd` renders **nothing at all** when ads are unavailable — no view, no request, no empty
strip. A user who paid gets the layout they paid for.

To keep `UNKNOWN` from lasting forever on a device where Play Billing does not exist at all,
`core:billing` resolves `Unknown → NotPurchased` on exactly two response codes,
`BILLING_UNAVAILABLE` and `FEATURE_NOT_SUPPORTED`. Every transient code (disconnected,
unavailable, timeout, network) leaves ownership unknown, because those are the codes a paying user
is on the other end of.

---

## Known limitations

Read these before you plan a release.

| Limitation | Detail |
|---|---|
| **Prolonged `UNKNOWN` suppresses ads indefinitely** | If Play keeps failing with a *transient* code, ownership never resolves and ads never show. Measured on an emulator without a Play account: `SERVICE_UNAVAILABLE` after ~30 s, ads suppressed for the whole session. A bounded grace period was deliberately **not** added without production evidence — this is an open production-validation item (ARCHITECTURE_PLAN.md §21). If you observe it in the field, report it as class **D** with numbers. |
| **No subscriptions** | One-time products only. `BillingConfig` holds a single product id. |
| **No entitlement tiers, trials or server verification** | Out of scope by design. A product needing them owns that logic. |
| **No entitlement caching** | `core:billing` does not depend on `core:datastore`. Ownership is `Unknown` until Play answers on every cold start. |
| **Restore has no progress indicator** | `refreshPurchases()` can take ~30 s to fail on a bad connection, during which a settings row looks unresponsive. Add your own progress affordance; this is an open consumer-validation item. |
| **Interstitial frequency state is in memory** | A process restart resets the interval — safe rather than exploitable, because the qualifying-action count resets too. |
| **Rewarded ads grant nothing** | `RewardedOutcome.Earned(amount, type)` reports what the ad unit was configured with in AdMob. What it *means* is your business rule. |
| **`ReviewOutcome.Completed` proves nothing** | Play never reveals whether a review was written. Do not thank, reward or re-prompt on it. |
| **The demo is sideloaded** | Its purchases are expected to be unavailable; that path is the "billing unavailable" case, not a bug. |

---

## Release checklist for a monetised app

- [ ] Real ad unit ids wired per build type, from a Gradle property or CI secret — not committed
- [ ] `manifestPlaceholders["admobApplicationId"]` set for **every** build type
- [ ] Release build verified to show real ads (test units in a release build disable advertising)
- [ ] Pro product created and **active** in Play Console, id matching `BillingConfig`
- [ ] Purchase and restore tested against a real Play account on an internal-testing track
- [ ] Restore tested with the network off — must say "could not check", never "nothing found"
- [ ] Ads verified absent immediately after a purchase, and after a cold start as a paying user
- [ ] Privacy options entry reachable wherever `privacyOptionsRequired` is true
- [ ] Privacy policy and terms URLs published and reachable
