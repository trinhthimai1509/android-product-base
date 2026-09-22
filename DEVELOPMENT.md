# Development

Working on the Base itself — not on a product built with it. If you're building a product, start
at [QUICK_START.md](QUICK_START.md) instead.

## Environment

| | |
|---|---|
| JDK | 21 for the Gradle daemon (`gradle/gradle-daemon-jvm.properties` pins this — the build fails before compiling anything without it) |
| Gradle | 9.7 via the wrapper (`./gradlew`) — never invoke a system-installed Gradle |
| AGP | 9.1.1 |
| Kotlin | 2.3.21 |
| compileSdk / minSdk | 37 / 26 |
| Android SDK | installed locally, referenced by your own `local.properties` → `sdk.dir` (never committed — see `.gitignore`) |

All of the above besides the SDK path live in `gradle/libs.versions.toml` and
`gradle/gradle-daemon-jvm.properties`, which are the source of truth — if this table and those
files disagree, the files win.

## Repository structure

```
android-product-base/
├── build-logic/        convention plugins (productbase.android.*) — an included build
├── core/                foundation modules — see MODULE_CATALOG.md
├── feature/              reusable feature-shell modules — see MODULE_CATALOG.md
├── demo/                 capability catalog app — verification shell, never a product
├── gradle/               wrapper + version catalog
└── *.md                  documentation (this file, README, module/architecture/integration docs)
```

## Verification commands

```bash
./gradlew spotlessCheck lint test assembleDebug assembleRelease
./gradlew :demo:installDebug        # capability catalog, on a connected device — manual check
```

These are the exact commands documented in README's **Verifying the Base itself** and what
`.github/workflows/ci.yml` runs on push/PR (minus `:demo:installDebug`, which needs a device and
is a manual step, not a CI one).

There is no dedicated Base-only Room migration test target beyond what `test` already runs; module
owners add tests under each module's own `src/test` / `src/androidTest`.

## Rules before committing

- Do not add product-specific behaviour, copy, or business logic — see
  [PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md) for the boundary and the A–E friction
  classification.
- Do not commit generated build output (`build/`, `.gradle/`, `.kotlin/`) — `.gitignore` already
  excludes these; if `git status` shows one, something is misconfigured, not something to force-add.
- Do not commit `local.properties`, a keystore, or any consumer's credentials — none of that
  belongs in this repository at all, from any consumer.
- Run the verification commands above before pushing. A failing `spotlessCheck` or `lint` is not
  something to suppress; fix the formatting/finding or, if the rule itself is wrong, fix the rule
  deliberately and say so in the commit.
- If a change alters public API surface, check known consumers (README's **Consumers** section)
  for usages you're about to break, and consider whether it needs a new version tag (see
  [VERSIONING.md](VERSIONING.md)) rather than silently landing on `main`.

## Rules before tagging

- A tag is a promise a consumer's CI will build against exactly this source, forever (tags are not
  moved once pushed — see [VERSIONING.md](VERSIONING.md)).
- Run the full verification command list above and confirm it passes before tagging, not after.
- Confirm the working tree is clean and the tag points at the commit you intend, not whatever
  happens to be checked out.
- Write an annotated tag (`git tag -a`, not a lightweight tag) with a message describing what's in
  it, especially anything that could break an existing consumer.

## What Base source should never contain

- Product-specific behaviour, screens, copy, or business logic (that's a consumer's job)
- Any consumer's credentials, signing keys, or API keys
- Local machine configuration (absolute paths, `local.properties`, IDE workspace state)
- Build output of any kind
