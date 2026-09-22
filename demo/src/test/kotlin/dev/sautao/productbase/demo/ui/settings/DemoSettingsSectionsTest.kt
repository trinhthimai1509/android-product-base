package dev.sautao.productbase.demo.ui.settings

import dev.sautao.productbase.core.billing.PurchaseState
import dev.sautao.productbase.core.common.ThemeMode
import dev.sautao.productbase.feature.settings.SettingsRow
import dev.sautao.productbase.feature.settings.SettingsSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The composition model under test: which rows a product gets for the capabilities it includes.
 *
 * No Android, no Compose and no SDK is faked here — the rules are data, so they are tested as
 * data.
 */
class DemoSettingsSectionsTest {
    @Test
    fun `an app with every capability gets every row`() {
        val sections = sections()

        assertTrue(sections.hasRow(LABELS.reminders))
        assertTrue(sections.hasRow(LABELS.premiumOffer))
        assertTrue(sections.hasRow(LABELS.restore))
        assertTrue(sections.hasRow(LABELS.rate))
        assertTrue(sections.hasRow(LABELS.share))
        assertTrue(sections.hasRow(LABELS.version))
    }

    @Test
    fun `an app without billing shows no premium and no restore`() {
        val sections = sections(purchaseState = null)

        assertFalse(sections.hasRow(LABELS.premiumOffer))
        assertFalse(sections.hasRow(LABELS.restore))
        assertFalse(sections.hasRow(LABELS.premiumOwned))
        // The section itself survives, because rate and share have nothing to do with billing.
        assertTrue(sections.hasRow(LABELS.rate))
    }

    @Test
    fun `an app without notifications shows no notification rows and no empty heading`() {
        val sections = sections(notificationsEnabled = null)

        assertFalse(sections.hasRow(LABELS.reminders))
        assertFalse(sections.hasRow(LABELS.systemNotifications))
        // The section is left empty rather than conditionally built, because feature:settings
        // drops empty sections when it renders — see SettingsEntryTest.
        assertTrue(sections.single { it.title == LABELS.notifications }.rows.isEmpty())
    }

    @Test
    fun `an app without ads shows no privacy options row`() {
        val sections = sections(privacyOptionsRequired = null)

        assertFalse(sections.hasRow(LABELS.privacyOptions))
        // Policy and terms are the product's own links and stay.
        assertTrue(sections.hasRow(LABELS.privacyPolicy))
        assertTrue(sections.hasRow(LABELS.terms))
    }

    @Test
    fun `privacy options appear only when consent rules require them`() {
        assertFalse(sections(privacyOptionsRequired = false).hasRow(LABELS.privacyOptions))
        assertTrue(sections(privacyOptionsRequired = true).hasRow(LABELS.privacyOptions))
    }

    @Test
    fun `owning the product replaces the offer with a statement and removes restore`() {
        val sections = sections(purchaseState = PurchaseState.Purchased)

        assertFalse(sections.hasRow(LABELS.premiumOffer))
        assertFalse(sections.hasRow(LABELS.restore))
        assertTrue(sections.hasRow(LABELS.premiumOwned))
    }

    @Test
    fun `unresolved ownership still offers the paywall`() {
        // Unknown means Play has not answered. Hiding the upgrade path would be worse than
        // offering it: the paywall itself handles an unknown state.
        val sections = sections(purchaseState = PurchaseState.Unknown)

        assertTrue(sections.hasRow(LABELS.premiumOffer))
        assertTrue(sections.hasRow(LABELS.restore))
    }

    @Test
    fun `a pending payment keeps the restore row available`() {
        val sections = sections(purchaseState = PurchaseState.Pending)

        assertTrue(sections.hasRow(LABELS.restore))
        assertFalse(sections.hasRow(LABELS.premiumOwned))
    }

    @Test
    fun `an offline free app keeps only the sections it can fill`() {
        val sections = sections(
            notificationsEnabled = null,
            privacyOptionsRequired = null,
            purchaseState = null,
        ).filter { it.rows.isNotEmpty() }

        assertEquals(
            listOf(LABELS.appearance, LABELS.privacy, LABELS.support, LABELS.about),
            sections.map { it.title },
        )
    }

    @Test
    fun `the theme picker reports the user's choice to the product`() {
        var chosen: ThemeMode? = null
        val sections = sections(onThemeModeChange = { chosen = it })

        sections.rows().filterIsInstance<SettingsRow.ThemePicker>().single().onThemeModeChange(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, chosen)
    }

    @Test
    fun `the version row shows what the build says`() {
        val sections = sections(versionName = "9.9.9")

        val row = sections.rows().filterIsInstance<SettingsRow.Info>().single { it.title == LABELS.version }
        assertEquals("9.9.9", row.value)
    }

    @Test
    fun `the notification toggle reports the new value`() {
        var value: Boolean? = null
        val sections = sections(onNotificationsChange = { value = it })

        sections.rows().filterIsInstance<SettingsRow.Toggle>().single().onCheckedChange(false)

        assertEquals(false, value)
    }

    private fun sections(
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        dynamicColorEnabled: Boolean = true,
        notificationsEnabled: Boolean? = true,
        privacyOptionsRequired: Boolean? = true,
        purchaseState: PurchaseState? = PurchaseState.NotPurchased,
        versionName: String = "0.1.0",
        onThemeModeChange: (ThemeMode) -> Unit = {},
        onNotificationsChange: (Boolean) -> Unit = {},
    ): List<SettingsSection> = demoSettingsSections(
        capabilities = DemoSettingsCapabilities(
            themeMode = themeMode,
            dynamicColorEnabled = dynamicColorEnabled,
            notificationsEnabled = notificationsEnabled,
            privacyOptionsRequired = privacyOptionsRequired,
            purchaseState = purchaseState,
            versionName = versionName,
        ),
        labels = LABELS,
        actions = actions(
            onThemeModeChange = onThemeModeChange,
            onNotificationsChange = onNotificationsChange,
        ),
    )

    private fun actions(
        onThemeModeChange: (ThemeMode) -> Unit = {},
        onNotificationsChange: (Boolean) -> Unit = {},
    ) = DemoSettingsActions(
        onThemeModeChange = onThemeModeChange,
        onDynamicColorChange = {},
        onNotificationsChange = onNotificationsChange,
        onOpenSystemNotifications = {},
        onShowPrivacyOptions = {},
        onOpenPrivacyPolicy = {},
        onOpenTerms = {},
        onOpenPremium = {},
        onRestorePurchase = {},
        onRateApp = {},
        onShareApp = {},
        onReplayOnboarding = {},
    )

    private fun List<SettingsSection>.rows(): List<SettingsRow> = flatMap { it.rows }

    private fun List<SettingsSection>.hasRow(title: String): Boolean = rows().any {
        when (it) {
            is SettingsRow.Action -> it.title == title
            is SettingsRow.Toggle -> it.title == title
            is SettingsRow.Info -> it.title == title
            is SettingsRow.ThemePicker, is SettingsRow.DynamicColor -> false
        }
    }

    private companion object {
        // Labels stand in for resolved resources; the values only have to be distinguishable.
        val LABELS = DemoSettingsLabels(
            appearance = "Appearance",
            notifications = "Notifications",
            reminders = "Reminders",
            remindersSubtitle = "",
            systemNotifications = "System notification settings",
            systemNotificationsSubtitle = "",
            privacy = "Privacy",
            privacyOptions = "Privacy options",
            privacyOptionsSubtitle = "",
            privacyPolicy = "Privacy policy",
            terms = "Terms of use",
            support = "Support",
            premiumOffer = "Remove ads",
            premiumOfferSubtitle = "",
            premiumOwned = "Full version",
            premiumOwnedValue = "Active",
            restore = "Restore purchase",
            rate = "Rate this app",
            share = "Share this app",
            about = "About",
            version = "Version",
            replayOnboarding = "Replay onboarding",
            replayOnboardingSubtitle = "",
        )
    }
}
