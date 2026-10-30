package com.reevan.godfatherreevz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.reevan.godfatherreevz.rotation.Rotation
import com.reevan.godfatherreevz.rotation.Slot
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import kotlin.random.Random

/** A friend about to be added. */
data class NewFriend(val name: String, val phone: String)

/** What pressing Call did. */
enum class CallOutcome {
    /** A new call was recorded for today. */
    RECORDED,

    /** This friend already had a call recorded today; nothing new was counted. */
    ALREADY_RECORDED,

    /** The friend no longer exists (removed while their page was open). */
    FRIEND_GONE,
}

/** The rotation columns of one friend, as stored, with their most recent call. */
data class SlotRow(
    val id: Long,
    val position: Int,
    val turnUsed: Boolean,
    val lastCalled: LocalDate?,
)

/**
 * Friends, and every change to the rotation.
 *
 * Each rotation change is one transaction: read the order, apply one pure [Rotation] rule, save
 * only the rows that changed. Who is "today's friend" and whether today's call is done are always
 * worked out here, inside the transaction, from [today] — never taken from what a screen last
 * showed — so a stale screen or two quick taps can't record the wrong thing.
 */
@Dao
abstract class FriendDao {

    // ---- Reads for screens -------------------------------------------------------------------

    /** Every friend in call order (the head first), with their most recent call. */
    @Query(
        """
        SELECT f.*, (SELECT MAX(c.date) FROM calls c WHERE c.friendId = f.id) AS lastCalled
        FROM friends f ORDER BY f.position, f.id
        """,
    )
    abstract fun observeInCallOrder(): Flow<List<FriendWithLastCall>>

    @Query(
        """
        SELECT f.*, (SELECT MAX(c.date) FROM calls c WHERE c.friendId = f.id) AS lastCalled
        FROM friends f WHERE f.id = :id
        """,
    )
    abstract fun observe(id: Long): Flow<FriendWithLastCall?>

    /** A–Z, for the Edit and Remove lists. */
    @Query("SELECT * FROM friends ORDER BY name COLLATE NOCASE, id")
    abstract fun observeAlphabetical(): Flow<List<Friend>>

    @Query("SELECT * FROM friends WHERE id = :id")
    abstract suspend fun get(id: Long): Friend?

    @Query("SELECT * FROM friends")
    abstract suspend fun getAll(): List<Friend>

    // ---- Edits that never touch the rotation -------------------------------------------------

    /** In place, never REPLACE: the friend's calls and rotation place stay exactly as they were. */
    @Query("UPDATE friends SET name = :name, phone = :phone WHERE id = :id")
    abstract suspend fun updateDetails(id: Long, name: String, phone: String): Int

    @Query("UPDATE friends SET notes = :notes WHERE id = :id")
    abstract suspend fun updateNotes(id: Long, notes: String)

    // ---- Rotation changes --------------------------------------------------------------------

    /**
     * Adds [friends] (in the given order) and places each in the rotation by the new-friend rule.
     * Returns their ids.
     */
    @Transaction
    open suspend fun addFriends(
        friends: List<NewFriend>,
        today: LocalDate,
        createdAt: Long,
        random: Random,
    ): List<Long> {
        var order = slots()
        val headLocked = isHeadLocked(today)
        val ids = friends.map { new ->
            val id = insertFriend(
                Friend(
                    name = new.name,
                    phone = new.phone,
                    addedOn = today,
                    createdAt = createdAt,
                    position = order.size,
                ),
            )
            order = Rotation.addFriend(order, id, headLocked, random)
            id
        }
        save(order)
        return ids
    }

    /**
     * The owner pressed Call for [friendId] on [today]. Records the call — scheduled if they are
     * today's friend and today's call isn't made yet — and moves them in the rotation.
     *
     * At most one call per friend per day: a second press the same day only redials. The one
     * exception is when that friend has since become today's friend (someone ahead of them was
     * removed): their earlier call then counts as today's call, so today can't get stuck.
     */
    @Transaction
    open suspend fun recordCall(friendId: Long, today: LocalDate, random: Random): CallOutcome {
        val friend = get(friendId) ?: return CallOutcome.FRIEND_GONE
        val order = slots()
        val slot = order.first { it.id == friendId }
        val isFocus = isHeadLocked(today) && order.first().id == friendId
        val rowId = insertCallOrIgnore(
            Call(
                friendId = friendId,
                friendName = friend.name,
                date = today,
                scheduled = isFocus,
                consumedTurn = !slot.turnUsed,
            ),
        )
        if (rowId != -1L) {
            save(Rotation.afterCall(order, friendId, today, random))
            return CallOutcome.RECORDED
        }
        if (isFocus) {
            val existing = callOn(friendId, today)
            if (existing != null) {
                markScheduled(existing.id, consumedTurn = existing.consumedTurn || !slot.turnUsed)
                if (!slot.turnUsed) save(Rotation.afterCall(order, friendId, today, random))
            }
        }
        return CallOutcome.ALREADY_RECORDED
    }

    /**
     * Undoes the call to [friendId] recorded on [today] (only today's call can be undone) and puts
     * the rotation back. Returns false if there was nothing to undo — e.g. a second tap on Undo.
     */
    @Transaction
    open suspend fun undoCall(friendId: Long, today: LocalDate, random: Random): Boolean {
        val call = callOn(friendId, today) ?: return false
        if (deleteCall(call.id) != 1) return false
        val order = slots()
        if (order.none { it.id == friendId }) return true
        val restored = when {
            call.scheduled -> Rotation.undoScheduled(order, friendId)
            call.consumedTurn -> Rotation.undoOffSchedule(
                order = order,
                friendId = friendId,
                headLocked = isHeadLocked(today),
                random = random,
            )
            else -> order
        }
        save(restored)
        return true
    }

    /** Removes [ids]. Their calls stay (friendId set to null) so Statistics keep counting them. */
    @Transaction
    open suspend fun removeFriends(ids: List<Long>) {
        if (ids.isEmpty()) return
        deleteFriends(ids)
        save(Rotation.afterRemoval(slots()))
    }

    /** Settings > Shuffle again. */
    @Transaction
    open suspend fun reshuffle(today: LocalDate, random: Random) {
        save(Rotation.reshuffle(slots(), today, random))
    }

    // ---- Internals ---------------------------------------------------------------------------

    @Query(
        """
        SELECT f.id AS id, f.position AS position, f.turnUsed AS turnUsed,
            (SELECT MAX(c.date) FROM calls c WHERE c.friendId = f.id) AS lastCalled
        FROM friends f ORDER BY f.position, f.id
        """,
    )
    abstract suspend fun slotRows(): List<SlotRow>

    @Insert
    abstract suspend fun insertFriend(friend: Friend): Long

    @Query("UPDATE friends SET position = :position, turnUsed = :turnUsed WHERE id = :id")
    abstract suspend fun updateSlot(id: Long, position: Int, turnUsed: Boolean)

    @Query("DELETE FROM friends WHERE id IN (:ids)")
    abstract suspend fun deleteFriends(ids: List<Long>): Int

    /** Returns -1 if this friend already has a call on that date. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCallOrIgnore(call: Call): Long

    @Query("SELECT * FROM calls WHERE friendId = :friendId AND date = :date")
    abstract suspend fun callOn(friendId: Long, date: LocalDate): Call?

    @Query("DELETE FROM calls WHERE id = :id")
    abstract suspend fun deleteCall(id: Long): Int

    @Query("UPDATE calls SET scheduled = 1, consumedTurn = :consumedTurn WHERE id = :id")
    abstract suspend fun markScheduled(id: Long, consumedTurn: Boolean)

    @Query(
        """
        SELECT * FROM calls WHERE scheduled = 1 AND date BETWEEN :from AND :to
        ORDER BY date DESC, id DESC LIMIT 1
        """,
    )
    abstract suspend fun scheduledCallBetween(from: LocalDate, to: LocalDate): Call?

    /** True while today's call hasn't been made: the head is today's friend and must stay put. */
    private suspend fun isHeadLocked(today: LocalDate): Boolean =
        TodayWindow.of(today).let { scheduledCallBetween(it.start, it.endInclusive) } == null

    private suspend fun slots(): List<Slot> =
        slotRows().map { Slot(id = it.id, turnUsed = it.turnUsed, lastCalled = it.lastCalled) }

    /** Writes [order] back, touching only the rows whose place or turn changed. */
    private suspend fun save(order: List<Slot>) {
        val stored = slotRows().associateBy { it.id }
        order.forEachIndexed { position, slot ->
            val row = stored[slot.id]
            if (row == null || row.position != position || row.turnUsed != slot.turnUsed) {
                updateSlot(slot.id, position, slot.turnUsed)
            }
        }
    }
}

/**
 * Which dates count as "today's call is done": a scheduled call dated today, or up to two days
 * ahead. The slack means setting the clock back a day can't unlock a second scheduled call for
 * the same real day, while a clock that was briefly set far ahead can't lock today out for long.
 */
object TodayWindow {
    private const val SLACK_DAYS = 2L

    fun of(today: LocalDate): ClosedRange<LocalDate> = today..today.plusDays(SLACK_DAYS)
}
