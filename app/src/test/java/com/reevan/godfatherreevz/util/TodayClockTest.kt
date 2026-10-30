package com.reevan.godfatherreevz.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TodayClockTest {

    private var now = at("2026-10-03T23:59", "Asia/Kolkata")
    private val clock = TodayClock { now }

    private fun at(text: String, zone: String): ZonedDateTime =
        ZonedDateTime.of(LocalDateTime.parse(text), ZoneId.of(zone))

    @Test
    fun startsOnTheCurrentDay() {
        assertEquals(LocalDate.of(2026, 10, 3), clock.today.value)
    }

    @Test
    fun refresh_afterMidnight_movesToTheNewDay() {
        now = at("2026-10-04T00:00:01", "Asia/Kolkata")
        clock.refresh()
        assertEquals(LocalDate.of(2026, 10, 4), clock.today.value)
    }

    @Test
    fun refresh_readsTheCurrentZone() {
        // 23:59 in Kolkata is still the previous evening in London; moving zone moves "today".
        now = now.withZoneSameInstant(ZoneId.of("Pacific/Kiritimati"))
        clock.refresh()
        assertEquals(LocalDate.of(2026, 10, 4), clock.today.value)
    }
}
