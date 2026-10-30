package com.reevan.godfatherreevz.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class DatesTest {

    @Test
    fun `call dates are dd-mm-yyyy`() {
        assertEquals("03-10-2026", formatCallDate(LocalDate.of(2026, 10, 3)))
        assertEquals("29-02-2028", formatCallDate(LocalDate.of(2028, 2, 29)))
    }

    @Test
    fun `never called shows a dash`() {
        assertEquals("-", formatLastCalled(null))
        assertEquals("31-12-2026", formatLastCalled(LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun `header date names the weekday and month`() {
        assertEquals("Thu, 29 October", formatHeaderDate(LocalDate.of(2026, 10, 29), Locale.UK))
    }

    @Test
    fun `next day starts at midnight`() {
        val now = ZonedDateTime.of(LocalDateTime.parse("2026-10-29T23:00"), ZoneId.of("Asia/Kolkata"))
        assertEquals(Duration.ofHours(1), durationUntilNextDay(now))
    }

    @Test
    fun `at exactly midnight the whole day is ahead`() {
        val now = ZonedDateTime.of(LocalDateTime.parse("2026-10-29T00:00"), ZoneId.of("Asia/Kolkata"))
        assertEquals(Duration.ofDays(1), durationUntilNextDay(now))
    }
}
