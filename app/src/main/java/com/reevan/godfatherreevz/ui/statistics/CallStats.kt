package com.reevan.godfatherreevz.ui.statistics

import com.reevan.godfatherreevz.data.FriendWithLastCall
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** A day in this month's strip. */
enum class DayMark {
    CALLED,
    MISSED,
    TODAY_PENDING,
    FUTURE,

    /** Before the owner started (first friend added or first call): neither called nor missed. */
    BEFORE_START,
}

/** A friend who has gone longest without a call. */
data class Waiting(val friendId: Long, val name: String, val lastCalled: LocalDate, val daysAgo: Long)

data class MonthCount(val month: YearMonth, val calls: Int)

/**
 * Everything Statistics shows. Deliberately no per-friend call counts: when every count is equal
 * a pass has just ended, and the owner asked not to be able to tell (docs/SPEC.md).
 */
data class CallStats(
    val friends: Int,
    val totalCalls: Int,
    /** Days of this month, up to today, with at least one call. */
    val daysCalledThisMonth: Int,
    /** Days of this month so far, today included — counted from the start if that was this month. */
    val daysSoFarThisMonth: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val neverCalled: Int,
    /** This month, day 1 to its last, in order. */
    val month: YearMonth,
    val monthDays: List<DayMark>,
    /** Up to three friends called longest ago (never-called friends are counted separately). */
    val waitingLongest: List<Waiting>,
    /** The last six months, oldest first, this one last. */
    val lastSixMonths: List<MonthCount>,
)

/**
 * Works out Statistics from every call ever made ([callDates] — removed friends' calls included,
 * so totals and streaks never drop), today's friends and [today].
 *
 * A streak is consecutive days with at least one call. Today counts once a call is made, but an
 * uncalled today doesn't break the streak until the day is over (it still shows yesterday's).
 * Calls dated after [today] (the phone's clock was moved back) count in the totals only.
 *
 * Days before the owner started — the earliest of a friend being added and a call — are neither
 * called nor missed: the app wasn't in use yet.
 */
fun callStats(callDates: List<LocalDate>, friends: List<FriendWithLastCall>, today: LocalDate): CallStats {
    val days = callDates.filter { it <= today }.toSortedSet()
    val month = YearMonth.from(today)
    val started = listOfNotNull(friends.minOfOrNull { it.friend.addedOn }, days.firstOrNull())
        .minOrNull()
        ?.coerceAtMost(today)
        ?: today

    val currentStreak = run {
        var day = if (today in days) today else today.minusDays(1)
        var count = 0
        while (day in days) {
            count++
            day = day.minusDays(1)
        }
        count
    }
    val longestStreak = run {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in days) {
            run = if (previous != null && day == previous.plusDays(1)) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        best
    }
    val monthDays = (1..month.lengthOfMonth()).map { dayOfMonth ->
        val day = month.atDay(dayOfMonth)
        when {
            day in days -> DayMark.CALLED
            day < started -> DayMark.BEFORE_START
            day == today -> DayMark.TODAY_PENDING
            day < today -> DayMark.MISSED
            else -> DayMark.FUTURE
        }
    }
    val waitingLongest = friends
        .mapNotNull { f -> f.lastCalled?.let { Waiting(f.friend.id, f.friend.name, it, ChronoUnit.DAYS.between(it, today)) } }
        .sortedWith(compareBy<Waiting> { it.lastCalled }.thenBy { it.name })
        .take(3)
    val lastSixMonths = (5 downTo 0).map { back ->
        val m = month.minusMonths(back.toLong())
        MonthCount(m, callDates.count { YearMonth.from(it) == m })
    }
    return CallStats(
        friends = friends.size,
        totalCalls = callDates.size,
        daysCalledThisMonth = days.count { YearMonth.from(it) == month },
        daysSoFarThisMonth = today.dayOfMonth - maxOf(started, month.atDay(1)).dayOfMonth + 1,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        neverCalled = friends.count { it.lastCalled == null },
        month = month,
        monthDays = monthDays,
        waitingLongest = waitingLongest,
        lastSixMonths = lastSixMonths,
    )
}
