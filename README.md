# Android Product Base

A local, reusable foundation for consumer Android apps: Compose design system, preferences,
database and HTTP plumbing, telemetry contracts, ads with consent, Play Billing, in-app review,
notifications, onboarding, settings and a paywall — **fifteen modules, each of which a product can
leave out.**

**Status: `0.2.0-alpha01`, frozen for consumer validation.** The implementation is stable; the next
work is building a real product against it and reporting what hurts.

This is a **reusable foundation/library, not a standalone end-user application.** It is not
published to a registry; it is consumed by separate Android product repositories, from a sibling
checkout pinned to a Git tag (never `main` — see **Versioning** below). It is versioned
independently from every product built on it.

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
| Understand version pinning and upgrades | [VERSIONING.md](VERSIONING.md) |
| Work on the Base itself | [DEVELOPMENT.md](DEVELOPMENT.md) |
| Wire up CI, here or in a consumer | [CI_CD.md](CI_CD.md) |
| Brief a coding agent working in this repo | [AGENTS.md](AGENTS.md) |

## Goals

- Reduce repeated Android project setup across products.
- Provide proven, reusable infrastructure (design system, persistence, telemetry contracts,
  ads/billing/review integration, notifications, onboarding, settings) instead of rebuilding it
  per app.
- Keep product-specific behaviour in consumer apps — this repository stops at the mechanism.
- Make new Android products faster to bootstrap (see [NEW_APP_CHECKLIST.md](NEW_APP_CHECKLIST.md)).
- Maintain a clean, enforced boundary between reusable infrastructure and product logic.
- Support independent Base evolution and versioning without forcing every consumer to move in
  lockstep.

## Non-goals

- Product-specific business logic, UI, or content — that belongs in the consumer.
- App-specific backend contracts.
- Forcing every module into every consumer — modules are opt-in per product
  (see [MODULE_CATALOG.md](MODULE_CATALOG.md)).
- Automatically upgrading consumers to the latest Base version. Consumers pin a version and
  upgrade deliberately (see **Versioning**).

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

This repository's own GitHub Actions workflow runs the first line on every PR and push to `main`
— see [CI_CD.md](CI_CD.md).

## Consumer model

**Product Base = reusable infrastructure. Consumer app = product/business behaviour.** A consumer
selects the modules it needs, wires them together in its own app module, and owns everything
product-specific — domain models, screens, navigation, copy, business rules. It never modifies
Base source; see **The rule** above and [PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md).

For example, **Lucky Wheel** consumes `core:common`, `core:designsystem`, `core:datastore`,
`core:database`, `core:telemetry`, `core:review`, `core:testing`, `feature:onboarding`, and
`feature:settings` — and owns all of its wheel/spin/draw-session domain logic itself.

## Integration

A consumer includes selected Base modules via Gradle composite build: `pluginManagement {
includeBuild(...) }` for `build-logic`'s convention plugins, and `project(path).projectDir =
File(...)` per module to map Base source directly into the consumer's build — no publishing, no
binary artifact. The consumer supplies the Base checkout's path; see
[QUICK_START.md](QUICK_START.md) for the full mechanism and the version-catalog pitfall to avoid.

Consumers commonly make that path configurable so CI can point it somewhere other than a
developer's fixed local checkout (Lucky Wheel does this via a `LUCKY_WHEEL_BASE_DIR` environment
variable, falling back to its developer's local path when unset) — see that consumer's own
integration code for the exact pattern; it's a consumer-side concern, not something this
repository prescribes.

## Versioning

Consumers pin a known Base version/tag — currently **`v0.2.0-alpha01`** — and upgrade
deliberately: update Base, test it, tag a new version, update the consumer's pin, run the
consumer's CI, then release the consumer when appropriate. **Do not consume `main` HEAD in CI.**
Full policy: [VERSIONING.md](VERSIONING.md).

## Development

See [DEVELOPMENT.md](DEVELOPMENT.md) for environment requirements, repository structure,
verification commands, and the rules that apply before committing or tagging.

## CI

This repository validates itself on push/PR; consumers check it out at a pinned tag in their own
CI. Full detail, including private-repository authentication: [CI_CD.md](CI_CD.md).

## Documentation

| Document | Covers |
|---|---|
| [QUICK_START.md](QUICK_START.md) | Fastest path to a running consumer app |
| [MODULE_CATALOG.md](MODULE_CATALOG.md) | Every module's responsibility, cost, requirements |
| [PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md) | Composing capabilities into a real product; the A–E friction classification |
| [ARCHITECTURE_PLAN.md](ARCHITECTURE_PLAN.md) | Why the Base is structured this way |
| [THEMING.md](THEMING.md) | Branding a consumer app |
| [MONETIZATION.md](MONETIZATION.md) | Ads, Billing, and paywall integration |
| [TESTING.md](TESTING.md) | Test utilities and recommended consumer test patterns |
| [NEW_APP_CHECKLIST.md](NEW_APP_CHECKLIST.md) | End-to-end checklist for a new consumer |
| [VERSIONING.md](VERSIONING.md) | Version scheme, pinning, upgrade procedure |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Working on the Base itself |
| [CI_CD.md](CI_CD.md) | This repo's CI, and how consumers check it out |
| [AGENTS.md](AGENTS.md) | Instructions for coding agents working in this repository |

## Consumers

| Consumer | Package | Base version |
|---|---|---|
| Lucky Wheel | `dev.sautao.luckywheel` | `v0.2.0-alpha01` |

## Stability

`0.x` / alpha: the public API may evolve between tags without a major-version-style guarantee.
Consumers should pin a specific version and upgrade intentionally — see **Versioning** — rather
than assume compatibility across tags.

## License

Private/internal repository — no public license currently declared.
