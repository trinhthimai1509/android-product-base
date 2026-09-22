# Agent instructions

For Claude, ChatGPT, or any other coding agent working in this repository.

## What this repository is

Android Product Base is **reusable infrastructure**, not a consumer product. It is consumed by
separate product repositories (currently: Lucky Wheel, `dev.sautao.luckywheel`) via a pinned Git
tag — see [VERSIONING.md](VERSIONING.md). Nothing in this repository is a shippable end-user app
except `demo/`, which exists only to exercise the other modules and is never itself a product.

## Before changing code

1. Read [README.md](README.md) and whichever of [ARCHITECTURE_PLAN.md](ARCHITECTURE_PLAN.md),
   [MODULE_CATALOG.md](MODULE_CATALOG.md), or [PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md) is
   relevant to the module you're touching.
2. Check [MODULE_CATALOG.md](MODULE_CATALOG.md) for the module's stated responsibility and public
   surface before adding to it — a change that doesn't fit the module's stated responsibility
   probably belongs in a consumer, or in a new module, not bolted on here.

## Rules

1. **Do not add product-specific business logic.** Domain models, product copy, product-specific
   screens, and business rules belong in a consumer app, never here.
2. **Do not modify public APIs casually.** A signature change, removed function, or changed
   default here can silently break every consumer pinned to a tag that includes it. Search for
   consumers/usages first (this repo has none locally to grep — check the known-consumers list in
   README, or ask before assuming a change is safe).
3. **Add or update tests for behavior changes.** See [TESTING.md](TESTING.md) for what's already
   provided in `core:testing` and the recommended product test patterns.
4. **Keep documentation synchronized with architecture changes.** If a change makes
   [ARCHITECTURE_PLAN.md](ARCHITECTURE_PLAN.md) or [MODULE_CATALOG.md](MODULE_CATALOG.md) wrong,
   update them in the same change — don't leave docs describing a prior state.
5. **Do not commit credentials or local machine paths.** No keystore, no API key, no
   `local.properties`, no absolute path specific to one machine.
6. **Do not edit generated build output.** Anything under `build/`, `.gradle/`, or `.kotlin/` is
   disposable; if you find yourself editing something there, you're editing the wrong file.
7. **Version/tag changes must be intentional.** Don't create or move a tag as a side effect of
   something else — see [VERSIONING.md](VERSIONING.md) for when a new version is warranted and
   the rules before tagging in [DEVELOPMENT.md](DEVELOPMENT.md).
8. **Consumers pin versions; `main` is not automatically anyone's dependency.** Don't write
   consumer-facing instructions or CI examples that track `main` HEAD.

## Classify friction, don't patch around it

If you're working *from* a consumer and hit something the Base doesn't support well, classify it
before doing anything:

| Class | Meaning | What to do |
|---|---|---|
| **A** | Product-specific requirement | Build it in the consumer. Not a Base concern. |
| **B** | Missing/wrong documentation | The capability exists but isn't documented correctly. Fix the doc. |
| **C** | Missing broadly reusable capability | Several products would need it. Report it, don't add it unreviewed. |
| **D** | Base API or design defect | The seam exists but is wrong, unusable, or unsafe. Report it with the failing usage. |
| **E** | Specialised extension candidate | Real but narrow — a candidate for a new optional module, not a change to an existing one. Report it. |

Full detail and the reasoning behind this table: [PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md#consumer-rules).
**A is the common case** — most things an agent is asked to do "in the Base" actually belong in
the consumer instead.

## Scope note

This file describes how to work on this repository. It intentionally contains no conversation
history, no task-specific context from any consumer's development, and no personal information —
keep it that way when editing it.
