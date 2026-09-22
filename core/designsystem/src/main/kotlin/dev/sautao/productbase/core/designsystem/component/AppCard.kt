package dev.sautao.productbase.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.sautao.productbase.core.designsystem.theme.AppTheme

/**
 * Content container with consistent padding and corner radius.
 *
 * @param onClick when non-null the whole card becomes a single clickable target, which is what
 * screen readers expect; when null the card is decorative and its children handle their own input.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick == null) {
        Card(modifier = modifier) {
            Column(modifier = Modifier.padding(AppTheme.spacing.md), content = content)
        }
    } else {
        Card(onClick = onClick, modifier = modifier) {
            Column(modifier = Modifier.padding(AppTheme.spacing.md), content = content)
        }
    }
}
