package com.reevan.godfatherreevz.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.reevan.godfatherreevz.R

/*
 * Two bundled variable fonts (tools/fetch_fonts.py, SIL OFL — docs/THIRD_PARTY.md). Each weight
 * below is the same file read at a different point on its weight axis. Reading a variable font's
 * axes is still marked experimental in Compose, hence the opt-ins.
 */

@OptIn(ExperimentalTextApi::class)
private fun playfair(weight: FontWeight) = Font(
    resId = R.font.playfair_display,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/**
 * Doto drawn with fully round dots, like Nothing OS's dot-matrix lettering. Its weight axis sets
 * the dot size, and at the usual weights the dots are too fine to read at text sizes, so each
 * weight reads further along the axis (Normal → 600 … Bold and up → 900).
 */
@OptIn(ExperimentalTextApi::class)
private fun doto(weight: FontWeight) = Font(
    resId = R.font.doto,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight((weight.weight + 200).coerceAtMost(900)),
        FontVariation.Setting("ROND", 100f),
    ),
)

/** Names and headings in every theme but Nothing: Playfair Display, a classic serif. */
val GodfatherSerifFont: FontFamily = FontFamily(
    playfair(FontWeight.Normal),
    playfair(FontWeight.Medium),
    playfair(FontWeight.SemiBold),
    playfair(FontWeight.Bold),
    playfair(FontWeight.ExtraBold),
)

/** Names and headings in the Nothing theme: Doto, a dot-matrix face. */
val DotMatrixFont: FontFamily = FontFamily(
    doto(FontWeight.Normal),
    doto(FontWeight.Medium),
    doto(FontWeight.SemiBold),
    doto(FontWeight.Bold),
    doto(FontWeight.ExtraBold),
)

/** Material's default type scale in the system font; the display font is applied per use. */
val Typography = Typography()
