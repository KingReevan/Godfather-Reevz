package com.reevan.godfatherreevz.rotation

import java.time.LocalDate
import kotlin.random.Random

/**
 * One friend's place in the rotation as the engine sees it. A `List<Slot>` is the call order:
 * index 0 (the head) is next up, and each friend's index is roughly the number of days until
 * their next call.
 */
data class Slot(
    val id: Long,
    /** Already called in the current pass. */
    val turnUsed: Boolean,
    /** Their most recent call, or null if they have never been called. */
    val lastCalled: LocalDate?,
) {
    val everCalled: Boolean get() = lastCalled != null
}

/**
 * The rules deciding who to call and when. Pure functions over a call order, so every rule is
 * pinned by `RotationTest`; `FriendDao` runs each one inside a transaction and saves the result.
 *
 * The owner's rules (docs/SPEC.md, decisions D1–D3):
 * - Everyone gets one call per pass, in a shuffled order; nobody is called twice before everyone
 *   has been called once. Then the next pass starts **silently**: there are no round numbers and
 *   nothing is ever reordered at a pass boundary, so the owner can't tell when repetition starts.
 * - Today's friend is the head and stays today's until called (a missed day just slides on).
 * - A new friend is called before any repeat.
 * - Calling someone who isn't today's friend uses their turn for this pass.
 *
 * **Invariant:** every waiting friend (`!turnUsed`) comes before every friend whose turn is used,
 * and while anyone is in the rotation at least one friend is waiting — when the last waiting
 * friend is called, everyone becomes waiting again in the same order ([rolledOver]).
 *
 * "The head is locked" means today's call hasn't been made yet, so the head is today's friend and
 * must not change under the owner: adding or un-calling a friend never lands in front of it.
 */
object Rotation {

    /**
     * How far back, at least, a just-called friend goes: nobody is called again within this many
     * days, even across the end of a pass. Half the friends, at most 3, so small rotations keep
     * some randomness (with 3 friends a fixed "always last" would make the order predictable).
     */
    fun spacing(size: Int): Int = minOf(3, size / 2)

    /** How many friends are still waiting for this pass's call. They are the first this many. */
    fun waitingCount(order: List<Slot>): Int = order.count { !it.turnUsed }

    /**
     * [friendId] was called on [calledOn]. Their turn is used (if it wasn't already) and they go to
     * a random place among the friends whose turn is used, but at least [spacing] places back. If
     * that was the last waiting friend, the next pass starts.
     *
     * Also used when someone whose turn is already used is called again off-schedule: they are
     * re-placed the same way, so they aren't scheduled again soon after.
     */
    fun afterCall(order: List<Slot>, friendId: Long, calledOn: LocalDate, random: Random): List<Slot> {
        val index = indexOf(order, friendId)
        val called = order[index]
        val rest = order.toMutableList().apply { removeAt(index) }
        val lastCalled = maxOf(called.lastCalled ?: calledOn, calledOn)
        val min = maxOf(waitingCount(rest), spacing(order.size))
        rest.add(random.nextInt(min, rest.size + 1), called.copy(turnUsed = true, lastCalled = lastCalled))
        return rolledOver(rest)
    }

    /**
     * Adds [newId] (never called) to the rotation: at a random place among the waiting friends who
     * have never been called, so they come before anyone who'd be getting a repeat call. If
     * everyone waiting has been called before, that puts them at the front. Never in front of a
     * locked head.
     */
    fun addFriend(order: List<Slot>, newId: Long, headLocked: Boolean, random: Random): List<Slot> {
        val (lo, firstCalledBefore) = freshRange(order, headLocked)
        val index = random.nextInt(lo, firstCalledBefore + 1)
        return order.toMutableList().apply { add(index, Slot(newId, turnUsed = false, lastCalled = null)) }
    }

    /**
     * Friends were removed. Renumbering is implicit; this only starts the next pass if everyone
     * left had already had their turn.
     */
    fun afterRemoval(order: List<Slot>): List<Slot> = rolledOver(order)

    /**
     * "Shuffle again" (Settings): everyone still waiting gets a new random place — today's friend
     * too, since the owner asked for it — while never-called friends stay ahead of anyone who'd be
     * a repeat, and nobody whose turn is used jumps ahead of anyone waiting. Friends called in the
     * last [spacing] days are kept at least that many places back.
     */
    fun reshuffle(order: List<Slot>, today: LocalDate, random: Random): List<Slot> {
        val space = spacing(order.size)
        val recentSince = today.minusDays(space.toLong())
        fun Slot.isRecent() = lastCalled != null && lastCalled >= recentSince

        val (waiting, used) = order.partition { !it.turnUsed }
        val (fresh, calledBefore) = waiting.partition { !it.everCalled }

        // Inserting one at a time at a uniformly random index gives a uniform shuffle. Recent
        // friends go in last, each at least `space` places back: insertions only ever push
        // earlier ones further back, so every recent friend keeps its distance.
        val result = fresh.shuffled(random).toMutableList()
        fun insertAll(slots: List<Slot>, floor: Int) {
            val (recent, others) = slots.partition { it.isRecent() }
            for (slot in others.shuffled(random)) {
                result.add(random.nextInt(floor, result.size + 1), slot)
            }
            for (slot in recent.shuffled(random)) {
                val min = minOf(maxOf(floor, space), result.size)
                result.add(random.nextInt(min, result.size + 1), slot)
            }
        }
        insertAll(calledBefore, floor = fresh.size)
        insertAll(used, floor = waiting.size)
        return result
    }

    /**
     * Undoing today's scheduled call to [friendId]: they are today's friend again, at the head,
     * waiting — exactly as before the call was recorded.
     */
    fun undoScheduled(order: List<Slot>, friendId: Long): List<Slot> {
        val index = indexOf(order, friendId)
        val friend = order[index].copy(turnUsed = false)
        return listOf(friend) + order.filterIndexed { i, _ -> i != index }
    }

    /**
     * Undoing an off-schedule call to [friendId] that used their turn: they are waiting again, at a
     * random place by the same rule as a new friend (if they have never been called otherwise) or
     * among the waiting friends who have been called before.
     *
     * If the next pass has started since (their turn is no longer marked used), the order is left
     * alone: putting them back would give them an extra turn in the new pass.
     */
    fun undoOffSchedule(
        order: List<Slot>,
        friendId: Long,
        headLocked: Boolean,
        random: Random,
    ): List<Slot> {
        val index = indexOf(order, friendId)
        val friend = order[index]
        if (!friend.turnUsed) return order
        val rest = order.toMutableList().apply { removeAt(index) }
        val (lo, firstCalledBefore) = freshRange(rest, headLocked)
        val range = if (friend.everCalled) {
            firstCalledBefore..waitingCount(rest)
        } else {
            lo..firstCalledBefore
        }
        rest.add(random.nextInt(range.first, range.last + 1), friend.copy(turnUsed = false))
        return rest
    }

    /** Starts the next pass if nobody is left waiting. Nothing moves, so it is invisible. */
    fun rolledOver(order: List<Slot>): List<Slot> =
        if (order.isNotEmpty() && order.all { it.turnUsed }) {
            order.map { it.copy(turnUsed = false) }
        } else {
            order
        }

    /**
     * Where a never-called friend may go: from `lo` (1 if the head is locked, so today's friend
     * stays put; else 0) up to and including the index of the first waiting friend at or after
     * `lo` who has been called before (or the end of the waiting block if there is none).
     */
    private fun freshRange(order: List<Slot>, headLocked: Boolean): Pair<Int, Int> {
        val waiting = waitingCount(order)
        val lo = if (headLocked && order.isNotEmpty()) 1 else 0
        val firstCalledBefore = (lo until waiting).firstOrNull { order[it].everCalled } ?: waiting
        return lo to maxOf(lo, firstCalledBefore)
    }

    private fun indexOf(order: List<Slot>, friendId: Long): Int =
        order.indexOfFirst { it.id == friendId }.also {
            require(it >= 0) { "Friend $friendId is not in the rotation" }
        }
}
