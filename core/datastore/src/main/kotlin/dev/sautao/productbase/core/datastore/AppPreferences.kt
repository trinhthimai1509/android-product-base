package dev.sautao.productbase.core.datastore

import dev.sautao.productbase.core.common.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Application-level user preferences.
 *
 * This is the single owner of preference keys, so two features cannot collide on one. Product
 * preferences that are specific to a product's domain belong in that product, not here.
 *
 * Never store secrets: a Preferences DataStore file is plain, unencrypted protobuf.
 */
interface AppPreferences {
    val themeMode: Flow<ThemeMode>

    val dynamicColorEnabled: Flow<Boolean>

    /**
     * Whether the user has been through onboarding, however it ended — finished or skipped.
     *
     * Skipping is a completed onboarding: a user who declined the tour has told you they do not
     * want it, and showing it again on the next launch ignores that.
     */
    val onboardingCompleted: Flow<Boolean>

    /**
     * Whether the user wants the app's own notifications.
     *
     * This is the *app's* switch, not the system permission — the two are independent, and a
     * product must respect both. What it actually gates is the product's decision; the base only
     * stores it. Defaults to true so a product that never shows the row behaves as if it had one
     * that was on.
     */
    val notificationsEnabled: Flow<Boolean>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setDynamicColorEnabled(enabled: Boolean)

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setNotificationsEnabled(enabled: Boolean)
}
