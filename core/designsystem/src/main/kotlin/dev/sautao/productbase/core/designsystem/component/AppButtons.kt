package dev.sautao.productbase.core.designsystem.component

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.sautao.productbase.core.designsystem.theme.AppTheme

/** The single most prominent action on a screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = AppTheme.spacing.minTouchTarget),
        enabled = enabled,
    ) {
        Text(text = text)
    }
}

/** A secondary action, shown alongside a [PrimaryButton]. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = AppTheme.spacing.minTouchTarget),
        enabled = enabled,
    ) {
        Text(text = text)
    }
}
