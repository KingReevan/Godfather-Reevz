package com.reevan.godfatherreevz.rotation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class RotationTest {

    private val day0: LocalDate = LocalDate.of(2026, 10, 1)
    private fun day(n: Int): LocalDate = day0.plusDays(n.toLong())

    private fun fresh(vararg ids: Long) = ids.map { Slot(it, turnUsed = false, lastCalled = null) }
    private fun seen(id: Long, on: Int = 0) = Slot(id, turnUsed = false, lastCalled = day(on))
    private fun used(id: Long, on: Int = 0) = Slot(id, turnUsed = true, lastCalled = day(on))
    private fun List<Slot>.ids() = map { it.id }

    // ---- spacing ----------------------------------------------------------------------------

    @Test
    fun `spacing is half the friends, at most three`() {
        assertEquals(listOf(0, 0, 1, 1, 2, 2, 3, 3, 3), (0..8).map(Rotation::spacing))
        assertEquals(3, Rotation.spacing(40))
    }

    // ---- afterCall --------------------------------------------------------------------------

    @Test
    fun `a called friend joins the used block at least spacing places back`() {
        val order = fresh(1, 2, 3, 4, 5, 6, 7, 8)
        repeat(200) { seed ->
            val after = Rotation.afterCall(order, 1, day(0), Random(seed))
            val index = after.indexOfFirst { it.id == 1L }
            assertTrue("index $index", index >= 7) // 7 still waiting, spacing 3
            assertTrue(after[index].turnUsed)
            assertEquals(day(0), after[index].lastCalled)
            assertEquals(listOf(2L, 3, 4, 5, 6, 7, 8), after.take(7).ids())
        }
    }

    @Test
    fun `near the end of a pass the spacing still holds`() {
        // One friend still waiting, five used: the called one must land at index 3 or later.
        val order = fresh(1) + listOf(used(2), used(3), used(4), used(5), used(6))
        repeat(200) { seed ->
            val after = Rotation.afterCall(order, 1, day(9), Random(seed))
            assertTrue(after.indexOfFirst { it.id == 1L } >= 3)
        }
    }

    @Test
    fun `calling the last waiting friend starts the next pass without moving anyone else`() {
        val order = fresh(1) + listOf(used(2), used(3), used(4))
        val after = Rotation.afterCall(order, 1, day(5), Random(1))
        assertTrue(after.none { it.turnUsed })
        assertEquals(listOf(2L, 3, 4), after.filter { it.id != 1L }.ids())
    }

    @Test
    fun `one friend is today's friend every day`() {
        var order = fresh(1)
        repeat(5) { d ->
            order = Rotation.afterCall(order, 1, day(d), Random(d))
            assertEquals(listOf(1L), order.ids())
            assertFalse(order.single().turnUsed)
        }
    }

    @Test
    fun `two friends strictly alternate`() {
        var order = fresh(1, 2)
        val called = mutableListOf<Long>()
        repeat(10) { d ->
            val head = order.first().id
            called += head
            order = Rotation.afterCall(order, head, day(d), Random(d))
        }
        called.zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }
    }

    @Test
    fun `calling someone whose turn is used re-places them without using another turn`() {
        val order = fresh(1, 2, 3) + listOf(used(4), used(5), used(6))
        repeat(100) { seed ->
            val after = Rotation.afterCall(order, 4, day(3), Random(seed))
            assertEquals(listOf(1L, 2, 3), after.take(3).ids())
            assertTrue(after.drop(3).all { it.turnUsed })
            assertTrue(after.indexOfFirst { it.id == 4L } >= 3)
            assertEquals(day(3), after.first { it.id == 4L }.lastCalled)
        }
    }

    @Test
    fun `an off-schedule call never moves today's friend`() {
        val order = fresh(1, 2, 3, 4)
        repeat(100) { seed ->
            assertEquals(1L, Rotation.afterCall(order, 3, day(0), Random(seed)).first().id)
        }
    }

    @Test
    fun `a call dated before an existing call keeps the later date`() {
        val order = listOf(seen(1, on = 10), seen(2, on = 3))
        val after = Rotation.afterCall(order, 1, day(8), Random(0)) // clock set back
        assertEquals(day(10), after.first { it.id == 1L }.lastCalled)
    }

    // ---- addFriend --------------------------------------------------------------------------

    @Test
    fun `the first friend goes in alone`() {
        assertEquals(listOf(Slot(9, false, null)), Rotation.addFriend(emptyList(), 9, true, Random(0)))
    }

    @Test
    fun `a locked head is never displaced by a new friend`() {
        val order = fresh(1, 2, 3)
        repeat(200) { seed ->
            val after = Rotation.addFriend(order, 9, headLocked = true, random = Random(seed))
            assertEquals(1L, after.first().id)
        }
    }

    @Test
    fun `in the first pass a new friend lands anywhere among the waiting`() {
        val order = fresh(1, 2, 3) + listOf(used(4), used(5))
        val seenAt = (0 until 500).map { seed ->
            Rotation.addFriend(order, 9, headLocked = false, random = Random(seed)).indexOfFirst { it.id == 9L }
        }.toSet()
        assertEquals(setOf(0, 1, 2, 3), seenAt)
    }

    @Test
    fun `when everyone waiting has been called before a new friend goes to the front`() {
        val order = listOf(seen(1), seen(2), seen(3), used(4))
        assertEquals(9L, Rotation.addFriend(order, 9, headLocked = false, random = Random(3)).first().id)
        // Locked: straight after today's friend.
        val locked = Rotation.addFriend(order, 9, headLocked = true, random = Random(3))
        assertEquals(listOf(1L, 9), locked.take(2).ids())
    }

    @Test
    fun `a new friend lands among never-called friends, ahead of every repeat`() {
        val order = fresh(1, 2) + listOf(seen(3), seen(4), used(5))
        repeat(300) { seed ->
            val index = Rotation.addFriend(order, 9, headLocked = false, random = Random(seed))
                .indexOfFirst { it.id == 9L }
            assertTrue("index $index", index in 0..2)
        }
    }

    @Test
    fun `behind a called-before locked head a new friend still beats every other repeat`() {
        // Today's friend (3) has been called before and is locked; 1 and 2 never have.
        val order = listOf(seen(3), Slot(1, false, null), Slot(2, false, null), seen(4))
        repeat(300) { seed ->
            val index = Rotation.addFriend(order, 9, headLocked = true, random = Random(seed))
                .indexOfFirst { it.id == 9L }
            assertTrue("index $index", index in 1..3)
        }
    }

    @Test
    fun `with today's call done a called-before head does not shelter repeats`() {
        // Regression: the unlocked head (tomorrow's friend) must count as a repeat.
        val order = listOf(seen(3), seen(4), used(5))
        repeat(50) { seed ->
            assertEquals(9L, Rotation.addFriend(order, 9, headLocked = false, random = Random(seed)).first().id)
        }
    }

    // ---- afterRemoval -----------------------------------------------------------------------

    @Test
    fun `removing the last waiting friend starts the next pass`() {
        val after = Rotation.afterRemoval(listOf(used(2), used(3)))
        assertTrue(after.none { it.turnUsed })
        assertEquals(listOf(2L, 3), after.ids())
    }

    @Test
    fun `removing everyone leaves an empty rotation`() {
        assertEquals(emptyList<Slot>(), Rotation.afterRemoval(emptyList()))
    }

    // ---- reshuffle --------------------------------------------------------------------------

    @Test
    fun `shuffling keeps never-called first, repeats next and used turns last`() {
        val order = fresh(1, 2, 3) + listOf(seen(4, on = 0), seen(5, on = 0)) +
            listOf(used(6, on = 1), used(7, on = 2), used(8, on = 3))
        repeat(300) { seed ->
            val after = Rotation.reshuffle(order, today = day(30), random = Random(seed))
            assertEquals(order.ids().toSet(), after.ids().toSet())
            assertEquals(setOf(1L, 2, 3), after.take(3).ids().toSet())
            assertEquals(setOf(4L, 5), after.subList(3, 5).ids().toSet())
            assertEquals(setOf(6L, 7, 8), after.drop(5).ids().toSet())
            assertEquals(order.map { it.id to it.turnUsed }.toSet(), after.map { it.id to it.turnUsed }.toSet())
        }
    }

    @Test
    fun `shuffling really reorders, including today's friend`() {
        val order = fresh(1, 2, 3, 4, 5, 6)
        val heads = (0 until 200).map { Rotation.reshuffle(order, day(0), Random(it)).first().id }.toSet()
        assertEquals(order.ids().toSet(), heads)
    }

    @Test
    fun `shuffling keeps friends called in the last few days away from the front`() {
        // Everyone has been called; 1 and 2 only yesterday and today.
        val order = listOf(seen(1, on = 9), seen(2, on = 10)) +
            (3L..8L).map { seen(it, on = 0) }
        repeat(300) { seed ->
            val after = Rotation.reshuffle(order, today = day(10), random = Random(seed))
            assertTrue(after.indexOfFirst { it.id == 1L } >= 3)
            assertTrue(after.indexOfFirst { it.id == 2L } >= 3)
        }
    }

    @Test
    fun `shuffling an empty or single rotation is harmless`() {
        assertEquals(emptyList<Slot>(), Rotation.reshuffle(emptyList(), day(0), Random(0)))
        assertEquals(fresh(1), Rotation.reshuffle(fresh(1), day(0), Random(0)))
    }

    // ---- undo -------------------------------------------------------------------------------

    @Test
    fun `undoing today's call makes that friend today's again`() {
        val called = Rotation.afterCall(fresh(1, 2, 3, 4), 1, day(0), Random(7))
        val undone = Rotation.undoScheduled(called, 1)
        assertEquals(1L, undone.first().id)
        assertTrue(undone.none { it.turnUsed })
        assertEquals(listOf(2L, 3, 4), undone.drop(1).ids())
    }

    @Test
    fun `undoing today's call after it ended the pass still restores today's friend`() {
        val order = fresh(1) + listOf(used(2), used(3))
        val called = Rotation.afterCall(order, 1, day(5), Random(1)) // pass rolled over
        val undone = Rotation.undoScheduled(called, 1)
        assertEquals(1L, undone.first().id)
        assertTrue(undone.none { it.turnUsed })
    }

    @Test
    fun `undoing an off-schedule call gives the turn back, behind the locked head`() {
        val order = fresh(1, 2, 3, 4)
        repeat(100) { seed ->
            val called = Rotation.afterCall(order, 3, day(0), Random(seed))
            // Undo: friend 3's only call is gone, so they are never-called again.
            val withoutCall = called.map { if (it.id == 3L) it.copy(lastCalled = null) else it }
            val undone = Rotation.undoOffSchedule(withoutCall, 3, headLocked = true, random = Random(seed))
            assertEquals(1L, undone.first().id)
            assertTrue(undone.none { it.turnUsed })
            assertEquals(setOf(1L, 2, 3, 4), undone.ids().toSet())
        }
    }

    @Test
    fun `an undone repeat goes back among the waiting repeats, not ahead of new friends`() {
        val order = listOf(seen(1), Slot(2, false, null), seen(3), used(4, on = 1))
        repeat(200) { seed ->
            val undone = Rotation.undoOffSchedule(order, 4, headLocked = true, random = Random(seed))
            val index = undone.indexOfFirst { it.id == 4L }
            assertTrue("index $index", index in 2..3)
            assertFalse(undone[index].turnUsed)
        }
    }

    @Test
    fun `undoing after the next pass began leaves the order alone`() {
        val order = listOf(seen(1), seen(2), seen(3)) // friend 2's turn already reset by a rollover
        assertEquals(order, Rotation.undoOffSchedule(order, 2, headLocked = true, random = Random(0)))
    }

    // ---- Simulations ------------------------------------------------------------------------

    /**
     * Mirrors FriendDao's transactions over a pure in-memory rotation, so long random runs can
     * check the owner's rules end to end.
     */
    private class World(seed: Int) {
        val random = Random(seed)
        var order: List<Slot> = emptyList()
        var today = 0
        var nextId = 1L
        val log = mutableListOf<Triple<Int, Long, Boolean>>() // day, friend, scheduled
        /** Today's scheduled call, if made. */
        var scheduledToday: Long? = null

        val headLocked get() = scheduledToday == null
        private fun date(d: Int) = LocalDate.of(2026, 1, 1).plusDays(d.toLong())
        private fun lastCalled(id: Long) = log.filter { it.second == id }.maxOfOrNull { it.first }?.let(::date)
        private fun refreshDates() {
            order = order.map { it.copy(lastCalled = lastCalled(it.id)) }
        }

        fun add(): Long {
            val id = nextId++
            order = Rotation.addFriend(order, id, headLocked, random)
            return id
        }

        fun remove(id: Long) {
            order = Rotation.afterRemoval(order.filter { it.id != id })
        }

        /** Mirrors FriendDao.recordCall. Returns whether a scheduled call was recorded. */
        fun call(id: Long): Boolean {
            val isFocus = headLocked && order.first().id == id
            if (log.any { it.first == today && it.second == id }) return false
            log += Triple(today, id, isFocus)
            if (isFocus) scheduledToday = id
            order = Rotation.afterCall(order, id, date(today), random)
            return isFocus
        }

        fun reshuffle() {
            order = Rotation.reshuffle(order, date(today), random)
        }

        fun nextDay() {
            today++
            scheduledToday = null
        }

        fun checkInvariants() {
            val ids = order.map { it.id }
            assertEquals("unique", ids.size, ids.toSet().size)
            val waiting = Rotation.waitingCount(order)
            assertTrue("waiting prefix", order.take(waiting).none { it.turnUsed })
            if (order.isNotEmpty()) assertTrue("someone waiting", waiting > 0)
            // From index 1, never-called friends come before any waiting repeat.
            val tail = order.take(waiting).drop(1)
            val firstRepeat = tail.indexOfFirst { it.everCalled }
            if (firstRepeat >= 0) assertTrue("fresh before repeats", tail.drop(firstRepeat).all { it.everCalled })
            refreshDates()
        }
    }

    @Test
    fun `scheduled calls only - every pass calls everyone once, with spacing`() {
        for (n in 1..12) {
            for (seed in 0 until 20) {
                val world = World(seed)
                repeat(n) { world.add() }
                val calls = mutableListOf<Long>()
                repeat(n * 12) {
                    val head = world.order.first().id
                    world.call(head)
                    calls += head
                    world.checkInvariants()
                    world.nextDay()
                }
                calls.chunked(n).forEach { pass -> assertEquals("n=$n seed=$seed", n, pass.toSet().size) }
                val space = Rotation.spacing(n)
                val lastIndex = mutableMapOf<Long, Int>()
                calls.forEachIndexed { i, id ->
                    lastIndex[id]?.let { prev -> assertTrue("gap n=$n", i - prev - 1 >= space) }
                    lastIndex[id] = i
                }
            }
        }
    }

    @Test
    fun `missed days keep today's friend until called`() {
        val world = World(4)
        repeat(5) { world.add() }
        val head = world.order.first().id
        repeat(3) {
            world.nextDay() // nobody called
            assertEquals(head, world.order.first().id)
        }
        assertTrue(world.call(head))
    }

    @Test
    fun `random life - new friends always beat repeats and the rules always hold`() {
        for (seed in 0 until 60) {
            val world = World(seed)
            repeat(1 + seed % 6) { world.add() }
            // New friend -> today's locked head when they were added (the only one allowed first).
            val pendingNew = mutableMapOf<Long, Long?>()
            val everCalled = mutableSetOf<Long>()

            repeat(600) {
                val r = world.random.nextDouble()
                val lockedHeadBefore = world.order.firstOrNull()?.id
                when {
                    r < 0.08 -> {
                        val exempt = if (world.headLocked) lockedHeadBefore else null
                        val id = world.add()
                        pendingNew[id] = exempt
                        if (world.headLocked && lockedHeadBefore != null) {
                            assertEquals("add moved a locked head", lockedHeadBefore, world.order.first().id)
                        }
                    }
                    r < 0.11 && world.order.size > 1 -> {
                        val victim = world.order.random(world.random).id
                        world.remove(victim)
                        pendingNew.remove(victim)
                        pendingNew.replaceAll { _, exempt -> exempt?.takeIf { it != victim } }
                    }
                    r < 0.16 && world.order.size > 1 -> {
                        // Off-schedule call of someone other than today's friend.
                        val other = world.order.drop(1).random(world.random).id
                        world.call(other)
                        if (world.headLocked && lockedHeadBefore != null) {
                            assertEquals("off-schedule call moved today's friend", lockedHeadBefore, world.order.first().id)
                        }
                        everCalled += other
                        pendingNew.remove(other)
                    }
                    r < 0.18 -> world.reshuffle()
                    r < 0.30 -> world.nextDay() // a missed day
                    world.order.isNotEmpty() && world.headLocked -> {
                        val head = world.order.first().id
                        if (head in everCalled) {
                            pendingNew.forEach { (newId, exempt) ->
                                if (exempt != head) fail("seed $seed: repeat call to $head before new friend $newId")
                            }
                        }
                        world.call(head)
                        everCalled += head
                        pendingNew.remove(head)
                        world.nextDay()
                    }
                    else -> world.nextDay()
                }
                world.checkInvariants()
            }
        }
    }
}
