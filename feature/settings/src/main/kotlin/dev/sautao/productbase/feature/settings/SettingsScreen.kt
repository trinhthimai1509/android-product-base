package dev.sautao.productbase.feature.settings

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.sautao.productbase.core.common.ThemeMode
import dev.sautao.productbase.core.designsystem.component.AppScaffold
import dev.sautao.productbase.core.designsystem.component.AppTopBar
import dev.sautao.productbase.core.designsystem.component.SectionHeader
import dev.sautao.productbase.core.designsystem.component.SettingsItem
import dev.sautao.productbase.core.designsystem.component.SwitchSettingsItem

/**
 * A settings screen assembled from what the product supplies.
 *
 * Stateless: rows carry their own values and callbacks, so this composable holds nothing, and a
 * product can drive it from a ViewModel, a preview or a test without changing it.
 *
 * @param title the screen title, product-owned like every other user-facing word.
 * @param sections in the order they should appear. Empty sections are dropped.
 */
@Composable
fun SettingsScreen(
    title: String,
    sections: List<SettingsSection>,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
) {
    AppScaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = title, navigationIcon = navigationIcon) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            sections.nonEmpty().forEach { section ->
                section.title?.let { SectionHeader(title = it) }
                section.rows.forEach { SettingsRowContent(it) }
            }
        }
    }
}

@Composable
private fun SettingsRowContent(row: SettingsRow) {
    when (row) {
        is SettingsRow.Action -> SettingsItem(
            title = row.title,
            subtitle = row.subtitle,
            onClick = row.onClick,
        )

        is SettingsRow.Toggle -> SwitchSettingsItem(
            title = row.title,
            subtitle = row.subtitle,
            checked = row.checked,
            onCheckedChange = row.onCheckedChange,
            enabled = row.enabled,
        )

        // No click affordance: a read-only row that announces itself as a button is a lie to a
        // screen reader.
        is SettingsRow.Info -> SettingsItem(title = row.title, subtitle = row.value)

        is SettingsRow.ThemePicker -> ThemePickerRows(row)

        is SettingsRow.DynamicColor -> DynamicColorRow(row)
    }
}

@Composable
private fun ThemePickerRows(row: SettingsRow.ThemePicker) {
    ThemeOption(R.string.pb_settings_theme_light, ThemeMode.LIGHT, row)
    ThemeOption(R.string.pb_settings_theme_dark, ThemeMode.DARK, row)
    ThemeOption(R.string.pb_settings_theme_system, ThemeMode.SYSTEM, row)
}

@Composable
private fun ThemeOption(@StringRes labelRes: Int, option: ThemeMode, row: SettingsRow.ThemePicker) {
    SettingsItem(
        title = stringResource(labelRes),
        onClick = { row.onThemeModeChange(option) },
        trailingContent = {
            // The row carries the click; the radio button is a visual indicator only, so screen
            // readers announce one control instead of two.
            RadioButton(selected = option == row.themeMode, onClick = null)
        },
    )
}

@Composable
private fun DynamicColorRow(row: SettingsRow.DynamicColor) {
    val supported = dynamicColorSupported(Build.VERSION.SDK_INT)

    SwitchSettingsItem(
        title = stringResource(R.string.pb_settings_dynamic_colour),
        subtitle = stringResource(
            if (supported) {
                R.string.pb_settings_dynamic_colour_subtitle
            } else {
                R.string.pb_settings_dynamic_colour_unsupported
            },
        ),
        checked = row.enabled && supported,
        onCheckedChange = row.onEnabledChange,
        enabled = supported,
    )
}

/**
 * Material You needs Android 12.
 *
 * A pure function of the SDK level so the rule is testable off-device, and so the one version
 * check in this module lives in a single named place instead of being repeated inline.
 */
internal fun dynamicColorSupported(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.S
