package com.reevan.godfatherreevz.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Read-only views of the call log. Calls are written only through [FriendDao]'s transactions. */
@Dao
interface CallDao {

    /** The most recent calls up to [today], newest first: Home's faded past section. */
    @Query("SELECT * FROM calls WHERE date <= :today ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecent(today: LocalDate, limit: Int): Flow<List<Call>>

    /** The scheduled call within [from]..[to] (see [TodayWindow]), if today's call is made. */
    @Query(
        """
        SELECT * FROM calls WHERE scheduled = 1 AND date BETWEEN :from AND :to
        ORDER BY date DESC, id DESC LIMIT 1
        """,
    )
    fun observeScheduledBetween(from: LocalDate, to: LocalDate): Flow<Call?>

    /** [friendId]'s call on [date], if any: drives "Called today ✓ · Undo". */
    @Query("SELECT * FROM calls WHERE friendId = :friendId AND date = :date")
    fun observeCallOn(friendId: Long, date: LocalDate): Flow<Call?>

    /** Every call ever, oldest first, removed friends included: Statistics. */
    @Query("SELECT * FROM calls ORDER BY date, id")
    fun observeAll(): Flow<List<Call>>
}
