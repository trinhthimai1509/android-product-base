# New app checklist

For an agent (or a human) creating a product from this Base. Work top to bottom; each phase ends
with a command that must succeed before you continue.

**The rule that governs all of it:** never edit, patch or copy a file inside
`android-product-base/`. If you cannot do something without touching it, stop and report it —
classification table in [PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md#consumer-rules).

---

## 0. Decide before you type

- [ ] What does this product *do*? One sentence.
- [ ] Which shape is it: offline · API-backed · ad-supported · premium?
      ([PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md#product-shapes))
- [ ] From that, list the Base modules to include. Everything else stays out —
      an excluded module is an SDK the app never ships.
- [ ] Confirm the tools: **JDK 21** for the Gradle daemon, Android SDK **37** installed.

## 1. Workspace

- [ ] `workspace/android-product-base/` and `workspace/consumer-app/` are siblings
- [ ] Copy `gradlew` and the `gradle/` directory from the Base, then **delete**
      `gradle/libs.versions.toml` from the copy (the Base owns versions)
- [ ] Keep `gradle/gradle-daemon-jvm.properties` — without it the build fails on Java 17
- [ ] `local.properties` points at your Android SDK

## 2. Gradle wiring

- [ ] `settings.gradle.kts`: `pluginManagement { includeBuild("../android-product-base/build-logic") }`
- [ ] Copy `gradle/libs.versions.toml` from the Base and **do not** declare an explicit
      `versionCatalogs { from(...) }` block — it conflicts with `build-logic`'s own catalog
      declaration (`Multiple 'from' invocations`). See the warning in
      [QUICK_START.md](QUICK_START.md#2-settingsgradlekts).
- [ ] `settings.gradle.kts`: map `:core` and `:feature` containers **and** each module you need
      (the exact block is in [QUICK_START.md](QUICK_START.md#2-settingsgradlekts))
- [ ] Root `build.gradle.kts`: `buildscript { classpath(libs.kotlin.gradlePlugin) }` and the
      `apply false` plugin aliases
- [ ] `app/build.gradle.kts`: `productbase.android.application`, `.compose`, `.hilt`
      (add `.room` only on the module that declares a `@Database`)
- [ ] `buildFeatures { buildConfig = true }` if you use `BuildConfig.DEBUG`
- [ ] `app/proguard-rules.pro` exists (release builds are minified by default)
- [ ] ✅ `./gradlew :app:assembleDebug`

## 3. Application skeleton

- [ ] `@HiltAndroidApp class YourApplication : Application()`, named in the manifest
- [ ] `AppModule` provides `Logger` — `AndroidLogger()` in debug, `NoOpLogger()` in release
- [ ] Per-module configuration bindings from
      [the table](PRODUCT_INTEGRATION.md#what-every-product-must-provide):
      `NetworkConfig` · `AdsConfig` + `PremiumState` · `BillingConfig` · `Analytics`/`CrashReporter`
- [ ] `@AndroidEntryPoint class MainActivity`, `enableEdgeToEdge()`, `setContent { }`
- [ ] An XML theme on the Activity for the pre-Compose window background
- [ ] ✅ `./gradlew :app:assembleDebug`

## 4. Theme

- [ ] `AppViewModel` reads `AppPreferences` (theme, dynamic colour, onboarding completion)
- [ ] `ProductBaseTheme(themeMode, dynamicColor)` wraps the whole app
- [ ] **Render nothing while `isLoading`** — no theme flash, no onboarding flash
- [ ] Your `lightColors` / `darkColors` passed in — shipping the Base's neutral palette is a
      decision to have no brand ([THEMING.md](THEMING.md))
- [ ] Typography, shapes, spacing overridden if the design calls for it
- [ ] ✅ Run on a device: switch light/dark, force-stop, confirm the choice survives

## 5. Navigation

- [ ] Type-safe routes (`@Serializable data object HomeRoute`)
- [ ] Start destination captured **once** with `remember` — changing it later resets the back stack
- [ ] Cross-feature navigation is a lambda in the NavHost, never a feature→feature dependency

## 6. Onboarding (if used)

- [ ] `OnboardingPage`s written by you — title, description, optional illustration slot
- [ ] Navigate on `uiState.isCompleted` (persisted), not on the button press
- [ ] `popUpTo(OnboardingRoute) { inclusive = true }` so back does not return to it
- [ ] ✅ Device: finish it, force-stop, relaunch — it must not reappear
- [ ] ✅ Device: skip it, force-stop, relaunch — same result (a skip *is* a completion)

## 7. Settings (if used)

- [ ] Sections built in a plain function over resolved strings, so visibility is unit-testable
- [ ] A row exists only when the product has that capability — no feature-flag object
- [ ] Theme rows: `SettingsRow.ThemePicker` + `SettingsRow.DynamicColor`
- [ ] Privacy-options row gated on `adsController.privacyOptionsRequired`
- [ ] Premium/restore rows only when billing is included, and only while not `Purchased`
- [ ] Legal links via `openUrl` (http/https only), share via `shareText` — handle `false` returns
- [ ] Version row from `BuildConfig.VERSION_NAME`
- [ ] ✅ `./gradlew :app:test` covering capability visibility

## 8. Monetisation (if used) — read [MONETIZATION.md](MONETIZATION.md) first

- [ ] `manifestPlaceholders["admobApplicationId"]` set for **every** build type
- [ ] Debug uses `AdsConfig.testAds()`; release ids come from a Gradle property, not the repo
- [ ] `adsController.initialize(activity)` called once from the first Activity — consent first
- [ ] `AppBannerAd` placed where an empty slot would not break the layout (it renders nothing)
- [ ] Interstitials only after a completed task; `recordQualifyingAction()` on real use
- [ ] `BillingConfig.proProductId` matches an **active** Play Console product
- [ ] Restore wired to `refreshPurchases()`, with a progress affordance (it can take ~30 s to fail)
- [ ] Restore failure says "could not check", never "nothing found"
- [ ] ✅ Device: as a paying user, cold start shows **no** ad at any point

## 9. Data (if used)

- [ ] `@Database` + DAOs + entities in your product, `InstantConverters` from `core:database`
- [ ] Schemas committed; migrations written and tested; **no destructive fallback**
- [ ] `NetworkConfig.enableHttpLogging = BuildConfig.DEBUG` and nothing else
- [ ] API interfaces and DTOs in your product; errors mapped with `toNetworkError()`

## 10. Notifications (if used)

- [ ] Channels created in `Application.onCreate` via `ensureNotificationChannels`
- [ ] `POST_NOTIFICATIONS` requested from your UI; `canPostNotifications()` before promising anything
- [ ] Reminder ids stable; deep link matches an intent-filter
- [ ] Remember reminders are **deferrable** — not an alarm clock

## 11. Tests

- [ ] `testImplementation(project(":core:testing"))`
- [ ] `MainDispatcherRule` on every ViewModel test
- [ ] Fakes for `BillingManager` / `AdsController` / `AppReviewManager` in your product
      ([TESTING.md](TESTING.md#product-fakes))
- [ ] Do not mock Play SDKs, Firebase or the Android framework
- [ ] ✅ `./gradlew :app:test`

## 12. Release readiness

- [ ] ✅ `./gradlew :app:lint` — clean, or every warning consciously accepted
- [ ] ✅ `./gradlew :app:assembleRelease` — R8 must not break the app
- [ ] Install the **release** build on a device and use it; do not ship a build you only debugged
- [ ] Firebase: `google-services.json` present and git-ignored, or the module omitted
- [ ] No secrets in the repository: ad ids, API keys and keystores come from properties or CI
- [ ] Privacy policy and terms published, linked from settings
- [ ] Store listing, icons, screenshots — none of which the Base provides

---

## Report, do not patch

Before you finish, check that you changed **nothing** under `android-product-base/`:

```bash
cd ../android-product-base && git status --short     # must be empty
```

Anything you had to work around goes in your report, classified:

| | |
|---|---|
| **A** | Product-specific — you built it, fine |
| **B** | Missing/wrong documentation |
| **C** | Missing broadly reusable Base capability |
| **D** | Base API or design defect |
| **E** | Specialised extension candidate (a new optional module) |

For **C**, **D** and **E**, include: what you were building, the API you tried, what happened, and
the smallest change that would have worked.
