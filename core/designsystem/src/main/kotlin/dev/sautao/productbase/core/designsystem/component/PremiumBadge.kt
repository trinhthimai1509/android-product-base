package dev.sautao.productbase.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.sautao.productbase.core.designsystem.theme.AppTheme

/**
 * Marks a feature as part of a paid tier.
 *
 * Purely visual — it knows nothing about entitlement. The caller decides when to show it, using
 * `PremiumState` (Phase 4), so this component stays usable in apps with no billing at all.
 *
 * @param text already-resolved, localised label.
 */
@Composable
fun PremiumBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier =
            Modifier.padding(
                horizontal = AppTheme.spacing.sm,
                vertical = AppTheme.spacing.xs,
            ),
        )
    }
}
