package dev.sautao.productbase.demo.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.sautao.productbase.core.common.ThemeMode
import dev.sautao.productbase.core.designsystem.component.AppDialog
import dev.sautao.productbase.demo.BuildConfig
import dev.sautao.productbase.demo.R
import dev.sautao.productbase.demo.ui.BackButton
import dev.sautao.productbase.feature.settings.SettingsScreen
import dev.sautao.productbase.feature.settings.openUrl
import dev.sautao.productbase.feature.settings.shareText

/**
 * The demo's settings screen: product composition, and nothing else.
 *
 * Every row is built here from capabilities this product happens to include, and handed to
 * `feature:settings` as data. Theme state arrives from the Activity's `AppViewModel`, billing and
 * ads state from [DemoSettingsViewModel] — two different sources feeding one screen, which is
 * what a composition model has to survive.
 */
@Composable
fun DemoSettingsScreen(
    uiState: DemoSettingsUiState,
    themeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onShowPrivacyOptions: () -> Unit,
    onOpenPremium: () -> Unit,
    onRestorePurchase: () -> Unit,
    onRateApp: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onActionUnhandled: () -> Unit,
    onDismissMessage: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Supplied by the product, as the base insists: no store listing, no policy URL and no
    // sentence about the app is shipped by any module below this one.
    val shareMessage = stringResource(R.string.settings_share_text, PLAY_STORE_URL)
    val shareChooserTitle = stringResource(R.string.settings_share_chooser)

    SettingsScreen(
        title = stringResource(R.string.settings_title),
        sections = demoSettingsSections(
            capabilities = DemoSettingsCapabilities(
                themeMode = themeMode,
                dynamicColorEnabled = dynamicColorEnabled,
                notificationsEnabled = uiState.notificationsEnabled,
                privacyOptionsRequired = uiState.privacyOptionsRequired,
                purchaseState = uiState.purchaseState,
                versionName = BuildConfig.VERSION_NAME,
            ),
            labels = settingsLabels(),
            actions = DemoSettingsActions(
                onThemeModeChange = onThemeModeChange,
                onDynamicColorChange = onDynamicColorChange,
                onNotificationsChange = onNotificationsChange,
                onOpenSystemNotifications = {
                    if (!context.openAppNotificationSettings()) onActionUnhandled()
                },
                onShowPrivacyOptions = onShowPrivacyOptions,
                onOpenPrivacyPolicy = {
                    if (!context.openUrl(PRIVACY_POLICY_URL)) onActionUnhandled()
                },
                onOpenTerms = {
                    if (!context.openUrl(TERMS_URL)) onActionUnhandled()
                },
                onOpenPremium = onOpenPremium,
                onRestorePurchase = onRestorePurchase,
                onRateApp = onRateApp,
                onShareApp = {
                    if (!context.shareText(shareMessage, shareChooserTitle)) onActionUnhandled()
                },
                onReplayOnboarding = onReplayOnboarding,
            ),
        ),
        modifier = modifier,
        navigationIcon = { BackButton(onNavigateBack) },
    )

    uiState.message?.let { message ->
        AppDialog(
            title = stringResource(R.string.settings_title),
            text = message.describe(),
            confirmText = stringResource(R.string.settings_dismiss),
            onConfirm = onDismissMessage,
            onDismiss = onDismissMessage,
        )
    }
}

@Composable
private fun DemoSettingsMessage.describe(): String = when (this) {
    DemoSettingsMessage.PurchaseRestored -> stringResource(R.string.settings_message_purchase_restored)

    DemoSettingsMessage.NothingToRestore -> stringResource(R.string.settings_message_nothing_to_restore)

    DemoSettingsMessage.CouldNotCheckPurchases ->
        stringResource(R.string.settings_message_could_not_check)

    DemoSettingsMessage.ReviewUnavailable -> stringResource(R.string.settings_message_review_unavailable)

    DemoSettingsMessage.NothingToHandleAction -> stringResource(R.string.settings_message_no_handler)
}

@Composable
private fun settingsLabels() = DemoSettingsLabels(
    appearance = stringResource(R.string.settings_section_appearance),
    notifications = stringResource(R.string.settings_section_notifications),
    reminders = stringResource(R.string.settings_reminders),
    remindersSubtitle = stringResource(R.string.settings_reminders_subtitle),
    systemNotifications = stringResource(R.string.settings_system_notifications),
    systemNotificationsSubtitle = stringResource(R.string.settings_system_notifications_subtitle),
    privacy = stringResource(R.string.settings_section_privacy),
    privacyOptions = stringResource(R.string.settings_privacy_options),
    privacyOptionsSubtitle = stringResource(R.string.settings_privacy_options_subtitle),
    privacyPolicy = stringResource(R.string.settings_privacy_policy),
    terms = stringResource(R.string.settings_terms),
    support = stringResource(R.string.settings_section_support),
    premiumOffer = stringResource(R.string.settings_premium),
    premiumOfferSubtitle = stringResource(R.string.settings_premium_subtitle),
    premiumOwned = stringResource(R.string.settings_premium_owned),
    premiumOwnedValue = stringResource(R.string.settings_premium_owned_value),
    restore = stringResource(R.string.settings_restore),
    rate = stringResource(R.string.settings_rate),
    share = stringResource(R.string.settings_share),
    about = stringResource(R.string.settings_section_about),
    version = stringResource(R.string.settings_version),
    replayOnboarding = stringResource(R.string.settings_replay_onboarding),
    replayOnboardingSubtitle = stringResource(R.string.settings_replay_onboarding_subtitle),
)

/**
 * Opens this app's notification settings.
 *
 * The system screen, not an imitation of it: the app-level switch, the channels and the
 * importance of each one all live there, and Android is the only thing allowed to change them.
 */
private fun Context.openAppNotificationSettings(): Boolean {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)

    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

// The product's own links. A real app points these at pages it actually publishes; the demo is
// not published anywhere, and says so rather than pretending.
private val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
private const val PRIVACY_POLICY_URL = "https://example.com/product-base-demo/privacy"
private const val TERMS_URL = "https://example.com/product-base-demo/terms"
