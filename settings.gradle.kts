pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "android-product-base"

// The demo shell. Verification harness only — never a product. See ARCHITECTURE_PLAN.md §3.1.
include(":demo")

// Capability modules. Each one exists to make a dependency optional; a product
// includes only what it needs. Phase 3+ modules are added here as they are built.
include(":core:common")
include(":core:designsystem")
include(":core:datastore")
include(":core:database")
include(":core:network")
include(":core:telemetry")
include(":core:telemetry-firebase")
include(":core:notification")
include(":core:ads")
include(":core:billing")
include(":core:review")
// Test-only: shared fakes and rules, consumed with testImplementation.
include(":core:testing")
include(":feature:onboarding")
include(":feature:settings")
include(":feature:premium")
