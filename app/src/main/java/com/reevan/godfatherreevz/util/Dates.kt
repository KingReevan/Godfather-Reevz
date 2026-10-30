package com.reevan.godfatherreevz.util

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CallDateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT)

/** A call date as the owner asked for it on every card: "03-10-2026". */
fun formatCallDate(date: LocalDate): String = date.format(CallDateFormat)

/** A card's last-called date, or "-" for a friend never called. */
fun formatLastCalled(date: LocalDate?): String = date?.let(::formatCallDate) ?: "-"

/** Home's header date, e.g. "Thu, 29 October". */
fun formatHeaderDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofPattern("EEE, dd MMMM", locale))

/**
 * Time from [now] until the next local day begins.
 *
 * Uses `atStartOfDay(zone)` rather than assuming 00:00, because in a few zones a DST change skips
 * midnight entirely and the day starts at 01:00. Always positive: at exactly midnight the answer is
 * the full day ahead, not zero.
 */
fun durationUntilNextDay(now: ZonedDateTime): Duration {
    val nextDayStart = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now, nextDayStart)
}
