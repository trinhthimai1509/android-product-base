package dev.sautao.productbase.demo.ui.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.sautao.productbase.core.designsystem.component.AppScaffold
import dev.sautao.productbase.core.designsystem.component.AppTopBar
import dev.sautao.productbase.core.designsystem.component.EmptyState
import dev.sautao.productbase.core.designsystem.component.LoadingState
import dev.sautao.productbase.core.designsystem.component.PrimaryButton
import dev.sautao.productbase.core.designsystem.component.SecondaryButton
import dev.sautao.productbase.core.designsystem.component.SettingsItem
import dev.sautao.productbase.core.designsystem.theme.AppTheme
import dev.sautao.productbase.demo.R
import dev.sautao.productbase.demo.data.SampleEntryEntity
import dev.sautao.productbase.demo.ui.BackButton
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DataScreen(
    uiState: DataUiState,
    onAddEntry: () -> Unit,
    onClearEntries: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.data_title),
                navigationIcon = { BackButton(onNavigateBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                PrimaryButton(text = stringResource(R.string.data_add), onClick = onAddEntry)
                SecondaryButton(text = stringResource(R.string.data_clear), onClick = onClearEntries)
            }

            Text(
                text = stringResource(R.string.data_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.md),
            )

            when {
                uiState.isLoading -> LoadingState(
                    loadingDescription = stringResource(R.string.data_loading),
                )

                uiState.entries.isEmpty() -> EmptyState(
                    title = stringResource(R.string.data_empty_title),
                    description = stringResource(R.string.data_empty_description),
                )

                else -> EntryList(uiState.entries)
            }
        }
    }
}

@Composable
private fun EntryList(entries: List<SampleEntryEntity>) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = entries, key = { it.id }) { entry ->
            SettingsItem(
                title = entry.label,
                subtitle = formatter
                    .withZone(ZoneId.systemDefault())
                    .format(entry.createdAt),
            )
        }
    }
}
