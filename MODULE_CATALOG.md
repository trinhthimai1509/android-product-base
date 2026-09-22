# Module catalog

Fifteen modules. **A product includes only what it needs**; every module left out of
`settings.gradle.kts` is a dependency the app never links. That is the point of the whole
structure — see [PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md) for worked combinations.

Every API listed here exists in the source at tag `0.2.0-alpha01`. Nothing planned is documented
as existing.

| Module | One line | Third-party weight |
|---|---|---|
| [`core:common`](#corecommon) | Theme mode, logging, premium status | kotlinx-coroutines only |
| [`core:designsystem`](#coredesignsystem) | Compose M3 theme + 13 components | Compose |
| [`core:datastore`](#coredatastore) | Typed app preferences | DataStore |
| [`core:database`](#coredatabase) | Room type converters + policy | Room runtime |
| [`core:network`](#corenetwork) | OkHttp/Retrofit/Json + connectivity | OkHttp, Retrofit, kotlinx-serialization |
| [`core:telemetry`](#coretelemetry) | Analytics + crash contracts | none |
| [`core:telemetry-firebase`](#coretelemetry-firebase) | The only module that sees Firebase | Firebase |
| [`core:ads`](#coreads) | AdMob + UMP consent | play-services-ads, UMP |
| [`core:billing`](#corebilling) | Play Billing | billing-ktx |
| [`core:review`](#corereview) | Play In-App Review | play review |
| [`core:notification`](#corenotification) | Channels, permission, reminders | WorkManager |
| [`core:testing`](#coretesting) | Shared fakes and rules (test-only) | junit, coroutines-test |
| [`feature:onboarding`](#featureonboarding) | Pager, skip/finish, persistence | — |
| [`feature:settings`](#featuresettings) | Settings assembled from supplied rows | androidx-core-ktx |
| [`feature:premium`](#featurepremium) | Paywall mechanism | — |

**Universal rules**

- Every module that logs injects `Logger`. Your product must provide exactly one binding for it.
- No module reads `BuildConfig`. Configuration is passed in from the product.
- No `feature` module depends on another `feature` module. Cross-feature navigation is a lambda
  you write in your NavHost.
- Package prefix `dev.sautao.productbase.*`; resource prefixes `pb_ds_`, `pb_onboarding_`,
  `pb_settings_`, `pb_premium_`.

---

## core:common

**Purpose.** The only module every other one depends on. Theme preference type, logging contract,
and the premium-status seam.

**Use when.** Always — it arrives transitively with anything else.

**Do not use for.** A dumping ground. It holds no error hierarchy, no `UiText`, no date utilities;
those belong to the capability that produces them.

**Reference.** `implementation(project(":core:common"))`

**Public API**

```kotlin
enum class ThemeMode { LIGHT, DARK, SYSTEM }

interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
class AndroidLogger : Logger      // logcat, for debug builds
class NoOpLogger : Logger         // discards, for release builds

interface PremiumState { val status: Flow<PremiumStatus> }
enum class PremiumStatus { UNKNOWN, PREMIUM, FREE }
// PremiumState.AlwaysFree — for apps that sell nothing
```

**Required configuration.** A `Logger` binding (see the minimal example in
[QUICK_START.md](QUICK_START.md#6-the-three-files-that-make-it-an-app)).

**Optional dependencies.** None.

---

## core:designsystem

**Purpose.** The Compose Material 3 theme and the shared component vocabulary.

**Use when.** The product has a Compose UI. Required by `core:ads` (the banner) and by all three
feature modules.

**Do not use for.** Product-specific screens or components — those live in the product. Do not add
your components to this module; a design system that grows a product's one-off card stops being
reusable.

**Reference.** `implementation(project(":core:designsystem"))` (it exposes `core:common` as `api`)

**Public API**

```kotlin
@Composable fun ProductBaseTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    lightColors: ColorScheme = ProductBaseLightColors,
    darkColors: ColorScheme = ProductBaseDarkColors,
    lightSemanticColors: SemanticColors = ProductBaseLightSemanticColors,
    darkSemanticColors: SemanticColors = ProductBaseDarkSemanticColors,
    typography: Typography = MaterialTheme.typography,
    shapes: Shapes = ProductBaseShapes,
    spacing: Spacing = Spacing(),
    content: @Composable () -> Unit,
)

object AppTheme { val spacing: Spacing; val semanticColors: SemanticColors }   // @Composable getters

@Immutable data class Spacing(xs, sm, md, lg, xl, xxl: Dp) { val minTouchTarget: Dp }
@Immutable data class SemanticColors(success, onSuccess, warning, onWarning: Color)
```

Components: `PrimaryButton`, `SecondaryButton`, `AppCard`, `AppDialog`, `AppScaffold`,
`AppTopBar`, `SectionHeader`, `SettingsItem`, `SwitchSettingsItem`, `PremiumBadge`,
`LoadingState`, `EmptyState`, `ErrorState`. All take **already-resolved, localised strings** —
they never hold a string resource.

**Required configuration.** None. Branding is parameters: see [THEMING.md](THEMING.md).

**Minimal example**

```kotlin
ProductBaseTheme(themeMode = ThemeMode.SYSTEM, dynamicColor = true) {
    AppScaffold(topBar = { AppTopBar(title = stringResource(R.string.home)) }) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryButton(text = stringResource(R.string.start), onClick = ::start)
        }
    }
}
```

**Optional dependencies.** None.

---

## core:datastore

**Purpose.** The single owner of application preference keys, so two features cannot collide.

**Use when.** The product persists theme, onboarding completion or a notification switch — i.e.
almost always. Required by `feature:onboarding`.

**Do not use for.** Product-domain data (entries, documents, sync state) — that is a database or
the product's own DataStore. Never for secrets: the file is unencrypted protobuf.

**Reference.** `implementation(project(":core:datastore"))`

**Public API**

```kotlin
interface AppPreferences {
    val themeMode: Flow<ThemeMode>                 // default SYSTEM
    val dynamicColorEnabled: Flow<Boolean>         // default true
    val onboardingCompleted: Flow<Boolean>         // default false
    val notificationsEnabled: Flow<Boolean>        // default true

    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
```

`notificationsEnabled` is the *app's* switch, independent of the system permission; what it gates
is the product's decision.

**Required configuration.** A `Logger` binding. The implementation and its DataStore (file
`app_preferences`) are provided by the module's Hilt module; a corrupt file resets to defaults
rather than crashing.

**Minimal example**

```kotlin
@HiltViewModel
class ThemeViewModel @Inject constructor(private val prefs: AppPreferences) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }
}
```

**Optional dependencies.** None.

---

## core:database

**Purpose.** Room type converters and the shared migration policy. **It contains no entities and
no `@Database`.**

**Use when.** The product owns a Room database and wants `Instant` columns handled consistently.

**Do not use for.** Expecting a database to be provided — you declare your own `@Database`, DAOs
and migrations in the product, and apply the `productbase.android.room` convention plugin there.

**Reference.** `implementation(project(":core:database"))` plus `id("productbase.android.room")`
on the module that declares the `@Database`.

**Public API**

```kotlin
class InstantConverters {   // Instant <-> epoch millis
    @TypeConverter fun instantToEpochMilli(instant: Instant?): Long?
    @TypeConverter fun epochMilliToInstant(epochMilli: Long?): Instant?
}
```

**Required configuration.** None from the module. The convention plugin exports schemas to
`<module>/schemas` — commit them; they are what makes migration tests possible.

**Minimal example**

```kotlin
@Database(entities = [EntryEntity::class], version = 2, exportSchema = true)
@TypeConverters(InstantConverters::class)
abstract class TrackerDatabase : RoomDatabase() { abstract fun entryDao(): EntryDao }

// Never configure fallbackToDestructiveMigration(): a missing migration must stop a developer,
// not delete a user's data.
Room.databaseBuilder(context, TrackerDatabase::class.java, "tracker.db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

**Optional dependencies.** None.

---

## core:network

**Purpose.** OkHttp + Retrofit + kotlinx-serialization wiring, an owned error type, and
connectivity monitoring.

**Use when.** The product calls an HTTP API, **or** wants an "you're offline" hint.

**Do not use for.** Caching, sync or repositories — there are none here. It ships no API
interface and no DTO; those are product code.

**Reference.** `implementation(project(":core:network"))` plus
`alias(libs.plugins.kotlin.serialization)` on the module that declares `@Serializable` DTOs.

**Public API**

```kotlin
data class NetworkConfig(
    val baseUrl: String,
    val connectTimeout: Duration = 15.seconds,
    val readTimeout: Duration = 20.seconds,
    val writeTimeout: Duration = 20.seconds,
    val enableHttpLogging: Boolean = false,      // wire to BuildConfig.DEBUG and nothing else
)

sealed interface NetworkError { NoConnection; Timeout; Http(code); Malformed; Unexpected(cause) }
fun Throwable.toNetworkError(): NetworkError

interface NetworkMonitor { val isOnline: Flow<Boolean> }   // cold; collect from a lifecycle scope
```

Hilt provides `Json`, `OkHttpClient`, `Retrofit` and `NetworkMonitor`. `INTERNET` and
`ACCESS_NETWORK_STATE` come from this module's manifest.

**Required configuration.** A `NetworkConfig` binding and a `Logger` binding.

**Minimal example**

```kotlin
@Provides @Singleton
fun provideNetworkConfig() = NetworkConfig(
    baseUrl = "https://api.example.com/",
    enableHttpLogging = BuildConfig.DEBUG,
)

@Provides @Singleton
fun provideTrackerApi(retrofit: Retrofit): TrackerApi = retrofit.create(TrackerApi::class.java)

// In a ViewModel:
val result = runCatching { api.entries() }.getOrElse { return show(it.toNetworkError()) }
```

**Optional dependencies.** A second API instance: build another Retrofit from the provided
`OkHttpClient` and `Json` with your own qualifier.

---

## core:telemetry

**Purpose.** Analytics and crash-reporting **contracts**, with no-op implementations. This is the
boundary that makes Firebase optional graph-wide.

**Use when.** Product code wants to log events without knowing the provider.

**Do not use for.** A taxonomy of events — you name your own.

**Reference.** `implementation(project(":core:telemetry"))`

**Public API**

```kotlin
interface Analytics {
    fun logScreenView(screenName: String)
    fun logEvent(event: AnalyticsEvent)
}
data class AnalyticsEvent(val name: String, val params: Map<String, Any> = emptyMap())
class NoOpAnalytics : Analytics

interface CrashReporter {
    fun recordNonFatal(throwable: Throwable)
    fun log(message: String)
    fun setCustomKey(key: String, value: String)
}
class NoOpCrashReporter : CrashReporter
```

**Required configuration.** **This module ships no Hilt module.** Either include
`core:telemetry-firebase`, or bind the no-ops yourself:

```kotlin
@Provides @Singleton fun provideAnalytics(): Analytics = NoOpAnalytics()
@Provides @Singleton fun provideCrashReporter(): CrashReporter = NoOpCrashReporter()
```

**Optional dependencies.** `core:telemetry-firebase`, or your own implementation of the two
interfaces.

---

## core:telemetry-firebase

**Purpose.** The only module in the repository that may see a Firebase type. Binds `Analytics`
and `CrashReporter` to Firebase Analytics and Crashlytics.

**Use when.** The product uses Firebase.

**Do not use for.** Remote Config, Messaging, Auth or Firestore — none are here.

**Reference.** `implementation(project(":core:telemetry-firebase"))` (exposes `core:telemetry` as
`api`)

**Public API.** None. It is bindings only; product code talks to `core:telemetry`.

**Required configuration.** Drop `google-services.json` into the app module and apply the plugins
conditionally:

```kotlin
val googleServicesConfig = file("google-services.json")
if (googleServicesConfig.exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}
```

Without the file the module detects that Firebase is unconfigured and binds the **no-op**
implementations instead of crashing — so a fresh clone builds and runs. Keep
`google-services.json` out of version control.

**Optional dependencies.** None.

---

## core:ads

**Purpose.** Google Mobile Ads and UMP consent, behind a product-facing API. No GMA or UMP type
appears outside it.

**Use when.** The product shows advertising.

**Do not use for.** Deciding *when* an ad is appropriate — that is the product's call. There is no
remote ad policy and no session model.

**Reference.** `implementation(project(":core:ads"))`

**Public API**

```kotlin
interface AdsController {
    val adsAvailable: Flow<Boolean>
    val privacyOptionsRequired: StateFlow<Boolean>
    val bannerAdUnitId: String
    suspend fun initialize(activity: Activity)
    suspend fun showPrivacyOptions(activity: Activity): ConsentOutcome
    suspend fun showInterstitial(activity: Activity): InterstitialOutcome
    suspend fun showRewarded(activity: Activity): RewardedOutcome
    fun recordQualifyingAction()
}

@Composable fun AppBannerAd(adsController: AdsController, modifier: Modifier = Modifier)

data class AdsConfig(bannerAdUnitId, interstitialAdUnitId, rewardedAdUnitId: String,
                     adsEnabled: Boolean = true, interstitialPolicy: InterstitialPolicy = …)
data class InterstitialPolicy(minInterval: Duration = 3.minutes, minQualifyingActions: Int = 3)
object TestAdUnits { APPLICATION_ID; BANNER; INTERSTITIAL; REWARDED }

sealed interface InterstitialOutcome { Shown; NotAvailable; Throttled; Failed(code) }
sealed interface RewardedOutcome { Earned(amount, type); Dismissed; NotAvailable; Failed(code) }
sealed interface ConsentOutcome { Completed; NotRequired; Failed(code) }
```

**Required configuration.** An `AdsConfig` binding, a `PremiumState` binding (from `core:billing`
or `PremiumState.AlwaysFree`), a `Logger` binding, and `manifestPlaceholders["admobApplicationId"]`
for **every** build type. Call `initialize(activity)` once from your first Activity. Full detail
in [MONETIZATION.md](MONETIZATION.md).

**Minimal example**

```kotlin
@Provides @Singleton fun provideAdsConfig(): AdsConfig = AdsConfig.testAds()
@Provides @Singleton fun providePremiumState(): PremiumState = PremiumState.AlwaysFree

// In the first Activity:
LaunchedEffect(Unit) { adsController.initialize(this@MainActivity) }
// Anywhere in the UI — renders nothing at all when ads are unavailable:
AppBannerAd(adsController = adsController)
```

**Optional dependencies.** `core:billing` (turns "premium" into a real purchase).

---

## core:billing

**Purpose.** Google Play Billing for one lifetime "Pro / remove ads" product, behind a
product-facing API. Publishes the `PremiumState` binding.

**Use when.** The product sells an in-app purchase.

**Do not use for.** Subscriptions, entitlement tiers, receipt verification or entitlement caching
— none exist. Google Play is the only source of ownership.

**Reference.** `implementation(project(":core:billing"))`

**Public API**

```kotlin
interface BillingManager {
    val purchaseState: StateFlow<PurchaseState>     // starts Unknown
    val proProduct: StateFlow<ProProduct?>          // null while loading or unavailable
    suspend fun refreshPurchases()                  // this is also "restore purchases"
    suspend fun launchPurchase(activity: Activity): PurchaseOutcome
}

data class BillingConfig(val proProductId: String)
sealed interface PurchaseState { Unknown; NotPurchased; Pending; Purchased }
sealed interface PurchaseOutcome { Purchased; Pending; AlreadyOwned; Cancelled; Failed(error) }
sealed interface BillingError { BillingUnavailable; ServiceDisconnected; NetworkUnavailable;
                                ProductUnavailable; DeveloperError; Unknown(code) }
data class ProProduct(productId, title, description, formattedPrice: String)
```

**Required configuration.** A `BillingConfig` binding and a `Logger` binding. Including this
module *is* the decision that premium means "owns the Pro product".

**Optional dependencies.** `feature:premium` (a ready paywall), `core:ads` (ads disappear when
`PurchaseState.Purchased`).

---

## core:review

**Purpose.** Play In-App Review, as one call.

**Use when.** The product wants to ask for a rating at a moment it chooses.

**Do not use for.** Deciding when to ask. There is no counter, no engagement score, no automatic
prompt and no remote policy — deliberately.

**Reference.** `implementation(project(":core:review"))`

**Public API**

```kotlin
interface AppReviewManager { suspend fun requestReview(activity: Activity): ReviewOutcome }
sealed interface ReviewOutcome { Completed; Unavailable }
```

`Completed` means the flow ran — **not** that a dialog appeared or a review was written. Play
never says. Do not thank, reward or re-prompt based on it. Nothing throws.

**Required configuration.** A `Logger` binding. Nothing else.

**Minimal example**

```kotlin
fun onTaskFinished(activity: Activity) = viewModelScope.launch {
    if (reviewManager.requestReview(activity) == ReviewOutcome.Unavailable) {
        // Optional: say nothing, or offer a store link. Never treat it as an error.
    }
}
```

**Optional dependencies.** None.

---

## core:notification

**Purpose.** Notification channels, the `POST_NOTIFICATIONS` helper, and deferrable local
reminders on WorkManager.

**Use when.** The product posts local notifications or schedules reminders.

**Do not use for.** Push messaging (not here), or exact alarms — reminders are deferrable work and
`SCHEDULE_EXACT_ALARM` is deliberately not declared. An alarm clock needs `AlarmManager` and its
own policy conversation.

**Reference.** `implementation(project(":core:notification"))`

**Public API**

```kotlin
data class NotificationChannelSpec(id, name: String, description: String? = null,
                                   importance: Int = IMPORTANCE_DEFAULT)
fun Context.ensureNotificationChannels(channels: List<NotificationChannelSpec>)

const val POST_NOTIFICATIONS_PERMISSION: String
fun Context.canPostNotifications(): Boolean      // permission AND user setting

data class ReminderRequest(id: String, triggerAt: Instant, channelId: String, title: String,
                           text: String, @DrawableRes smallIconResId: Int, deepLink: String? = null)
interface ReminderScheduler {
    fun schedule(request: ReminderRequest)       // same id replaces
    fun cancel(id: String)
}
```

**Required configuration.** Create channels at start-up (`Application.onCreate`). Request the
runtime permission from your UI — this module ships no Compose dependency and no permission UI.
`POST_NOTIFICATIONS` is declared by the module's manifest.

**Minimal example**

```kotlin
override fun onCreate() {
    super.onCreate()
    ensureNotificationChannels(listOf(
        NotificationChannelSpec(id = "reminders", name = getString(R.string.channel_reminders)),
    ))
}

scheduler.schedule(ReminderRequest(
    id = "daily", triggerAt = Instant.now().plus(1, ChronoUnit.DAYS), channelId = "reminders",
    title = title, text = text, smallIconResId = R.drawable.ic_notification,
    deepLink = "tracker://today",
))
```

**Optional dependencies.** `core:datastore` if you gate reminders on a user preference.

---

## core:testing

**Purpose.** Test-only helpers shared by more than one module.

**Use when.** Writing unit tests. `testImplementation(project(":core:testing"))`.

**Do not use for.** Capability fakes for billing, ads or review — they are deliberately **not**
here, because that would put those SDKs on the test classpath of modules that do not link them.
Write those fakes in your product; [TESTING.md](TESTING.md) has copy-ready versions.

**Public API**

```kotlin
class MainDispatcherRule(dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher()

class FakeAppPreferences(
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
    initialDynamicColorEnabled: Boolean = true,
    initialOnboardingCompleted: Boolean = false,
    initialNotificationsEnabled: Boolean = true,
) : AppPreferences
```

**Required configuration.** None. It exposes junit and coroutines-test as `api`, so a consumer
test module gets both.

---

## feature:onboarding

**Purpose.** A reusable onboarding *mechanism*: pager, next, skip, finish, observable state and
the persisted completion flag. It ships no content.

**Use when.** The product shows a first-launch introduction.

**Do not use for.** Feature tours, tooltips, or a content system — there is none.

**Reference.** `implementation(project(":feature:onboarding"))`

**Public API**

```kotlin
data class OnboardingPage(val title: String, val description: String,
                          val illustration: (@Composable () -> Unit)? = null)

data class OnboardingUiState(val pageIndex: Int = 0, val isCompleted: Boolean = false)

@HiltViewModel class OnboardingViewModel : ViewModel() {
    val uiState: StateFlow<OnboardingUiState>
    fun next(pageCount: Int)     // advances, or finishes when the last page is exhausted
    fun goToPage(index: Int)     // report a swipe
    fun skip()                   // completes; a skipped onboarding is a completed one
    fun finish()                 // completes
}

@Composable fun OnboardingScreen(
    pages: List<OnboardingPage>, uiState: OnboardingUiState,
    onNext: (pageCount: Int) -> Unit, onSkip: () -> Unit, onFinish: () -> Unit,
    onPageChanged: (Int) -> Unit, modifier: Modifier = Modifier,
)
```

Base-owned strings, overridable in your `res/values`: `pb_onboarding_skip`, `pb_onboarding_next`,
`pb_onboarding_finish`, `pb_onboarding_page_indicator`.

**Required configuration.** `core:datastore` (transitive) and a `Logger` binding. Navigate on
`uiState.isCompleted`, which is read back from storage — not on the button press.

**Optional dependencies.** None.

---

## feature:settings

**Purpose.** A settings screen assembled from rows the product supplies. **The lightest feature
module: `core:designsystem`, plus `androidx-core-ktx` for one `String.toUri()` call. No billing,
no ads, no notifications, no review, no Firebase — and no Hilt.**

**Use when.** The product has a settings screen.

**Do not use for.** Storing anything — it holds no state and has no ViewModel. The product owns
every value and every callback.

**Reference.** `implementation(project(":feature:settings"))`

**Public API**

```kotlin
data class SettingsSection(val title: String? = null, val rows: List<SettingsRow>)

sealed interface SettingsRow {
    data class Action(title: String, onClick: () -> Unit, subtitle: String? = null)
    data class Toggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit,
                      subtitle: String? = null, enabled: Boolean = true)
    data class Info(title: String, value: String)
    data class ThemePicker(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit)
    data class DynamicColor(enabled: Boolean, onEnabledChange: (Boolean) -> Unit)
}

@Composable fun SettingsScreen(title: String, sections: List<SettingsSection>,
                               modifier: Modifier = Modifier,
                               navigationIcon: @Composable () -> Unit = {})

fun Context.shareText(text: String, chooserTitle: String? = null): Boolean
fun Context.openUrl(url: String): Boolean      // http/https only; returns false otherwise
```

`ThemePicker` and `DynamicColor` label themselves (`pb_settings_*` strings); every other row is
labelled by you. `DynamicColor` renders disabled with an explanation below Android 12. Sections
with no rows are not rendered, so capability-driven lists need no filtering.

**Required configuration.** None.

**Minimal example**

```kotlin
SettingsScreen(
    title = stringResource(R.string.settings),
    sections = listOf(
        SettingsSection("Appearance", listOf(
            SettingsRow.ThemePicker(themeMode, onThemeModeChange),
            SettingsRow.DynamicColor(dynamicColorEnabled, onDynamicColorChange),
        )),
        SettingsSection("About", listOf(
            SettingsRow.Action("Share this app", onClick = { context.shareText(message) }),
            SettingsRow.Info("Version", BuildConfig.VERSION_NAME),
        )),
    ),
)
```

**Optional dependencies.** None — and that is the design. Premium, consent and notification rows
are wired by the product; see [PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md#settings-composition).

---

## feature:premium

**Purpose.** A paywall mechanism: benefits list, Play-supplied price, purchase, restore, owned
state. It ships no marketing copy.

**Use when.** The product sells the Pro purchase and wants a ready screen.

**Do not use for.** Multi-tier pricing or a subscription comparison table.

**Reference.** `implementation(project(":feature:premium"))` (requires `core:billing`)

**Public API**

```kotlin
data class PremiumContent(val title: String, val subtitle: String,
                          val benefits: List<String>, val footnote: String? = null)

data class PremiumUiState(purchaseState: PurchaseState = Unknown, product: ProProduct? = null,
                          isWorking: Boolean = false, message: PremiumMessage? = null)

sealed interface PremiumMessage { PurchaseCompleted; PurchasePending; AlreadyOwned;
                                  NothingToRestore; CouldNotCheck; Failed(error) }

@HiltViewModel class PremiumViewModel : ViewModel() {
    val uiState: StateFlow<PremiumUiState>
    fun purchase(activity: Activity)
    fun restore()
    fun dismissMessage()
}

@Composable fun PremiumScreen(content: PremiumContent, uiState: PremiumUiState,
                              onPurchase: () -> Unit, onRestore: () -> Unit,
                              onDismissMessage: () -> Unit, modifier: Modifier = Modifier,
                              navigationIcon: @Composable () -> Unit = {})
```

**Required configuration.** Whatever `core:billing` needs. The price comes from Google Play,
already localised — never format or hardcode one.

**Optional dependencies.** None. Navigation to it is a lambda in your NavHost.
