package dev.sautao.productbase.demo.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sautao.productbase.core.designsystem.component.AppCard
import dev.sautao.productbase.core.designsystem.component.AppDialog
import dev.sautao.productbase.core.designsystem.component.AppScaffold
import dev.sautao.productbase.core.designsystem.component.AppTopBar
import dev.sautao.productbase.core.designsystem.component.EmptyState
import dev.sautao.productbase.core.designsystem.component.ErrorState
import dev.sautao.productbase.core.designsystem.component.LoadingState
import dev.sautao.productbase.core.designsystem.component.PremiumBadge
import dev.sautao.productbase.core.designsystem.component.PrimaryButton
import dev.sautao.productbase.core.designsystem.component.SecondaryButton
import dev.sautao.productbase.core.designsystem.component.SectionHeader
import dev.sautao.productbase.core.designsystem.component.SettingsItem
import dev.sautao.productbase.core.designsystem.component.SwitchSettingsItem
import dev.sautao.productbase.core.designsystem.theme.AppTheme
import dev.sautao.productbase.demo.R

/**
 * Renders every component in `core:designsystem` in one place.
 *
 * This is the design system's verification surface: if a component cannot be shown here without
 * awkwardness, its API is wrong.
 */
@Composable
fun CatalogScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    var dialogVisible by remember { mutableStateOf(false) }
    var switchChecked by remember { mutableStateOf(true) }

    AppScaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.catalog_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                actions = {
                    PremiumBadge(
                        text = stringResource(R.string.catalog_premium_badge),
                        modifier = Modifier.padding(end = AppTheme.spacing.md),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(title = stringResource(R.string.catalog_section_buttons))
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.catalog_primary_button),
                    onClick = { dialogVisible = true },
                )
                SecondaryButton(
                    text = stringResource(R.string.catalog_secondary_button),
                    onClick = { dialogVisible = true },
                )
            }
            PrimaryButton(
                text = stringResource(R.string.catalog_disabled_button),
                onClick = {},
                enabled = false,
                modifier =
                Modifier.padding(
                    start = AppTheme.spacing.md,
                    top = AppTheme.spacing.sm,
                ),
            )

            SectionHeader(title = stringResource(R.string.catalog_section_containers))
            AppCard(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spacing.md),
                onClick = { dialogVisible = true },
            ) {
                Text(
                    text = stringResource(R.string.catalog_card_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.catalog_card_body),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = AppTheme.spacing.xs),
                )
            }
            SecondaryButton(
                text = stringResource(R.string.catalog_show_dialog),
                onClick = { dialogVisible = true },
                modifier =
                Modifier.padding(
                    start = AppTheme.spacing.md,
                    top = AppTheme.spacing.sm,
                ),
            )

            SectionHeader(title = stringResource(R.string.catalog_section_states))
            CatalogStateBox {
                LoadingState(loadingDescription = stringResource(R.string.catalog_loading_description))
            }
            CatalogStateBox {
                EmptyState(
                    title = stringResource(R.string.catalog_empty_title),
                    description = stringResource(R.string.catalog_empty_description),
                    actionText = stringResource(R.string.catalog_empty_action),
                    onAction = { dialogVisible = true },
                )
            }
            CatalogStateBox {
                ErrorState(
                    title = stringResource(R.string.catalog_error_title),
                    description = stringResource(R.string.catalog_error_description),
                    retryText = stringResource(R.string.catalog_error_retry),
                    onRetry = { dialogVisible = true },
                )
            }

            SectionHeader(title = stringResource(R.string.catalog_section_list_items))
            SettingsItem(
                title = stringResource(R.string.catalog_settings_item),
                subtitle = stringResource(R.string.catalog_settings_item_subtitle),
                onClick = { dialogVisible = true },
            )
            SwitchSettingsItem(
                title = stringResource(R.string.catalog_switch_item),
                subtitle = stringResource(R.string.catalog_switch_item_subtitle),
                checked = switchChecked,
                onCheckedChange = { switchChecked = it },
            )
        }
    }

    if (dialogVisible) {
        AppDialog(
            title = stringResource(R.string.catalog_dialog_title),
            text = stringResource(R.string.catalog_dialog_text),
            confirmText = stringResource(R.string.catalog_dialog_confirm),
            dismissText = stringResource(R.string.catalog_dialog_dismiss),
            onConfirm = { dialogVisible = false },
            onDismiss = { dialogVisible = false },
        )
    }
}

/** The state components fill their parent, so the catalog gives each one a bounded box. */
@Composable
private fun CatalogStateBox(content: @Composable () -> Unit) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(CATALOG_STATE_HEIGHT_DP.dp),
    ) {
        content()
    }
}

private const val CATALOG_STATE_HEIGHT_DP = 200
