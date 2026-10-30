package com.reevan.godfatherreevz.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.reevan.godfatherreevz.data.AppTheme
import com.reevan.godfatherreevz.ui.theme.colorScheme
import com.reevan.godfatherreevz.ui.theme.godfatherColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards legibility across all six themes, using WCAG contrast ratios: 4.5 for body text, 3.0 for
 * icons, large text and other graphics. Run after touching any colour.
 */
class ThemeColorsTest {

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertContrast(min: Double, a: Color, b: Color, what: String) {
        val ratio = contrast(a, b)
        assertTrue("$what: %.2f < %.1f".format(ratio, min), ratio >= min)
    }

    @Test
    fun everyTheme_textIsReadable() {
        AppTheme.entries.forEach { theme ->
            val s = theme.colorScheme()
            assertContrast(4.5, s.onBackground, s.background, "$theme onBackground")
            assertContrast(4.5, s.onSurface, s.surface, "$theme onSurface")
            assertContrast(4.5, s.onSurfaceVariant, s.surface, "$theme onSurfaceVariant")
            assertContrast(4.5, s.onSurface, s.surfaceContainerHigh, "$theme card/dialog text")
            assertContrast(4.5, s.onSurfaceVariant, s.surfaceContainerHigh, "$theme card secondary text")
            assertContrast(4.5, s.onPrimary, s.primary, "$theme onPrimary")
            assertContrast(4.5, s.onPrimaryContainer, s.primaryContainer, "$theme onPrimaryContainer")
            assertContrast(4.5, s.onSecondary, s.secondary, "$theme onSecondary")
            assertContrast(4.5, s.onSecondaryContainer, s.secondaryContainer, "$theme onSecondaryContainer")
            assertContrast(4.5, s.onError, s.error, "$theme onError")
            assertContrast(4.5, s.inverseOnSurface, s.inverseSurface, "$theme snackbar")
        }
    }

    @Test
    fun everyTheme_accentsStandOutFromTheBackground() {
        AppTheme.entries.forEach { theme ->
            val s = theme.colorScheme()
            // The app name, menu button outlines and text buttons are drawn in primary.
            assertContrast(3.0, s.primary, s.surface, "$theme primary on surface")
            assertContrast(3.0, s.primary, s.surfaceContainerHigh, "$theme primary on container")
            assertContrast(3.0, s.error, s.surface, "$theme error on surface")
            assertContrast(3.0, s.outline, s.surface, "$theme outline (field borders)")
        }
    }

    @Test
    fun everyTheme_todaysCardIsReadableAcrossItsWholeGradient() {
        AppTheme.entries.forEach { theme ->
            val c = theme.godfatherColors()
            assertContrast(4.5, c.onFocus, c.focusStart, "$theme today's card start")
            assertContrast(4.5, c.onFocus, c.focusEnd, "$theme today's card end")
            assertContrast(4.5, c.onFocus, lerp(c.focusStart, c.focusEnd), "$theme today's card middle")
        }
    }

    @Test
    fun everyTheme_pastCardsAreFadedButReadable() {
        AppTheme.entries.forEach { theme ->
            val s = theme.colorScheme()
            val c = theme.godfatherColors()
            assertContrast(3.0, c.onPastCard, c.pastCard, "$theme past card text")
            assertContrast(3.0, c.called, s.surface, "$theme called mark")
            assertContrast(3.0, c.called, s.surfaceContainerHigh, "$theme called mark on card")
        }
    }

    @Test
    fun everyTheme_knowsWhetherItIsDark() {
        AppTheme.entries.forEach { theme ->
            val dark = theme.colorScheme().background.luminance() < 0.5f
            assertEquals("$theme isDark", dark, theme.isDark)
            assertEquals("$theme colours isDark", dark, theme.godfatherColors().isDark)
        }
    }

    @Test
    fun theDefaultIsSicilianSunsetInCoralAndYellow() {
        assertEquals(AppTheme.SICILIAN_SUNSET, AppTheme.entries.first())
        val c = AppTheme.SICILIAN_SUNSET.godfatherColors()
        assertEquals(Color(0xFFFF6F5E), c.focusStart) // coral
        assertEquals(Color(0xFFFFC23C), c.focusEnd) // sunflower yellow
    }

    private fun lerp(a: Color, b: Color) = Color(
        red = (a.red + b.red) / 2,
        green = (a.green + b.green) / 2,
        blue = (a.blue + b.blue) / 2,
    )
}
