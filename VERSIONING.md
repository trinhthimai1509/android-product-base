# Versioning

## Current version

**`v0.2.0-alpha01`** — the first trustworthy Git snapshot of this codebase. The implementation
existed and was consumed locally before Git provenance was established; this tag is where
history starts, not a claim about when the code was written. No earlier tag or commit is
claimed to exist.

## Scheme

Not Semantic Versioning in the strict sense (no public API stability guarantee is implied by a
`0.x` version, and there is no compatibility contract between alpha releases). The scheme is:

```
<minor-line>.<patch>-<pre-release>
```

Examples of what would come next, in order:

```
0.2.0-alpha01   ← current
0.2.0-alpha02   fixes/additions on the same module set, still pre-release
0.2.0           first non-alpha snapshot of the 0.2 module set
0.3.0-alpha01   next module set with new/changed public surface
```

A `0.x` version may change its public API between tags without a major version bump — that is
what "alpha" signals. Once a version drops the pre-release suffix (plain `0.2.0`), it is meant to
be stable enough for a consumer to build a release against, but still not API-frozen the way a
`1.0.0` would imply.

## When to create a new version

- A module's public API changes in a way that could break a consumer silently (signature change,
  removed function, changed default behaviour).
- A new module is added that consumers should be able to pin to.
- A defect fix consumers need, without dragging in unrelated in-flight work.

Not every commit needs a tag. Day-to-day Base development happens on `main`; a tag is a
deliberate checkpoint a consumer can build against.

## How consumers pin a version

A consumer's CI checks out the Base repository at a specific tag (`ref: v0.2.0-alpha01`), never
at `main`. This repository does not publish artifacts to a registry — consumption is
source-checkout based (see [QUICK_START.md](QUICK_START.md) and each consumer's own CI docs), so
the tag is the only thing standing between a consumer's build and an unreviewed change landing on
`main`.

**Do not point consumer CI at `main` HEAD.** `main` can change at any time for reasons that have
nothing to do with any given consumer; a consumer tracking it would get an unreviewed, unpinned
dependency with no way to know what changed or when.

## How breaking changes are communicated

There is no changelog automation yet. Until one exists:

- A tag that changes public API in a way that breaks an existing consumer usage should say so in
  the annotated tag message.
- If you are about to make such a change, check known consumers (see README's **Consumers**
  section) for the usage you are about to break before tagging.

## Upgrade checklist (for a consumer moving to a newer Base tag)

1. Read what changed between the consumer's current pin and the target tag (tag messages, and if
   needed `git log <old-tag>..<new-tag>` against this repository).
2. Update the Base locally and run its own verification (`./gradlew spotlessCheck lint test
   assembleDebug assembleRelease` — see README's **Verifying the Base itself**).
3. Update the consumer's pinned ref (e.g. the tag used in its GitHub Actions Base-checkout step,
   or the local checkout if developing by hand).
4. Run the consumer's own full verification (`:app:test`, `:app:lint`, `:app:assembleDebug` —
   Lucky Wheel's exact commands are in its own `docs/CI_CD.md`).
5. Fix anything the upgrade broke in the **consumer** — never by patching the Base locally (see
   [PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md#consumer-rules)). If the break points at a Base
   defect, classify and report it (class **D**) instead of working around it in the consumer.
6. Commit the consumer's pin change on its own, with a message that names the old and new Base
   version, so the dependency bump is visible in the consumer's own history.

## What this explicitly does not do

No consumer, anywhere, should be configured to silently track `main`. If a consumer's build ever
resolves the Base without a pinned ref, that is a misconfiguration to fix, not a convenience to
keep.
