# Testing

What the Base gives you, what to write yourself, and — more usefully — what not to test at all.

The Base ships 112 unit tests and no test framework of its own. JUnit 4, `kotlinx-coroutines-test`
and Turbine are the tools; every version comes from the shared catalog.

---

## core:testing

```kotlin
testImplementation(project(":core:testing"))
```

It exposes JUnit and `kotlinx-coroutines-test` as `api`, so that one line is usually all a test
module needs.

### MainDispatcherRule

```kotlin
class MainDispatcherRule(dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher()
```

Swaps `Dispatchers.Main` so ViewModels can be tested off-device.

```kotlin
class HomeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
}
```

### FakeAppPreferences

```kotlin
class FakeAppPreferences(
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
    initialDynamicColorEnabled: Boolean = true,
    initialOnboardingCompleted: Boolean = false,
    initialNotificationsEnabled: Boolean = true,
) : AppPreferences
```

In-memory, backed by `MutableStateFlow`s, and readable after the fact — so a test can assert both
what the ViewModel exposed and what was actually persisted:

```kotlin
@Test
fun `finishing onboarding is persisted`() = runTest {
    val preferences = FakeAppPreferences()
    val viewModel = OnboardingViewModel(preferences)

    viewModel.uiState.test {
        assertFalse(awaitItem().isCompleted)
        viewModel.finish()
        assertTrue(awaitItem().isCompleted)
        assertTrue(preferences.onboardingCompleted.value)   // it really was written
        cancelAndIgnoreRemainingEvents()
    }
}
```

### What is deliberately **not** in core:testing

No `FakeBillingManager`, `FakeAdsController` or `FakeAppReviewManager`. Putting them here would
give `core:testing` a dependency on billing, ads and review — and therefore put those SDKs on the
test classpath of every module that deliberately does not link them.

Write them in your product. Each is a dozen lines, and copy-ready versions are below.

---

## Product fakes

Every Base capability is an interface for exactly this reason. These are the fakes the Base's own
demo uses; copy them into your `src/test`.

```kotlin
class FakeBillingManager(initialState: PurchaseState = PurchaseState.NotPurchased) : BillingManager {
    private val _purchaseState = MutableStateFlow(initialState)
    override val purchaseState: StateFlow<PurchaseState> = _purchaseState
    override val proProduct: StateFlow<ProProduct?> = MutableStateFlow(null)

    /** What Google Play reports on the next refresh. */
    var stateAfterRefresh: PurchaseState = initialState
    var refreshCount: Int = 0; private set

    override suspend fun refreshPurchases() {
        refreshCount++
        _purchaseState.value = stateAfterRefresh
    }

    override suspend fun launchPurchase(activity: Activity): PurchaseOutcome = PurchaseOutcome.Cancelled
}

class FakeAdsController(privacyOptionsRequired: Boolean = false) : AdsController {
    override val adsAvailable: Flow<Boolean> = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = MutableStateFlow(privacyOptionsRequired)
    override val bannerAdUnitId: String = "fake-banner"
    var privacyOptionsShown: Int = 0; private set

    override suspend fun initialize(activity: Activity) = Unit
    override suspend fun showPrivacyOptions(activity: Activity): ConsentOutcome {
        privacyOptionsShown++
        return ConsentOutcome.Completed
    }
    override suspend fun showInterstitial(activity: Activity) = InterstitialOutcome.NotAvailable
    override suspend fun showRewarded(activity: Activity) = RewardedOutcome.NotAvailable
    override fun recordQualifyingAction() = Unit
}

class FakeAppReviewManager(var outcome: ReviewOutcome = ReviewOutcome.Completed) : AppReviewManager {
    var requests: Int = 0; private set
    override suspend fun requestReview(activity: Activity): ReviewOutcome {
        requests++
        return outcome
    }
}
```

Also easy to fake when you need them: `NetworkMonitor` (one `Flow<Boolean>`), `ReminderScheduler`
(record calls), `Analytics` / `CrashReporter` (`NoOpAnalytics` and `NoOpCrashReporter` ship with
`core:telemetry` and are usually enough).

---

## Recommended product test patterns

### 1. Test the composition, not the rendering

The most valuable settings test is not a UI test. Build your section list in a plain function over
already-resolved strings, and the rule "which row appears when" becomes a pure-Kotlin assertion:

```kotlin
internal fun settingsSections(
    capabilities: Capabilities,      // nullable fields = "this product lacks that module"
    labels: Labels,                  // resolved strings
    actions: Actions,                // lambdas
): List<SettingsSection>

@Test
fun `an app without billing shows no premium and no restore`() {
    val sections = settingsSections(capabilities.copy(purchaseState = null), LABELS, ACTIONS)

    assertFalse(sections.hasRow(LABELS.premiumOffer))
    assertFalse(sections.hasRow(LABELS.restore))
    assertTrue(sections.hasRow(LABELS.rate))     // unrelated rows survive
}
```

The Composable then just renders the list. This is how the Base tests its own demo, and it catches
the mistakes that matter (a row shown to someone who already paid) without an emulator.

### 2. Test state mapping, not plumbing

Pure functions over states are cheap and permanent. Write them in your product — the Base's own
ownership mapping is `internal` to `core:billing` and is already tested there:

```kotlin
// your product: what a purchase state means to *your* UI
internal fun PurchaseState.toUpgradeRow(): UpgradeRow = when (this) {
    PurchaseState.Purchased -> UpgradeRow.Owned
    PurchaseState.Unknown, PurchaseState.Pending, PurchaseState.NotPurchased -> UpgradeRow.Offer
}

@Test fun `unresolved ownership still offers the upgrade`() {
    assertEquals(UpgradeRow.Offer, PurchaseState.Unknown.toUpgradeRow())
}
```

For a ViewModel, assert what it *decides*, not how it got there:

```kotlin
@Test
fun `restore does not tell a paying user they own nothing when Play was unreachable`() = runTest {
    val billing = FakeBillingManager(PurchaseState.Unknown).apply {
        stateAfterRefresh = PurchaseState.Unknown
    }
    val viewModel = SettingsViewModel(prefs, billing, ads, review)

    viewModel.uiState.test {
        awaitItem()
        viewModel.restorePurchases()
        assertEquals(Message.CouldNotCheckPurchases, awaitItem().message)
        cancelAndIgnoreRemainingEvents()
    }
}
```

### 3. Turbine for flows, and always cancel

```kotlin
viewModel.uiState.test {
    assertEquals(expected, awaitItem())
    cancelAndIgnoreRemainingEvents()
}
```

A `stateIn(..., WhileSubscribed(...))` flow has no collector until you subscribe: asserting on
`.value` before that gives you the initial value, which is a real test only if that is what you
meant to check.

### 4. Passing an `Activity` in a unit test

The Base's convention plugin sets `testOptions.unitTests.isReturnDefaultValues = true`, so Android
stubs return defaults instead of throwing `"Stub!"`. That makes this legal:

```kotlin
private val ACTIVITY = android.app.Activity()   // never touched; only passed along
viewModel.requestReview(ACTIVITY)
```

Use it only for objects your code passes *through*. Anything that actually calls into the
framework belongs in an instrumentation test, not against a hollow stub.

### 5. Room migrations get a real test

If your product owns a database, keep schemas committed (the `productbase.android.room` plugin
exports them) and test the migration against the real previous schema with
`MigrationTestHelper` in `androidTest`. Destructive fallback is never configured anywhere in this
Base, and should not be in your product either: a missing migration must stop a developer, not
delete a user's data.

---

## What should NOT be mocked

| Do not mock | Why | Do this instead |
|---|---|---|
| **Google Play Billing** (`BillingClient`, `Purchase`, `BillingResult`) | Faking a payments SDK produces confident tests about behaviour Play does not have. The Base found a real bug this way: Play's JSON encodes "pending" as `4`, not as the SDK's `PENDING` constant. | Fake `BillingManager`; test the real SDK on a device with an internal-testing track |
| **Google Mobile Ads / UMP** | Same reason, plus ad delivery depends on fill, region and consent | Fake `AdsController`; verify real ads on a device |
| **Play In-App Review** | Play decides whether anything appears, and never reports the outcome | Fake `AppReviewManager`; check the real path on a device once |
| **Firebase** | It is behind `Analytics` / `CrashReporter` for exactly this reason | `NoOpAnalytics`, `NoOpCrashReporter`, or your own recording fake |
| **The Android framework** (`Context`, `Activity`, `Intent`, `ConnectivityManager`) | Stubs teach you nothing about Android's actual behaviour | Keep framework use at the edge; test the decision that led to it |
| **Compose rendering, to prove a row is hidden** | Slow, brittle, and it tests the renderer rather than your rule | Assert on the section list (pattern 1) |
| **Your own value types and data classes** | Nothing to mock | Construct them |

The rule behind the table: **mock the boundary you own, exercise the boundary you don't.** Every
Base capability is an interface so the first half is easy; the second half needs a device, and no
amount of mocking replaces it.

---

## Running tests

```bash
./gradlew :app:test                  # unit tests
./gradlew :app:connectedAndroidTest  # instrumentation, needs a device or emulator
./gradlew :app:lint                  # lint, with checkDependencies on for applications
./gradlew :app:assembleRelease       # proves R8 does not break the app
```

The last one is a test. A build that only ever runs `assembleDebug` finds R8 problems from users.

### Device checks worth doing once per release

- Cold start as a **paying** user: no ad appears at any point
- Restore with the network off: the message says "could not check", never "nothing found"
- Force-stop and relaunch: theme and onboarding completion survive
- Share and any external link: they open, and a device with no handler does not crash the app
- Notification permission denied, then granted: the app behaves sensibly on both sides
