package com.reevan.godfatherreevz.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/*
 * The six themes. Each is a full Material colour scheme — including the surfaceContainer roles
 * that dialogs, menus and switches read, so none of them falls back to Material's default
 * purple-tinted greys — plus the few app-specific colours Material has no role for.
 *
 * Coral and sunflower yellow are the house colours (the owner's brief); the default, Sicilian
 * Sunset, is built on them, and every other theme keeps a warm accent.
 */

/** App colours Material 3 has no role for. Read through `GodfatherTheme.colors`. */
@Immutable
data class GodfatherColors(
    val isDark: Boolean,
    /** Home: today's card is a gradient from [focusStart] to [focusEnd]... */
    val focusStart: Color,
    val focusEnd: Color,
    /** ...with its text and icons in [onFocus]. */
    val onFocus: Color,
    /** The soft pulsing ring round today's card while the call is still to make. */
    val focusGlow: Color,
    /** Home: the faded cards of past calls above today's card. */
    val pastCard: Color,
    val onPastCard: Color,
    /** "Called ✓" marks. */
    val called: Color,
)

// ---- Sicilian Sunset (default, light): coral and sunflower on warm cream --------------------

internal val SicilianSunsetScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFB83A2B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = Color(0xFF410001),
    inversePrimary = Color(0xFFFFB4A8),
    secondary = Color(0xFF7A5900),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDF9E),
    onSecondaryContainer = Color(0xFF261A00),
    tertiary = Color(0xFF8B4A62),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E3),
    onTertiaryContainer = Color(0xFF3A071F),
    background = Color(0xFFFFF6EA),
    onBackground = Color(0xFF2B1D17),
    surface = Color(0xFFFFF6EA),
    onSurface = Color(0xFF2B1D17),
    surfaceVariant = Color(0xFFF6E3D5),
    onSurfaceVariant = Color(0xFF5D4639),
    surfaceTint = Color(0xFFB83A2B),
    inverseSurface = Color(0xFF3F2D25),
    inverseOnSurface = Color(0xFFFFEDE4),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF8F7266),
    outlineVariant = Color(0xFFE5CFC2),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFF6EA),
    surfaceDim = Color(0xFFE8D6C8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0E2),
    surfaceContainer = Color(0xFFFCEADB),
    surfaceContainerHigh = Color(0xFFF7E3D2),
    surfaceContainerHighest = Color(0xFFF1DCCB),
)

internal val SicilianSunsetColors = GodfatherColors(
    isDark = false,
    focusStart = Color(0xFFFF6F5E),
    focusEnd = Color(0xFFFFC23C),
    onFocus = Color(0xFF2B1D17),
    focusGlow = Color(0xFFFF6F5E),
    pastCard = Color(0xFFFCEFE3),
    onPastCard = Color(0xFF7A6052),
    called = Color(0xFF2E7D32),
)

// ---- Sunset After Dark (dark): the default's evening sibling --------------------------------

internal val SunsetAfterDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFF8A7A),
    onPrimary = Color(0xFF5C0E05),
    primaryContainer = Color(0xFF8C2A1D),
    onPrimaryContainer = Color(0xFFFFDAD4),
    inversePrimary = Color(0xFFB83A2B),
    secondary = Color(0xFFFFC94D),
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF5C4300),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = Color(0xFFFFB1C8),
    onTertiary = Color(0xFF541D33),
    tertiaryContainer = Color(0xFF703349),
    onTertiaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFF1C1412),
    onBackground = Color(0xFFF4DED6),
    surface = Color(0xFF1C1412),
    onSurface = Color(0xFFF4DED6),
    surfaceVariant = Color(0xFF3A2C27),
    onSurfaceVariant = Color(0xFFD8C2B8),
    surfaceTint = Color(0xFFFF8A7A),
    inverseSurface = Color(0xFFF4DED6),
    inverseOnSurface = Color(0xFF3A2C27),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFFA08C84),
    outlineVariant = Color(0xFF53433D),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF44352F),
    surfaceDim = Color(0xFF1C1412),
    surfaceContainerLowest = Color(0xFF160F0D),
    surfaceContainerLow = Color(0xFF241A17),
    surfaceContainer = Color(0xFF2A1F1B),
    surfaceContainerHigh = Color(0xFF352925),
    surfaceContainerHighest = Color(0xFF403330),
)

internal val SunsetAfterDarkColors = GodfatherColors(
    isDark = true,
    focusStart = Color(0xFFFF6F5E),
    focusEnd = Color(0xFFFFB43C),
    onFocus = Color(0xFF2B1D17),
    focusGlow = Color(0xFFFFB43C),
    pastCard = Color(0xFF221916),
    onPastCard = Color(0xFFA08C84),
    called = Color(0xFF81C784),
)

// ---- Corleone Noir (dark): a tuxedo — black, ivory, a red rose and gold ---------------------

internal val CorleoneNoirScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFE8B44A),
    onPrimary = Color(0xFF3A2800),
    primaryContainer = Color(0xFF5A4100),
    onPrimaryContainer = Color(0xFFFFDEA3),
    inversePrimary = Color(0xFF7A5A00),
    secondary = Color(0xFFE5484D),
    onSecondary = Color(0xFF2A0004),
    secondaryContainer = Color(0xFF7A1A20),
    onSecondaryContainer = Color(0xFFFFDAD8),
    tertiary = Color(0xFFC9C3BD),
    onTertiary = Color(0xFF312E2A),
    tertiaryContainer = Color(0xFF484440),
    onTertiaryContainer = Color(0xFFE6E0DA),
    background = Color(0xFF0E0C0B),
    onBackground = Color(0xFFEDE6E1),
    surface = Color(0xFF0E0C0B),
    onSurface = Color(0xFFEDE6E1),
    surfaceVariant = Color(0xFF2A2624),
    onSurfaceVariant = Color(0xFFCFC6C0),
    surfaceTint = Color(0xFFE8B44A),
    inverseSurface = Color(0xFFEDE6E1),
    inverseOnSurface = Color(0xFF2A2624),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8E8580),
    outlineVariant = Color(0xFF3A3533),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF34302E),
    surfaceDim = Color(0xFF0E0C0B),
    surfaceContainerLowest = Color(0xFF080706),
    surfaceContainerLow = Color(0xFF161312),
    surfaceContainer = Color(0xFF1B1817),
    surfaceContainerHigh = Color(0xFF24201F),
    surfaceContainerHighest = Color(0xFF2E2A28),
)

internal val CorleoneNoirColors = GodfatherColors(
    isDark = true,
    focusStart = Color(0xFF4A1014),
    focusEnd = Color(0xFF1A1412),
    onFocus = Color(0xFFF5D58A),
    focusGlow = Color(0xFFE8B44A),
    pastCard = Color(0xFF141110),
    onPastCard = Color(0xFF8E8580),
    called = Color(0xFFA5D6A7),
)

// ---- Limoncello (light): lemon and leaf green ------------------------------------------------

internal val LimoncelloScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF4E7A3A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE873),
    onPrimaryContainer = Color(0xFF2A2400),
    inversePrimary = Color(0xFFB2D99A),
    secondary = Color(0xFF7A6400),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE16A),
    onSecondaryContainer = Color(0xFF241D00),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF2C1700),
    background = Color(0xFFFFFDF2),
    onBackground = Color(0xFF1F1D10),
    surface = Color(0xFFFFFDF2),
    onSurface = Color(0xFF1F1D10),
    surfaceVariant = Color(0xFFF3EDCC),
    onSurfaceVariant = Color(0xFF4D4733),
    surfaceTint = Color(0xFF4E7A3A),
    inverseSurface = Color(0xFF33311F),
    inverseOnSurface = Color(0xFFF8F2D8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF7E775F),
    outlineVariant = Color(0xFFD9D2B4),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFDF2),
    surfaceDim = Color(0xFFE3DBB4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF9E0),
    surfaceContainer = Color(0xFFFBF4D4),
    surfaceContainerHigh = Color(0xFFF5EDC8),
    surfaceContainerHighest = Color(0xFFEFE7BE),
)

internal val LimoncelloColors = GodfatherColors(
    isDark = false,
    focusStart = Color(0xFFFFD93D),
    focusEnd = Color(0xFFB7DD8C),
    onFocus = Color(0xFF1F1D10),
    focusGlow = Color(0xFFE5BE00),
    pastCard = Color(0xFFFBF6DE),
    onPastCard = Color(0xFF6E6850),
    called = Color(0xFF2E7D32),
)

// ---- Amalfi Coast (light): coral sunsets over Mediterranean blue, on sand --------------------

internal val AmalfiScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1F6FA3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE5FF),
    onPrimaryContainer = Color(0xFF001D33),
    inversePrimary = Color(0xFF98CBFF),
    secondary = Color(0xFFB23B2C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD4),
    onSecondaryContainer = Color(0xFF3F0400),
    tertiary = Color(0xFF6B5E00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF7E379),
    onTertiaryContainer = Color(0xFF201C00),
    background = Color(0xFFFBF3E4),
    onBackground = Color(0xFF1E1B16),
    surface = Color(0xFFFBF3E4),
    onSurface = Color(0xFF1E1B16),
    surfaceVariant = Color(0xFFEFE4D0),
    onSurfaceVariant = Color(0xFF4F4537),
    surfaceTint = Color(0xFF1F6FA3),
    inverseSurface = Color(0xFF33302A),
    inverseOnSurface = Color(0xFFF7EFE2),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF81756A),
    outlineVariant = Color(0xFFD6CAB6),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFBF3E4),
    surfaceDim = Color(0xFFDFD4C0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF8EC),
    surfaceContainer = Color(0xFFF7EDDC),
    surfaceContainerHigh = Color(0xFFF1E6D3),
    surfaceContainerHighest = Color(0xFFEBE0CB),
)

internal val AmalfiColors = GodfatherColors(
    isDark = false,
    focusStart = Color(0xFFFF8A73),
    focusEnd = Color(0xFF7FC0EA),
    onFocus = Color(0xFF1E1B16),
    focusGlow = Color(0xFF1F6FA3),
    pastCard = Color(0xFFF7EFE0),
    onPastCard = Color(0xFF6E6357),
    called = Color(0xFF2E7D32),
)

// ---- Nothing (dark): made for the Phone (2a) — black, white and one red dot -----------------

internal val NothingScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFD71921),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3B0A0C),
    onPrimaryContainer = Color(0xFFFFDAD7),
    inversePrimary = Color(0xFFD71921),
    secondary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF1F1F1F),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFBDBDBD),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF2A2A2A),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceTint = Color(0xFFD71921),
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF000000),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF5C0A0E),
    onErrorContainer = Color(0xFFFFDAD7),
    outline = Color(0xFF7A7A7A),
    outlineVariant = Color(0xFF2A2A2A),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF262626),
    surfaceDim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0D0D0D),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF242424),
)

internal val NothingColors = GodfatherColors(
    isDark = true,
    focusStart = Color(0xFFFFFFFF),
    focusEnd = Color(0xFFD9D9D9),
    onFocus = Color(0xFF000000),
    focusGlow = Color(0xFFD71921),
    pastCard = Color(0xFF0B0B0B),
    onPastCard = Color(0xFF8A8A8A),
    called = Color(0xFFFFFFFF),
)
