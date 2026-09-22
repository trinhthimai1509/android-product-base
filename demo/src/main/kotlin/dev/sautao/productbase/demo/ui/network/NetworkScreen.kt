package dev.sautao.productbase.demo.ui.network

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.sautao.productbase.core.designsystem.component.AppCard
import dev.sautao.productbase.core.designsystem.component.AppScaffold
import dev.sautao.productbase.core.designsystem.component.AppTopBar
import dev.sautao.productbase.core.designsystem.component.PrimaryButton
import dev.sautao.productbase.core.designsystem.component.SectionHeader
import dev.sautao.productbase.core.designsystem.theme.AppTheme
import dev.sautao.productbase.core.network.NetworkError
import dev.sautao.productbase.demo.R
import dev.sautao.productbase.demo.ui.BackButton

@Composable
fun NetworkScreen(
    isOnline: Boolean?,
    requestState: RequestState,
    onSendRequest: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.network_title),
                navigationIcon = { BackButton(onNavigateBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SectionHeader(title = stringResource(R.string.network_connectivity))
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spacing.md),
            ) {
                Text(
                    text = when (isOnline) {
                        null -> stringResource(R.string.network_status_checking)
                        true -> stringResource(R.string.network_status_online)
                        false -> stringResource(R.string.network_status_offline)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.network_status_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = AppTheme.spacing.xs),
                )
            }

            SectionHeader(title = stringResource(R.string.network_request))
            PrimaryButton(
                text = stringResource(R.string.network_send),
                onClick = onSendRequest,
                enabled = requestState != RequestState.InFlight,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.md),
            )
            Text(
                text = requestState.describe(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(AppTheme.spacing.md),
            )
        }
    }
}

/**
 * Errors become text here, at the UI edge, rather than in the ViewModel — which is why the base
 * needs no `UiText` type yet and why `NetworkError` never has to know about string resources.
 */
@Composable
private fun RequestState.describe(): String = when (this) {
    RequestState.Idle -> stringResource(R.string.network_result_idle)

    RequestState.InFlight -> stringResource(R.string.network_result_in_flight)

    is RequestState.Success -> stringResource(R.string.network_result_success, url)

    is RequestState.Failure -> when (val networkError = error) {
        NetworkError.NoConnection -> stringResource(R.string.network_error_no_connection)

        NetworkError.Timeout -> stringResource(R.string.network_error_timeout)

        is NetworkError.Http -> stringResource(R.string.network_error_http, networkError.code)

        NetworkError.Malformed -> stringResource(R.string.network_error_malformed)

        // Note what is not shown: the exception's message. Users get an explanation, and the
        // cause is what a CrashReporter would receive.
        is NetworkError.Unexpected -> stringResource(R.string.network_error_unexpected)
    }
}
