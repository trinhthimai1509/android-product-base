package dev.sautao.productbase.demo.ui.infra

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.sautao.productbase.core.designsystem.component.AppScaffold
import dev.sautao.productbase.core.designsystem.component.AppTopBar
import dev.sautao.productbase.core.designsystem.component.PrimaryButton
import dev.sautao.productbase.core.designsystem.component.SecondaryButton
import dev.sautao.productbase.core.designsystem.component.SectionHeader
import dev.sautao.productbase.core.designsystem.theme.AppTheme
import dev.sautao.productbase.core.notification.POST_NOTIFICATIONS_PERMISSION
import dev.sautao.productbase.core.notification.canPostNotifications
import dev.sautao.productbase.demo.R
import dev.sautao.productbase.demo.ui.BackButton

@Composable
fun InfraScreen(
    uiState: InfraUiState,
    onSendTelemetry: () -> Unit,
    onScheduleReminder: (title: String, text: String) -> Unit,
    onCancelReminder: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var canNotify by remember { mutableStateOf(context.canPostNotifications()) }

    // Requesting the permission is the product's job; core:notification stays free of any UI
    // dependency and only tells us whether a notification would currently be shown.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { canNotify = context.canPostNotifications() }

    val reminderTitle = stringResource(R.string.infra_reminder_notification_title)
    val reminderText = stringResource(R.string.infra_reminder_notification_text)

    AppScaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.infra_title),
                navigationIcon = { BackButton(onNavigateBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(title = stringResource(R.string.infra_telemetry))
            Body(stringResource(R.string.infra_telemetry_explanation))
            PrimaryButton(
                text = stringResource(R.string.infra_telemetry_send),
                onClick = onSendTelemetry,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.md),
            )
            if (uiState.telemetrySent) {
                Body(stringResource(R.string.infra_telemetry_sent))
            }

            SectionHeader(title = stringResource(R.string.infra_reminder))
            Body(stringResource(R.string.infra_reminder_explanation))

            if (!canNotify) {
                Body(stringResource(R.string.infra_reminder_permission_needed))
                SecondaryButton(
                    text = stringResource(R.string.infra_reminder_grant),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(POST_NOTIFICATIONS_PERMISSION)
                        }
                    },
                    modifier = Modifier.padding(horizontal = AppTheme.spacing.md),
                )
            }

            PrimaryButton(
                text = stringResource(R.string.infra_reminder_schedule),
                onClick = { onScheduleReminder(reminderTitle, reminderText) },
                enabled = canNotify,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.md),
            )
            SecondaryButton(
                text = stringResource(R.string.infra_reminder_cancel),
                onClick = onCancelReminder,
                modifier = Modifier.padding(
                    horizontal = AppTheme.spacing.md,
                    vertical = AppTheme.spacing.sm,
                ),
            )
            if (uiState.reminderScheduled) {
                Body(stringResource(R.string.infra_reminder_scheduled))
            }
            if (uiState.remindersTurnedOff) {
                Body(stringResource(R.string.infra_reminder_turned_off))
            }
        }
    }
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
