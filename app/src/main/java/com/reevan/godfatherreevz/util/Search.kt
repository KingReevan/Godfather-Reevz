package com.reevan.godfatherreevz.util

import java.text.Normalizer

/** Lower-cased with accents removed: "Émile" → "emile". */
fun String.folded(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

/**
 * The search rule every list shares (contact importer, Edit, Remove): any part of the [name],
 * ignoring case and accents, or — when the query has three or more digits — any of the [numbers].
 * A blank query matches everything.
 */
fun matchesSearch(name: String, numbers: List<String>, query: String): Boolean {
    val text = query.trim()
    if (text.isEmpty()) return true
    if (name.folded().contains(text.folded())) return true
    val digits = text.filter(Char::isDigit)
    return digits.length >= 3 && numbers.any { it.filter(Char::isDigit).contains(digits) }
}
