package dev.sautao.productbase.demo.ui.settings

import dev.sautao.productbase.core.billing.PurchaseState
import dev.sautao.productbase.core.common.ThemeMode
import dev.sautao.productbase.feature.settings.SettingsRow
import dev.sautao.productbase.feature.settings.SettingsSection

/**
 * Which capabilities this product has, as the settings screen sees them.
 *
 * The nullable fields are the whole idea. `null` is how a product says "this app does not include
 * that module" — and the row simply never gets built. The demo includes everything, so it always
 * passes real values; the tests pass `null` to show exactly what a product without billing, ads
 * or notifications renders, which is the same screen minus those rows.
 */
internal data class DemoSettingsCapabilities(
    val themeMode: ThemeMode,
    val dynamicColorEnabled: Boolean,
    /** `null` when the product does not include `core:notification`. */
    val notificationsEnabled: Boolean?,
    /** `null` when the product does not include `core:ads`; false when UMP does not require it. */
    val privacyOptionsRequired: Boolean?,
    /** `null` when the product does not include `core:billing`. */
    val purchaseState: PurchaseState?,
    val versionName: String,
)

/** Already-localised text, resolved from resources by the screen. */
internal data class DemoSettingsLabels(
    val appearance: String,
    val notifications: String,
    val reminders: String,
    val remindersSubtitle: String,
    val systemNotifications: String,
    val systemNotificationsSubtitle: String,
    val privacy: String,
    val privacyOptions: String,
    val privacyOptionsSubtitle: String,
    val privacyPolicy: String,
    val terms: String,
    val support: String,
    val premiumOffer: String,
    val premiumOfferSubtitle: String,
    val premiumOwned: String,
    val premiumOwnedValue: String,
    val restore: String,
    val rate: String,
    val share: String,
    val about: String,
    val version: String,
    val replayOnboarding: String,
    val replayOnboardingSubtitle: String,
)

internal data class DemoSettingsActions(
    val onThemeModeChange: (ThemeMode) -> Unit,
    val onDynamicColorChange: (Boolean) -> Unit,
    val onNotificationsChange: (Boolean) -> Unit,
    val onOpenSystemNotifications: () -> Unit,
    val onShowPrivacyOptions: () -> Unit,
    val onOpenPrivacyPolicy: () -> Unit,
    val onOpenTerms: () -> Unit,
    val onOpenPremium: () -> Unit,
    val onRestorePurchase: () -> Unit,
    val onRateApp: () -> Unit,
    val onShareApp: () -> Unit,
    val onReplayOnboarding: () -> Unit,
)

/**
 * The demo's settings screen, as data.
 *
 * A plain function rather than a Composable so the composition rules — which row appears when —
 * are testable without a device, a Compose runtime or a screenshot. This is the product's job:
 * `feature:settings` renders whatever list it is handed and decides nothing.
 */
internal fun demoSettingsSections(
    capabilities: DemoSettingsCapabilities,
    labels: DemoSettingsLabels,
    actions: DemoSettingsActions,
): List<SettingsSection> = listOf(
    SettingsSection(
        title = labels.appearance,
        rows = listOf(
            SettingsRow.ThemePicker(
                themeMode = capabilities.themeMode,
                onThemeModeChange = actions.onThemeModeChange,
            ),
            SettingsRow.DynamicColor(
                enabled = capabilities.dynamicColorEnabled,
                onEnabledChange = actions.onDynamicColorChange,
            ),
        ),
    ),
    SettingsSection(
        title = labels.notifications,
        rows = buildList {
            // Present only because this product includes core:notification.
            capabilities.notificationsEnabled?.let { enabled ->
                add(
                    SettingsRow.Toggle(
                        title = labels.reminders,
                        subtitle = labels.remindersSubtitle,
                        checked = enabled,
                        onCheckedChange = actions.onNotificationsChange,
                    ),
                )
                add(
                    SettingsRow.Action(
                        title = labels.systemNotifications,
                        subtitle = labels.systemNotificationsSubtitle,
                        onClick = actions.onOpenSystemNotifications,
                    ),
                )
            }
        },
    ),
    SettingsSection(
        title = labels.privacy,
        rows = buildList {
            // Two conditions, not one: the product has to include ads at all, *and* UMP has to
            // say this user's region requires an ongoing way to change their choice.
            if (capabilities.privacyOptionsRequired == true) {
                add(
                    SettingsRow.Action(
                        title = labels.privacyOptions,
                        subtitle = labels.privacyOptionsSubtitle,
                        onClick = actions.onShowPrivacyOptions,
                    ),
                )
            }
            add(SettingsRow.Action(title = labels.privacyPolicy, onClick = actions.onOpenPrivacyPolicy))
            add(SettingsRow.Action(title = labels.terms, onClick = actions.onOpenTerms))
        },
    ),
    SettingsSection(
        title = labels.support,
        rows = buildList {
            when (capabilities.purchaseState) {
                // No billing in this product: no upsell, no restore, nothing to hide.
                null -> Unit

                // Owning it already turns the row from an offer into a statement of fact, and
                // takes the restore row away — there is nothing left to restore.
                PurchaseState.Purchased -> add(
                    SettingsRow.Info(title = labels.premiumOwned, value = labels.premiumOwnedValue),
                )

                else -> {
                    add(
                        SettingsRow.Action(
                            title = labels.premiumOffer,
                            subtitle = labels.premiumOfferSubtitle,
                            onClick = actions.onOpenPremium,
                        ),
                    )
                    add(SettingsRow.Action(title = labels.restore, onClick = actions.onRestorePurchase))
                }
            }
            add(SettingsRow.Action(title = labels.rate, onClick = actions.onRateApp))
            add(SettingsRow.Action(title = labels.share, onClick = actions.onShareApp))
        },
    ),
    SettingsSection(
        title = labels.about,
        rows = listOf(
            SettingsRow.Info(title = labels.version, value = capabilities.versionName),
            SettingsRow.Action(
                title = labels.replayOnboarding,
                subtitle = labels.replayOnboardingSubtitle,
                onClick = actions.onReplayOnboarding,
            ),
        ),
    ),
)
