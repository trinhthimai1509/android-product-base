package dev.sautao.productbase.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale. Material 3 has no spacing tokens of its own, so components and screens use
 * these instead of scattering magic dp values.
 */
@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
) {
    /** Android's minimum accessible touch target. */
    val minTouchTarget: Dp = 48.dp
}

val LocalSpacing = staticCompositionLocalOf { Spacing() }
