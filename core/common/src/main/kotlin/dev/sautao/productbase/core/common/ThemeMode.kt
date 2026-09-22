package dev.sautao.productbase.core.common

/**
 * The user's theme preference.
 *
 * Lives here rather than in `core:designsystem` because `core:datastore` persists it and
 * `core:designsystem` applies it, and those two modules must not depend on each other.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}
