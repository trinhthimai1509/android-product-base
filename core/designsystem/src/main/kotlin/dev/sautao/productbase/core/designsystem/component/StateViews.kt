package dev.sautao.productbase.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.sautao.productbase.core.designsystem.theme.AppTheme

/**
 * @param loadingDescription announced by screen readers. Required, because "loading" is
 * information a sighted user gets from the spinner and a non-sighted user would otherwise lose.
 */
@Composable
fun LoadingState(loadingDescription: String, modifier: Modifier = Modifier) {
    Box(
        modifier =
        modifier
            .fillMaxSize()
            .semantics { contentDescription = loadingDescription },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** Shown when a screen has loaded successfully but has nothing to display. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    MessageState(
        title = title,
        description = description,
        actionText = actionText,
        onAction = onAction,
        modifier = modifier,
    )
}

/**
 * Shown when a screen failed.
 *
 * @param description a message the product has already mapped to something a user can act on.
 * Never pass a raw exception message.
 */
@Composable
fun ErrorState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    retryText: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    MessageState(
        title = title,
        description = description,
        actionText = retryText,
        onAction = onRetry,
        modifier = modifier,
    )
}

@Composable
private fun MessageState(
    title: String,
    description: String?,
    actionText: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = AppTheme.spacing.sm),
            )
        }
        if (actionText != null && onAction != null) {
            PrimaryButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier.padding(top = AppTheme.spacing.md),
            )
        }
    }
}
