package dev.sautao.productbase.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * A deliberately neutral fallback palette.
 *
 * The base ships no brand. A product supplies its own colour schemes through
 * [ProductBaseTheme]'s `lightColors` / `darkColors` parameters — it must never need to edit
 * this file to look like itself.
 */
private val Indigo40 = Color(0xFF4C5BD4)
private val Indigo80 = Color(0xFFB9C0FF)
private val Indigo90 = Color(0xFFE0E3FF)
private val Indigo20 = Color(0xFF1B2379)
private val Indigo10 = Color(0xFF0B1155)

private val Slate40 = Color(0xFF5C5D72)
private val Slate80 = Color(0xFFC5C4DD)
private val Slate90 = Color(0xFFE2E0F9)
private val Slate20 = Color(0xFF2E2F42)

private val Teal40 = Color(0xFF0F6E63)
private val Teal80 = Color(0xFF7BD8CB)
private val Teal90 = Color(0xFF9FF2E4)
private val Teal20 = Color(0xFF003731)

private val Red40 = Color(0xFFBA1A1A)
private val Red80 = Color(0xFFFFB4AB)
private val Red90 = Color(0xFFFFDAD6)
private val Red20 = Color(0xFF690005)

private val Neutral10 = Color(0xFF1B1B1F)
private val Neutral20 = Color(0xFF303034)
private val Neutral90 = Color(0xFFE4E1E6)
private val Neutral95 = Color(0xFFF3F0F4)
private val Neutral99 = Color(0xFFFFFBFF)

/** Default light scheme, used when a product supplies none. */
val ProductBaseLightColors = lightColorScheme(
    primary = Indigo40,
    onPrimary = Color.White,
    primaryContainer = Indigo90,
    onPrimaryContainer = Indigo10,
    secondary = Slate40,
    onSecondary = Color.White,
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate20,
    tertiary = Teal40,
    onTertiary = Color.White,
    tertiaryContainer = Teal90,
    onTertiaryContainer = Teal20,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red20,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    onSurfaceVariant = Neutral20,
    outline = Slate40,
)

/** Default dark scheme, used when a product supplies none. */
val ProductBaseDarkColors = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo20,
    primaryContainer = Indigo20,
    onPrimaryContainer = Indigo90,
    secondary = Slate80,
    onSecondary = Slate20,
    secondaryContainer = Slate20,
    onSecondaryContainer = Slate90,
    tertiary = Teal80,
    onTertiary = Teal20,
    tertiaryContainer = Teal20,
    onTertiaryContainer = Teal90,
    error = Red80,
    onError = Red20,
    errorContainer = Red20,
    onErrorContainer = Red90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = Neutral90,
    outline = Slate80,
)
