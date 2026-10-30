package com.reevan.godfatherreevz.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import com.reevan.godfatherreevz.data.AppTheme

/** The Material scheme for a theme. Exposed so Settings can preview themes that aren't active. */
fun AppTheme.colorScheme(): ColorScheme = when (this) {
    AppTheme.SICILIAN_SUNSET -> SicilianSunsetScheme
    AppTheme.SUNSET_AFTER_DARK -> SunsetAfterDarkScheme
    AppTheme.CORLEONE_NOIR -> CorleoneNoirScheme
    AppTheme.LIMONCELLO -> LimoncelloScheme
    AppTheme.AMALFI -> AmalfiScheme
    AppTheme.NOTHING -> NothingScheme
}

/** The app-specific colours for a theme. */
fun AppTheme.godfatherColors(): GodfatherColors = when (this) {
    AppTheme.SICILIAN_SUNSET -> SicilianSunsetColors
    AppTheme.SUNSET_AFTER_DARK -> SunsetAfterDarkColors
    AppTheme.CORLEONE_NOIR -> CorleoneNoirColors
    AppTheme.LIMONCELLO -> LimoncelloColors
    AppTheme.AMALFI -> AmalfiColors
    AppTheme.NOTHING -> NothingColors
}

/** The font for names and headings: an elegant serif, or dot-matrix for the Nothing theme. */
fun AppTheme.displayFont(): FontFamily = when (this) {
    AppTheme.NOTHING -> DotMatrixFont
    else -> GodfatherSerifFont
}

private val LocalGodfatherColors = staticCompositionLocalOf { SicilianSunsetColors }
private val LocalDisplayFont = staticCompositionLocalOf { GodfatherSerifFont }

/** Entry point for what Material 3 has no role for, alongside `MaterialTheme`. */
object GodfatherTheme {
    val colors: GodfatherColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGodfatherColors.current

    /** Names and headings. Body text stays in the system font. */
    val displayFont: FontFamily
        @Composable
        @ReadOnlyComposable
        get() = LocalDisplayFont.current
}

/**
 * The app theme: one of six fixed palettes chosen in Settings (decision D7 — each theme is light
 * or dark by itself; there is no "follow system").
 *
 * **Material You dynamic colour is deliberately off.** The themes are named palettes, and a
 * wallpaper-derived scheme would repaint them in whatever the phone's background happens to be.
 */
@Composable
fun GodfatherReevzTheme(
    theme: AppTheme = AppTheme.SICILIAN_SUNSET,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalGodfatherColors provides theme.godfatherColors(),
        LocalDisplayFont provides theme.displayFont(),
    ) {
        MaterialTheme(
            colorScheme = theme.colorScheme(),
            typography = Typography,
            content = content,
        )
    }
}
