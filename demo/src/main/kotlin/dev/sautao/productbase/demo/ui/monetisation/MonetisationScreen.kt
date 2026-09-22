package dev.sautao.productbase.demo.ui.monetisation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.sautao.productbase.core.ads.AdsController
import dev.sautao.productbase.core.ads.AppBannerAd
import dev.sautao.productbase.core.ads.InterstitialOutcome
import dev.sautao.productbase.core.ads.RewardedOutcome
import dev.sautao.productbase.core.common.PremiumStatus
import dev.sautao.productbase.core.designsystem.component.AppCard
import dev.sautao.productbase.core.designsystem.component.AppScaffold
import dev.sautao.productbase.core.designsystem.component.AppTopBar
import dev.sautao.productbase.core.designsystem.component.PremiumBadge
import dev.sautao.productbase.core.designsystem.component.PrimaryButton
import dev.sautao.productbase.core.designsystem.component.SecondaryButton
import dev.sautao.productbase.core.designsystem.component.SectionHeader
import dev.sautao.productbase.core.designsystem.theme.AppTheme
import dev.sautao.productbase.demo.R
import dev.sautao.productbase.demo.ui.BackButton

@Composable
fun MonetisationScreen(
    uiState: MonetisationUiState,
    adsController: AdsController,
    onRecordAction: () -> Unit,
    onShowInterstitial: () -> Unit,
    onShowRewarded: () -> Unit,
    onShowPrivacyOptions: () -> Unit,
    onOpenPremium: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.monetisation_title),
                navigationIcon = { BackButton(onNavigateBack) },
                actions = {
                    if (uiState.premiumStatus == PremiumStatus.PREMIUM) {
                        PremiumBadge(
                            text = stringResource(R.string.monetisation_premium_badge),
                            modifier = Modifier.padding(end = AppTheme.spacing.md),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(title = stringResource(R.string.monetisation_status))
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spacing.md),
            ) {
                Body(
                    when {
                        uiState.adsAvailable -> stringResource(R.string.monetisation_ads_available)

                        uiState.premiumStatus == PremiumStatus.PREMIUM ->
                            stringResource(R.string.monetisation_ads_suppressed_premium)

                        // The start-up window. Worth showing here because it is the state this
                        // demo exists to make visible: ownership is unresolved, so ads wait.
                        uiState.premiumStatus == PremiumStatus.UNKNOWN ->
                            stringResource(R.string.monetisation_ads_suppressed_unknown)

                        else -> stringResource(R.string.monetisation_ads_unavailable)
                    },
                )
                Body(stringResource(R.string.monetisation_actions_count, uiState.qualifyingActions))
                Body(stringResource(R.string.monetisation_hints, uiState.hints))
            }

            if (uiState.privacyOptionsRequired) {
                SecondaryButton(
                    text = stringResource(R.string.monetisation_privacy_options),
                    onClick = onShowPrivacyOptions,
                    modifier = Modifier.padding(
                        horizontal = AppTheme.spacing.md,
                        vertical = AppTheme.spacing.sm,
                    ),
                )
            }

            SectionHeader(title = stringResource(R.string.monetisation_ads))
            PrimaryButton(
                text = stringResource(R.string.monetisation_record_action),
                onClick = onRecordAction,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.md),
            )
            SecondaryButton(
                text = stringResource(R.string.monetisation_show_interstitial),
                onClick = onShowInterstitial,
                modifier = Modifier.padding(
                    horizontal = AppTheme.spacing.md,
                    vertical = AppTheme.spacing.sm,
                ),
            )
            uiState.lastInterstitial?.let { Body(it.describe()) }

            SecondaryButton(
                text = stringResource(R.string.monetisation_show_rewarded),
                onClick = onShowRewarded,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.md),
            )
            uiState.lastRewarded?.let { Body(it.describe()) }

            SectionHeader(title = stringResource(R.string.monetisation_premium))
            PrimaryButton(
                text = stringResource(R.string.monetisation_open_premium),
                onClick = onOpenPremium,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.md),
            )

            // Shows nothing at all when ads are unavailable — no view, no request, no gap.
            AppBannerAd(
                adsController = adsController,
                modifier = Modifier.padding(top = AppTheme.spacing.lg),
            )
        }
    }
}

@Composable
private fun InterstitialOutcome.describe(): String = when (this) {
    InterstitialOutcome.Shown -> stringResource(R.string.monetisation_interstitial_shown)
    InterstitialOutcome.NotAvailable -> stringResource(R.string.monetisation_interstitial_unavailable)
    InterstitialOutcome.Throttled -> stringResource(R.string.monetisation_interstitial_throttled)
    is InterstitialOutcome.Failed -> stringResource(R.string.monetisation_interstitial_failed, code)
}

@Composable
private fun RewardedOutcome.describe(): String = when (this) {
    is RewardedOutcome.Earned -> stringResource(R.string.monetisation_rewarded_earned, amount, type)
    RewardedOutcome.Dismissed -> stringResource(R.string.monetisation_rewarded_dismissed)
    RewardedOutcome.NotAvailable -> stringResource(R.string.monetisation_rewarded_unavailable)
    is RewardedOutcome.Failed -> stringResource(R.string.monetisation_rewarded_failed, code)
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            horizontal = AppTheme.spacing.md,
            vertical = AppTheme.spacing.xs,
        ),
    )
}
