package com.reevan.godfatherreevz.ui

import com.reevan.godfatherreevz.data.Friend
import com.reevan.godfatherreevz.data.FriendWithLastCall
import com.reevan.godfatherreevz.ui.statistics.DayMark
import com.reevan.godfatherreevz.ui.statistics.callStats
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CallStatsTest {

    private val today = LocalDate.of(2026, 10, 30)
    private fun day(offset: Long) = today.plusDays(offset)

    private fun friend(id: Long, name: String, lastCalled: LocalDate?) = FriendWithLastCall(
        Friend(id, name, "9000000$id", addedOn = day(-100), createdAt = id, position = id.toInt()),
        lastCalled,
    )

    @Test
    fun `nothing yet is all zeros`() {
        val stats = callStats(emptyList(), emptyList(), today)
        assertEquals(0, stats.totalCalls)
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.longestStreak)
        assertEquals(1, stats.daysSoFarThisMonth) // starting today
        assertEquals(31, stats.monthDays.size)
    }

    @Test
    fun `an uncalled today keeps yesterday's streak`() {
        val dates = listOf(day(-3), day(-2), day(-1))
        assertEquals(3, callStats(dates, emptyList(), today).currentStreak)
        assertEquals(4, callStats(dates + today, emptyList(), today).currentStreak)
    }

    @Test
    fun `a missed day breaks the streak`() {
        val dates = listOf(day(-5), day(-4), day(-3), day(-1))
        val stats = callStats(dates, emptyList(), today)
        assertEquals(1, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
    }

    @Test
    fun `two calls on one day count twice in totals but once in streaks and the month`() {
        val stats = callStats(listOf(today, today, day(-1)), emptyList(), today)
        assertEquals(3, stats.totalCalls)
        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.daysCalledThisMonth)
    }

    @Test
    fun `streaks cross month and year boundaries`() {
        val newYear = LocalDate.of(2027, 1, 2)
        val dates = listOf(LocalDate.of(2026, 12, 30), LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1), newYear)
        assertEquals(4, callStats(dates, emptyList(), newYear).currentStreak)
    }

    @Test
    fun `future-dated calls count in totals only`() {
        val stats = callStats(listOf(day(1), day(-1)), emptyList(), today)
        assertEquals(2, stats.totalCalls)
        assertEquals(1, stats.currentStreak)
        assertEquals(DayMark.FUTURE, stats.monthDays[30]) // the 31st
    }

    @Test
    fun `days before starting are neither called nor missed`() {
        // First friend added on the 28th, nothing called yet.
        val stats = callStats(emptyList(), listOf(friend(1, "New", null).let { it.copy(friend = it.friend.copy(addedOn = day(-2))) }), today)
        assertEquals(DayMark.BEFORE_START, stats.monthDays[26]) // the 27th
        assertEquals(DayMark.MISSED, stats.monthDays[27]) // the 28th
        assertEquals(DayMark.TODAY_PENDING, stats.monthDays[29])
        assertEquals(3, stats.daysSoFarThisMonth) // 28th, 29th, 30th
    }

    @Test
    fun `a start in an earlier month counts the whole month so far`() {
        val stats = callStats(listOf(LocalDate.of(2026, 9, 15)), emptyList(), today)
        assertEquals(30, stats.daysSoFarThisMonth)
        assertEquals(DayMark.MISSED, stats.monthDays[0])
    }

    @Test
    fun `the month strip marks called, missed, today and the future`() {
        val stats = callStats(listOf(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)), emptyList(), today)
        assertEquals(YearMonth.of(2026, 10), stats.month)
        assertEquals(DayMark.CALLED, stats.monthDays[0])
        assertEquals(DayMark.MISSED, stats.monthDays[1])
        assertEquals(DayMark.CALLED, stats.monthDays[2])
        assertEquals(DayMark.TODAY_PENDING, stats.monthDays[29])
        assertEquals(DayMark.FUTURE, stats.monthDays[30])
    }

    @Test
    fun `waiting longest lists the oldest last calls and counts never-called apart`() {
        val friends = listOf(
            friend(1, "Recent", day(-1)),
            friend(2, "Old", day(-40)),
            friend(3, "Never", null),
            friend(4, "Older", day(-60)),
            friend(5, "Middle", day(-10)),
        )
        val stats = callStats(emptyList(), friends, today)
        assertEquals(listOf("Older", "Old", "Middle"), stats.waitingLongest.map { it.name })
        assertEquals(60L, stats.waitingLongest.first().daysAgo)
        assertEquals(1, stats.neverCalled)
        assertEquals(5, stats.friends)
    }

    @Test
    fun `six months of counts, oldest first`() {
        val dates = listOf(LocalDate.of(2026, 5, 31), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2), today)
        val months = callStats(dates, emptyList(), today).lastSixMonths
        assertEquals((5..10).map { YearMonth.of(2026, it) }, months.map { it.month })
        assertEquals(listOf(1, 0, 0, 0, 2, 1), months.map { it.calls })
    }
}
