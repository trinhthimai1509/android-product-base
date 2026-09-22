package dev.sautao.productbase.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import dev.sautao.productbase.core.common.ThemeMode

/**
 * The root theme for every product built on this base.
 *
 * Every visual decision is a parameter with a neutral default, so a product brands itself by
 * passing its own values — never by editing `core:designsystem`.
 *
 * @param themeMode the user's preference; [ThemeMode.SYSTEM] follows the device setting.
 * @param dynamicColor use Material You wallpaper colours where the platform supports them (API 31+).
 */
@Composable
fun ProductBaseTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    lightColors: ColorScheme = ProductBaseLightColors,
    darkColors: ColorScheme = ProductBaseDarkColors,
    lightSemanticColors: SemanticColors = ProductBaseLightSemanticColors,
    darkSemanticColors: SemanticColors = ProductBaseDarkSemanticColors,
    typography: Typography = MaterialTheme.typography,
    shapes: Shapes = ProductBaseShapes,
    spacing: Spacing = Spacing(),
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }

    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme =
        when {
            dynamicColor && supportsDynamicColor -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> {
                darkColors
            }

            else -> {
                lightColors
            }
        }

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalSemanticColors provides if (darkTheme) darkSemanticColors else lightSemanticColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}

/** Access point for the tokens Material 3 does not carry itself. */
object AppTheme {
    val spacing: Spacing
        @Composable @ReadOnlyComposable
        get() = LocalSpacing.current

    val semanticColors: SemanticColors
        @Composable @ReadOnlyComposable
        get() = LocalSemanticColors.current
}
