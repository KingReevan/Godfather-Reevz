package com.reevan.godfatherreevz.util

/**
 * Phone-number rules for adding, editing and dialling friends. Numbers are stored as typed or
 * imported (trimmed), so they look familiar; these helpers decide validity, sameness and what to
 * dial.
 */
object PhoneNumbers {

    const val MIN_DIGITS = 3
    const val MAX_DIGITS = 15

    /** Digits compared when checking whether two numbers are the same (see [sameNumber]). */
    private const val MATCH_DIGITS = 10

    private val Separators = setOf(' ', '-', '(', ')', '.')

    /** Trims and collapses runs of whitespace: "  +91  98765 43210 " → "+91 98765 43210". */
    fun clean(phone: String): String = phone.trim().replace(Regex("\\s+"), " ")

    fun digits(phone: String): String = phone.filter(Char::isDigit)

    /**
     * A plausible phone number: digits with optional separators (space - ( ) .) and an optional
     * leading +, and between [MIN_DIGITS] and [MAX_DIGITS] digits (the ITU maximum).
     */
    fun isValid(phone: String): Boolean {
        val cleaned = clean(phone)
        val body = cleaned.removePrefix("+")
        if (body.any { !it.isDigit() && it !in Separators }) return false
        return digits(body).length in MIN_DIGITS..MAX_DIGITS
    }

    /**
     * Whether [a] and [b] reach the same person. Compares the last 10 digits, so "+91 98765 43210",
     * "098765 43210" and "9876543210" all match; shorter numbers must match exactly.
     */
    fun sameNumber(a: String, b: String): Boolean {
        val x = digits(a)
        val y = digits(b)
        if (x.isEmpty() || y.isEmpty()) return false
        return if (x.length >= MATCH_DIGITS && y.length >= MATCH_DIGITS) {
            x.takeLast(MATCH_DIGITS) == y.takeLast(MATCH_DIGITS)
        } else {
            x == y
        }
    }

    /** What to put in the `tel:` URI: the leading + (if any) and the digits. */
    fun dialString(phone: String): String {
        val cleaned = clean(phone)
        return (if (cleaned.startsWith("+")) "+" else "") + digits(cleaned)
    }
}
