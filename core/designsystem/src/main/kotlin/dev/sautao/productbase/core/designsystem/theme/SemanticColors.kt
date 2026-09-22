package dev.sautao.productbase.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colours with a meaning that Material 3 does not define a role for.
 *
 * Kept to the two that recur in almost every app. Resist growing this: a colour belongs here
 * only when several unrelated screens need the same meaning, not when one screen wants a hue.
 */
@Immutable
data class SemanticColors(val success: Color, val onSuccess: Color, val warning: Color, val onWarning: Color)

val ProductBaseLightSemanticColors = SemanticColors(
    success = Color(0xFF1B6C3A),
    onSuccess = Color.White,
    warning = Color(0xFF8A5300),
    onWarning = Color.White,
)

val ProductBaseDarkSemanticColors = SemanticColors(
    success = Color(0xFF8FDBA6),
    onSuccess = Color(0xFF00391A),
    warning = Color(0xFFFFB95C),
    onWarning = Color(0xFF472A00),
)

val LocalSemanticColors = staticCompositionLocalOf { ProductBaseLightSemanticColors }
