package com.reevan.godfatherreevz.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * The app's single notion of "today". Home, who is today's friend, and which day a call
 * is recorded on all read from here, so they can never disagree.
 *
 * Kept current three ways, because none is enough alone:
 * - [tickAtMidnight] sleeps until the next day starts while the app is on screen.
 * - MainActivity restarts it every time the app comes back to the foreground, which re-reads the
 *   clock. Coroutine delays don't count time the phone spends asleep, so a delay started at 23:00
 *   could otherwise wake up hours late.
 * - MainActivity also restarts it on every clock, time zone or date change ([clockChanges]). The
 *   delay measures elapsed time, so a jump in the wall clock would otherwise go unnoticed.
 *
 * [now] is read fresh on every call, so a time-zone change is picked up at the next refresh.
 */
class TodayClock(
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) {
    private val _today = MutableStateFlow(now().toLocalDate())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    fun refresh() {
        _today.value = now().toLocalDate()
    }

    /**
     * Refreshes now and then at the start of every following day, until cancelled. If the delay
     * wakes a moment early the loop simply refreshes to the same date and waits again.
     */
    suspend fun tickAtMidnight(): Nothing {
        while (true) {
            refresh()
            delay(durationUntilNextDay(now()).toMillis())
        }
    }

    companion object {
        /** The process-wide clock. ViewModels read [today] from this instance. */
        val instance = TodayClock()
    }
}
