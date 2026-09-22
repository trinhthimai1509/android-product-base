# Android Product Base

A local, reusable foundation for consumer Android apps: Compose design system, preferences,
database and HTTP plumbing, telemetry contracts, ads with consent, Play Billing, in-app review,
notifications, onboarding, settings and a paywall — **fifteen modules, each of which a product can
leave out.**

**Status: `0.2.0-alpha01`, frozen for consumer validation.** The implementation is stable; the next
work is building a real product against it and reporting what hurts.

This repository is not published anywhere. It is consumed locally, from a sibling checkout.

```
workspace/
├── android-product-base/      ← this repository, read-only infrastructure
└── consumer-app/              ← your product
```

---

## Start here

| If you want to… | Read |
|---|---|
| Get a running app as fast as possible | **[QUICK_START.md](QUICK_START.md)** |
| Know what each module does, costs and requires | [MODULE_CATALOG.md](MODULE_CATALOG.md) |
| Compose capabilities into a real product | [PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md) |
| Ship ads, purchases or a paywall | [MONETIZATION.md](MONETIZATION.md) |
| Brand the app | [THEMING.md](THEMING.md) |
| Test product code built on the Base | [TESTING.md](TESTING.md) |
| Follow a checklist end to end | [NEW_APP_CHECKLIST.md](NEW_APP_CHECKLIST.md) |
| Understand *why* it is built this way | [ARCHITECTURE_PLAN.md](ARCHITECTURE_PLAN.md) |

## The rule

**Consumer products must not modify Product Base source code.** Friction gets classified and
reported, not patched locally — see
[the classification table](PRODUCT_INTEGRATION.md#consumer-rules).

## What is in the box

| | |
|---|---|
| `core:common` · `core:designsystem` · `core:datastore` | Foundation, theme, preferences |
| `core:database` · `core:network` | Room converters; OkHttp/Retrofit + connectivity |
| `core:telemetry` · `core:telemetry-firebase` | Contracts, and the only module that sees Firebase |
| `core:ads` · `core:billing` · `core:review` | AdMob + UMP, Play Billing, Play In-App Review |
| `core:notification` · `core:testing` | Channels and reminders; shared test fakes |
| `feature:onboarding` · `feature:settings` · `feature:premium` | Reusable product shell |
| `demo` | Verification shell — a capability catalog, never a product |

## Toolchain

Gradle 9.7 · AGP 9.1.1 · Kotlin 2.3.21 · KSP 2.3.11 · Hilt 2.60.1 · Compose BOM 2026.08.00 ·
compileSdk 37 · minSdk 26 · **Gradle daemon on JDK 21**.

Products set none of these: the version catalog and the `productbase.android.*` convention plugins
are shared with the consumer build.

## Verifying the Base itself

```bash
./gradlew spotlessCheck lint test assembleDebug assembleRelease
./gradlew :demo:installDebug        # the capability catalog, on a device
```
