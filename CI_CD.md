# CI/CD

Two separate concerns: validating this repository on its own, and how a consumer's CI obtains it.
They are documented separately on purpose — this repository's CI has no idea any consumer exists.

## This repository's own CI

`.github/workflows/ci.yml` runs on every pull request and push to `main`:

1. checkout
2. JDK 21 (Temurin) + Gradle setup, with dependency caching (`gradle/actions/setup-gradle`)
3. `./gradlew spotlessCheck lint test assembleDebug assembleRelease` — the exact commands in
   README's **Verifying the Base itself**
4. lint reports uploaded as a build artifact

`:demo:installDebug` is not run in CI — it needs a connected device and is a manual verification
step (see [DEVELOPMENT.md](DEVELOPMENT.md)), not something CI can do.

No release process, no signing, no publishing exists for this repository. It produces no artifact
a consumer downloads — consumers consume it by source checkout at a pinned tag, not by pulling a
build output from here.

## How a consumer's CI obtains this repository

This repository is currently **private**. A consumer's GitHub Actions workflow needs to check out
two repositories in the same job: itself, and this one, at a pinned tag (never `main` — see
[VERSIONING.md](VERSIONING.md)).

Conceptually:

```
workspace/
├── <consumer-repo>/            actions/checkout, default (this run's ref)
└── android-product-base/        actions/checkout, repository: trinhthimai1509/android-product-base,
                                  ref: <pinned tag, e.g. v0.2.0-alpha01>
```

Then the consumer's build points its Base-directory variable (e.g. Lucky Wheel's
`LUCKY_WHEEL_BASE_DIR`) at wherever the second checkout landed.

### Private-repository authentication

`actions/checkout` on a private repository needs a token with read access to it. The default
`GITHUB_TOKEN` a workflow run gets is scoped to the repository the workflow lives in — it does
**not** automatically get access to a second private repository, even under the same account,
unless that's been explicitly granted. A consumer's workflow needs one of:

1. **A dedicated read-only deploy key** for this repository (simplest for a single consumer;
   generated per-repository, scoped to read-only, added as a deploy key here and the matching
   private key as a secret in the consumer).
2. **A fine-grained personal access token** scoped to read-only **Contents** access on this
   repository specifically (not a classic PAT with broad `repo` scope — that would over-grant).
3. **A GitHub App installation token**, if a GitHub App with access to both repositories already
   exists for the account.

Which one a given consumer actually uses, and the exact secret name, is documented in that
consumer's own CI docs (e.g. Lucky Wheel's `docs/CI_CD.md`) — this file only states the concept
and the constraint, since this repository has no opinion on any particular consumer's secret
naming.

No token, key, or credential value is ever stored in this repository or in this file.

## How future apps should consume this Base

Same mechanism as any other consumer: pin a tag, check it out alongside the app's own checkout in
CI, point the app's Base-directory variable at it. See [QUICK_START.md](QUICK_START.md) for the
local Gradle wiring and [VERSIONING.md](VERSIONING.md) for the pinning discipline. There is
nothing consumer-specific in this repository to change to onboard a new app — a new consumer adds
a checkout step and a directory-variable line in *its own* workflow, not here.
