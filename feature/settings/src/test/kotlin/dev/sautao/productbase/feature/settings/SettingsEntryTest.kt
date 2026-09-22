package dev.sautao.productbase.feature.settings

import android.os.Build
import dev.sautao.productbase.core.common.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsEntryTest {
    @Test
    fun `a section a product could not fill is not rendered`() {
        // The capability case: an app without billing builds an empty list for that section and
        // must not be left with a stray heading over nothing.
        val sections = listOf(
            SettingsSection(title = "Appearance", rows = listOf(themePicker())),
            SettingsSection(title = "Premium", rows = emptyList()),
        )

        assertEquals(listOf("Appearance"), sections.nonEmpty().map { it.title })
    }

    @Test
    fun `sections keep the order the product gave them`() {
        val sections = listOf(
            SettingsSection(title = "One", rows = listOf(action("a"))),
            SettingsSection(title = "Two", rows = emptyList()),
            SettingsSection(title = "Three", rows = listOf(action("b"))),
        )

        assertEquals(listOf("One", "Three"), sections.nonEmpty().map { it.title })
    }

    @Test
    fun `an action row reports the tap to the product`() {
        var tapped = false
        val row = SettingsRow.Action(title = "Share this app", onClick = { tapped = true })

        row.onClick()

        assertTrue(tapped)
    }

    @Test
    fun `a toggle row reports the new value`() {
        var value: Boolean? = null
        val row = SettingsRow.Toggle(title = "Reminders", checked = true, onCheckedChange = { value = it })

        row.onCheckedChange(false)

        assertEquals(false, value)
    }

    @Test
    fun `the theme picker reports the chosen mode`() {
        var chosen: ThemeMode? = null
        val row = SettingsRow.ThemePicker(themeMode = ThemeMode.SYSTEM, onThemeModeChange = { chosen = it })

        row.onThemeModeChange(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, chosen)
    }

    @Test
    fun `dynamic colour is offered from Android 12 onwards`() {
        assertTrue(dynamicColorSupported(Build.VERSION_CODES.S))
        assertTrue(dynamicColorSupported(Build.VERSION_CODES.S + 1))
        assertFalse(dynamicColorSupported(Build.VERSION_CODES.R))
    }

    private fun action(title: String) = SettingsRow.Action(title = title, onClick = {})

    private fun themePicker() =
        SettingsRow.ThemePicker(themeMode = ThemeMode.SYSTEM, onThemeModeChange = {})
}
