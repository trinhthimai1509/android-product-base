package dev.sautao.productbase.demo.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.sautao.productbase.core.designsystem.component.AppCard
import dev.sautao.productbase.core.designsystem.component.AppScaffold
import dev.sautao.productbase.core.designsystem.component.AppTopBar
import dev.sautao.productbase.core.designsystem.component.SectionHeader
import dev.sautao.productbase.core.designsystem.component.SettingsItem
import dev.sautao.productbase.core.designsystem.theme.AppTheme
import dev.sautao.productbase.demo.R

@Composable
fun HomeScreen(
    onOpenCatalog: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenData: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenInfra: () -> Unit,
    onOpenMonetisation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = stringResource(R.string.home_title)) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            AppCard(modifier = Modifier.padding(AppTheme.spacing.md)) {
                Text(
                    text = stringResource(R.string.home_intro),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.home_phase),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = AppTheme.spacing.sm),
                )
            }

            SectionHeader(title = stringResource(R.string.home_section_ui))
            SettingsItem(
                title = stringResource(R.string.home_open_catalog),
                subtitle = stringResource(R.string.home_open_catalog_subtitle),
                onClick = onOpenCatalog,
            )
            SettingsItem(
                title = stringResource(R.string.home_open_settings),
                subtitle = stringResource(R.string.home_open_settings_subtitle),
                onClick = onOpenSettings,
            )

            SectionHeader(title = stringResource(R.string.home_section_capabilities))
            SettingsItem(
                title = stringResource(R.string.home_open_data),
                subtitle = stringResource(R.string.home_open_data_subtitle),
                onClick = onOpenData,
            )
            SettingsItem(
                title = stringResource(R.string.home_open_network),
                subtitle = stringResource(R.string.home_open_network_subtitle),
                onClick = onOpenNetwork,
            )
            SettingsItem(
                title = stringResource(R.string.home_open_infra),
                subtitle = stringResource(R.string.home_open_infra_subtitle),
                onClick = onOpenInfra,
            )
            SettingsItem(
                title = stringResource(R.string.home_open_monetisation),
                subtitle = stringResource(R.string.home_open_monetisation_subtitle),
                onClick = onOpenMonetisation,
            )
        }
    }
}
