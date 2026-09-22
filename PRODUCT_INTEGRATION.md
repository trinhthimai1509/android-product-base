# Product integration

How a product composes Base capabilities **without modifying the Base**.

Read [QUICK_START.md](QUICK_START.md) first for the Gradle wiring. This document is about what
goes in your code once the build works.

---

## Consumer rules

### Consumer products must not modify Product Base source code

The Base is read-only infrastructure. A consumer never edits, patches, forks-in-place or copies a
file out of `android-product-base/`. The only things a consumer build writes there are Gradle's
own `build/` output directories.

That rule is enforceable because every difference a product needs is already a seam:

| You want to change | The seam |
|---|---|
| Colours, type, shapes, spacing | `ProductBaseTheme` parameters — [THEMING.md](THEMING.md) |
| Any user-facing string | Product-supplied text, or override the `pb_*` string in your `res/values` |
| Which capabilities exist | Which modules you include in `settings.gradle.kts` |
| What a capability is configured with | A Hilt `@Provides` in your app module |
| What happens when a row is tapped | The lambda you pass |
| Where a screen navigates | Your NavHost |
| An implementation's behaviour in tests | Your own fake of the Base interface |

### When you hit friction, classify it

Do not "just fix it locally". Classify and report:

| Class | Meaning | What to do |
|---|---|---|
| **A** | Product-specific requirement | Build it in your product. Not a Base concern. |
| **B** | Missing documentation | The API exists but is undocumented or wrongly documented. **Report it.** |
| **C** | Missing broadly reusable Base capability | Several products would need it. **Report it** with the second product that would use it. |
| **D** | Base API or design defect | The seam exists but is wrong, unusable or unsafe. **Report it** with the failing usage. |
| **E** | Specialised extension candidate | Real but narrow — a new optional module, not a change to an existing one. **Report it.** |

A report for C, D or E should carry: what you were building, the API you tried, what happened, and
the smallest change that would have worked. A local edit to the Base produces none of that and
guarantees the next product hits the same wall.

**A is the common case.** The Base deliberately stops at the mechanism; the product owns its
domain, its copy, its navigation and its business rules.

---

## What every product must provide

Two bindings are unavoidable; the rest depend on which modules you included.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Required whenever core:datastore, core:network, core:ads, core:billing or core:review is in.
    @Provides @Singleton
    fun provideLogger(): Logger = if (BuildConfig.DEBUG) AndroidLogger() else NoOpLogger()
}
```

| Included module | You must also provide |
|---|---|
| `core:network` | `NetworkConfig` |
| `core:ads` | `AdsConfig`, `PremiumState`, `manifestPlaceholders["admobApplicationId"]` per build type |
| `core:billing` | `BillingConfig` (it provides the `PremiumState` binding for you) |
| `core:telemetry` **without** `core:telemetry-firebase` | `Analytics` and `CrashReporter` (the no-ops are supplied) |
| `core:notification` | Channels created in `Application.onCreate`; the runtime permission request |

Nothing else. If Hilt reports a missing binding for anything not in that table, that is a class
**B** or **D** finding — report it.

---

## App-level state

Theme and onboarding status are read before the first frame, because both decide what the first
frame *is*. This is the complete ViewModel; it is product code, and it is about twenty lines.

```kotlin
data class AppUiState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
)

@HiltViewModel
class AppViewModel @Inject constructor(private val appPreferences: AppPreferences) : ViewModel() {
    val uiState: StateFlow<AppUiState> = combine(
        appPreferences.themeMode,
        appPreferences.dynamicColorEnabled,
        appPreferences.onboardingCompleted,
    ) { themeMode, dynamicColor, onboardingCompleted ->
        AppUiState(false, themeMode, dynamicColor, onboardingCompleted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setDynamicColorEnabled(enabled) }
    }
}
```

In the Activity, render nothing while loading. One blank frame is the price of never flashing the
light theme at someone who chose dark, and never showing onboarding to someone who finished it:

```kotlin
ProductBaseTheme(themeMode = uiState.themeMode, dynamicColor = uiState.dynamicColorEnabled) {
    if (uiState.isLoading) return@ProductBaseTheme
    AppNavHost(onboardingCompleted = uiState.onboardingCompleted, /* … */)
}
```

Decide the NavHost start destination **once**, or changing it later throws away the back stack:

```kotlin
val startDestination: Any = remember { if (onboardingCompleted) HomeRoute else OnboardingRoute }
```

---

## Onboarding

```kotlin
composable<OnboardingRoute> {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate on the persisted flag, not on the button press: if the write failed, the user
    // stays put rather than landing in an app that will show onboarding again tomorrow.
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            navController.navigate(HomeRoute) { popUpTo(OnboardingRoute) { inclusive = true } }
        }
    }

    OnboardingScreen(
        pages = listOf(                                  // your words, your pictures
            OnboardingPage(
                title = stringResource(R.string.onboarding_1_title),
                description = stringResource(R.string.onboarding_1_body),
                illustration = { Image(painterResource(R.drawable.onboarding_1), null) },
            ),
        ),
        uiState = uiState,
        onNext = viewModel::next,
        onSkip = viewModel::skip,
        onFinish = viewModel::finish,
        onPageChanged = viewModel::goToPage,
    )
}
```

---

## Settings composition

`feature:settings` knows nothing about billing, ads, notifications or review. **A row exists
because you built it**, so an app without a capability has nothing to hide and no flag to forget.

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val billingManager: BillingManager,      // only if you included core:billing
    private val adsController: AdsController,        // only if you included core:ads
    private val reviewManager: AppReviewManager,     // only if you included core:review
) : ViewModel() { /* expose state, expose actions */ }
```

```kotlin
SettingsScreen(
    title = stringResource(R.string.settings),
    sections = listOf(
        SettingsSection(stringResource(R.string.appearance), listOf(
            SettingsRow.ThemePicker(themeMode, onThemeModeChange),
            SettingsRow.DynamicColor(dynamicColorEnabled, onDynamicColorChange),
        )),
        SettingsSection(stringResource(R.string.privacy), buildList {
            // Two conditions: you shipped ads at all, and UMP says this user's region requires
            // an ongoing way to change their choice.
            if (privacyOptionsRequired) {
                add(SettingsRow.Action(stringResource(R.string.privacy_options)) {
                    activity?.let(viewModel::showPrivacyOptions)
                })
            }
            add(SettingsRow.Action(stringResource(R.string.privacy_policy)) {
                if (!context.openUrl(PRIVACY_POLICY_URL)) showNoHandlerMessage()
            })
        }),
        SettingsSection(stringResource(R.string.support), buildList {
            when (purchaseState) {
                PurchaseState.Purchased ->
                    add(SettingsRow.Info(stringResource(R.string.pro), stringResource(R.string.active)))
                else -> {
                    // Cross-feature navigation belongs to you. There is no
                    // feature:settings -> feature:premium dependency, and this lambda is why.
                    add(SettingsRow.Action(stringResource(R.string.remove_ads)) {
                        navController.navigate(PremiumRoute)
                    })
                    add(SettingsRow.Action(stringResource(R.string.restore)) { viewModel.restore() })
                }
            }
            add(SettingsRow.Action(stringResource(R.string.rate)) { activity?.let(viewModel::rate) })
            add(SettingsRow.Action(stringResource(R.string.share)) {
                if (!context.shareText(shareMessage)) showNoHandlerMessage()
            })
        }),
    ),
    navigationIcon = { BackButton(onNavigateBack) },
)
```

**Tip that pays for itself:** build the section list in a plain (non-`@Composable`) function over
resolved strings. Then "which row appears when" is testable without a device — see
[TESTING.md](TESTING.md#test-the-composition-not-the-rendering).

---

## Product shapes

Four combinations, each validated against this tag. Copy the module list, the `AppModule`, and
nothing else.

### Offline app

A tracker, a calculator, a journal. No network, no ads, no purchases.

```kotlin
// settings.gradle.kts
includeBase(":core:common", "core/common")
includeBase(":core:designsystem", "core/designsystem")
includeBase(":core:datastore", "core/datastore")
includeBase(":core:database", "core/database")
includeBase(":core:notification", "core/notification")
includeBase(":feature:onboarding", "feature/onboarding")
includeBase(":feature:settings", "feature/settings")
```

```kotlin
// app/build.gradle.kts — plus id("productbase.android.room") for the module owning @Database
@Provides @Singleton fun provideLogger(): Logger = if (BuildConfig.DEBUG) AndroidLogger() else NoOpLogger()
```

Ships: no OkHttp, no Play Billing, no GMA, no Firebase. The APK contains none of them because the
modules were never included.

### API-backed app

```kotlin
includeBase(":core:common", "core/common")
includeBase(":core:designsystem", "core/designsystem")
includeBase(":core:datastore", "core/datastore")
includeBase(":core:network", "core/network")
includeBase(":core:telemetry", "core/telemetry")
includeBase(":feature:settings", "feature/settings")
```

```kotlin
@Provides @Singleton
fun provideNetworkConfig() = NetworkConfig(
    baseUrl = "https://api.example.com/",
    enableHttpLogging = BuildConfig.DEBUG,       // never anything else
)

@Provides @Singleton
fun provideTrackerApi(retrofit: Retrofit): TrackerApi = retrofit.create(TrackerApi::class.java)

// core:telemetry ships no Hilt module, so bind something:
@Provides @Singleton fun provideAnalytics(): Analytics = NoOpAnalytics()
@Provides @Singleton fun provideCrashReporter(): CrashReporter = NoOpCrashReporter()
```

Map failures at the edge and show mapped text, never a raw exception:

```kotlin
runCatching { api.entries() }
    .onSuccess { entries -> _state.update { state -> state.copy(entries = entries) } }
    .onFailure { throwable ->
        val message = throwable.toNetworkError().toUserMessage()   // your mapping, your words
        _state.update { state -> state.copy(errorMessage = message) }
    }
```

### Ad-supported free app

The app sells nothing, so it never links Play Billing — and `PremiumState.AlwaysFree` is what
tells `core:ads` that.

```kotlin
includeBase(":core:common", "core/common")
includeBase(":core:designsystem", "core/designsystem")
includeBase(":core:datastore", "core/datastore")
includeBase(":core:ads", "core/ads")
includeBase(":core:review", "core/review")
includeBase(":feature:onboarding", "feature/onboarding")
includeBase(":feature:settings", "feature/settings")
```

```kotlin
@Provides @Singleton fun provideAdsConfig(): AdsConfig = AdsConfig.testAds()   // real ids in release
@Provides @Singleton fun providePremiumState(): PremiumState = PremiumState.AlwaysFree
```

```kotlin
// app/build.gradle.kts — a Gradle script cannot reference Kotlin source, so the test value is
// repeated here as a literal. The real one comes from a property, never from the repository.
val testAdmobApplicationId = "ca-app-pub-3940256099942544~3347511713"

android {
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["admobApplicationId"] = testAdmobApplicationId
        }
        getByName("release") {
            manifestPlaceholders["admobApplicationId"] =
                providers.gradleProperty("tracker.admob.applicationId").get()
        }
    }
}
```

```kotlin
// First Activity: consent, then SDK initialisation, before any ad request.
LaunchedEffect(Unit) { adsController.initialize(this@MainActivity) }
```

### Premium / remove-ads app

Everything above, plus billing and the paywall. Ads disappear the moment Play reports the purchase
— `core:billing` provides the `PremiumState` binding, so you provide none.

```kotlin
includeBase(":core:common", "core/common")
includeBase(":core:designsystem", "core/designsystem")
includeBase(":core:datastore", "core/datastore")
includeBase(":core:ads", "core/ads")
includeBase(":core:billing", "core/billing")
includeBase(":core:review", "core/review")
includeBase(":feature:onboarding", "feature/onboarding")
includeBase(":feature:settings", "feature/settings")
includeBase(":feature:premium", "feature/premium")
```

```kotlin
@Provides @Singleton fun provideAdsConfig(): AdsConfig = AdsConfig.testAds()
@Provides @Singleton fun provideBillingConfig() = BillingConfig(proProductId = "pro_lifetime")
// No PremiumState provider — core:billing binds it. Providing one too is a duplicate-binding error.
```

Wire the paywall as a route and navigate to it from wherever you upsell:

```kotlin
composable<PremiumRoute> {
    val viewModel: PremiumViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PremiumScreen(
        content = PremiumContent(                       // your copy, not the Base's
            title = stringResource(R.string.premium_title),
            subtitle = stringResource(R.string.premium_subtitle),
            benefits = listOf(stringResource(R.string.premium_benefit_ads)),
        ),
        uiState = uiState,
        onPurchase = { activity?.let(viewModel::purchase) },
        onRestore = viewModel::restore,
        onDismissMessage = viewModel::dismissMessage,
        navigationIcon = { BackButton(::popBack) },
    )
}
```

Read [MONETIZATION.md](MONETIZATION.md) before shipping either of the last two shapes — especially
the start-up ownership semantics.

---

## Things the Base will never do for you

Not omissions; decisions. Building them in your product is class **A**.

- Your domain model, database entities, API interfaces and DTOs.
- Your navigation graph and your routes.
- Your copy: onboarding pages, paywall benefits, settings labels, share text, store URLs.
- **When** to show an interstitial, ask for a review, or upsell.
- What a rewarded ad's reward *means*.
- Entitlement tiers, trials, subscriptions, server-side receipt verification.
- A global error hierarchy, a BaseViewModel, a generic `UiState`, or a repository layer.
