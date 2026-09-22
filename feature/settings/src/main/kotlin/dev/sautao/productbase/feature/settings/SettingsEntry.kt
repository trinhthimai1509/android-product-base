package dev.sautao.productbase.feature.settings

import dev.sautao.productbase.core.common.ThemeMode

/**
 * A titled group of rows.
 *
 * A section whose [rows] are empty is not rendered, so a product can build a section from
 * capabilities it may or may not have without checking whether anything survived.
 */
data class SettingsSection(val title: String? = null, val rows: List<SettingsRow>)

/**
 * One row of a settings screen.
 *
 * **This is how "only show what exists" is enforced: by construction.** A product assembles the
 * list it wants from the capabilities it actually includes. An app without billing never builds
 * a Premium row; an app without ads never builds a privacy-options row; an app without
 * notifications never builds a notifications row. There is no capability flag to set, no
 * registry to populate and nothing in this module that knows those capabilities exist.
 *
 * Every string here is already localised and resolved by the product — the same rule the design
 * system components follow. The one exception is [ThemePicker] and [DynamicColor], which are
 * mechanism this base owns and therefore label themselves.
 */
sealed interface SettingsRow {
    /**
     * Navigates somewhere or performs something: Premium, Restore purchase, Privacy policy,
     * Terms, Rate app, Share app, Privacy options, system notification settings.
     *
     * All of those are the same row. What distinguishes them is what the product passes in, which
     * is exactly why this module needs no vocabulary for any of them.
     */
    data class Action(val title: String, val onClick: () -> Unit, val subtitle: String? = null) : SettingsRow

    /** A switch the product persists however it likes. */
    data class Toggle(
        val title: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        val subtitle: String? = null,
        val enabled: Boolean = true,
    ) : SettingsRow

    /** Read-only: an app version, a build number, an account e-mail. */
    data class Info(val title: String, val value: String) : SettingsRow

    /**
     * Light / Dark / Follow system, labelled by this module.
     *
     * The base owns these three because it owns [ThemeMode] and `ProductBaseTheme`, and because
     * "Follow system" means the same thing in every app. Persisting the choice is the product's
     * job — usually one line to `AppPreferences` — which is what keeps this module free of a
     * storage dependency.
     */
    data class ThemePicker(val themeMode: ThemeMode, val onThemeModeChange: (ThemeMode) -> Unit) : SettingsRow

    /**
     * The Material You toggle.
     *
     * Rendered disabled, with an explanation, on devices older than Android 12 rather than
     * hidden: a control that vanishes on some phones generates support questions, and a switch
     * that says why it cannot be used answers them.
     *
     * A product that does not want dynamic colour simply never adds this row.
     */
    data class DynamicColor(val enabled: Boolean, val onEnabledChange: (Boolean) -> Unit) : SettingsRow
}

/** Sections with nothing in them are dropped, so capability-driven lists need no post-filtering. */
internal fun List<SettingsSection>.nonEmpty(): List<SettingsSection> = filter { it.rows.isNotEmpty() }
