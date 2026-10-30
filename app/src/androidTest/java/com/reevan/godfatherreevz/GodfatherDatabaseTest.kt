package com.reevan.godfatherreevz

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reevan.godfatherreevz.data.CallOutcome
import com.reevan.godfatherreevz.data.FriendDao
import com.reevan.godfatherreevz.data.GodfatherDatabase
import com.reevan.godfatherreevz.data.NewFriend
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.random.Random

/** The rotation transactions against a real (in-memory) Room database. */
@RunWith(AndroidJUnit4::class)
class GodfatherDatabaseTest {

    private lateinit var db: GodfatherDatabase
    private lateinit var friends: FriendDao
    private val today = LocalDate.of(2026, 10, 29)
    private val random = Random(42)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GodfatherDatabase::class.java,
        ).build()
        friends = db.friendDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun add(vararg names: String): List<Long> =
        friends.addFriends(names.map { NewFriend(it, "98765 4321${names.indexOf(it)}") }, today, 0L, random)

    private suspend fun order() = friends.observeInCallOrder().first()
    private suspend fun head() = order().first().friend.id

    @Test
    fun positionsAreAPermutationAfterAdding() = runBlocking {
        add("A", "B", "C", "D", "E")
        assertEquals((0..4).toList(), order().map { it.friend.position })
    }

    @Test
    fun callingTodaysFriendRecordsAScheduledCallAndMovesThemBack() = runBlocking {
        add("A", "B", "C", "D")
        val head = head()
        assertEquals(CallOutcome.RECORDED, friends.recordCall(head, today, random))
        val call = db.callDao().observeCallOn(head, today).first()!!
        assertTrue(call.scheduled)
        assertTrue(call.consumedTurn)
        assertNotEquals(head, head())
        assertEquals(today, order().first { it.friend.id == head }.lastCalled)
    }

    @Test
    fun redialingTheSameDayDoesNotCountTwice() = runBlocking {
        add("A", "B", "C")
        val head = head()
        friends.recordCall(head, today, random)
        val orderAfterFirst = order()
        assertEquals(CallOutcome.ALREADY_RECORDED, friends.recordCall(head, today, random))
        assertEquals(1, db.callDao().observeAll().first().size)
        assertEquals(orderAfterFirst, order())
    }

    @Test
    fun anOffScheduleCallKeepsTodaysFriendAndIsNotScheduled() = runBlocking {
        add("A", "B", "C", "D")
        val head = head()
        val other = order()[2].friend.id
        friends.recordCall(other, today, random)
        assertEquals(head, head())
        assertFalse(db.callDao().observeCallOn(other, today).first()!!.scheduled)
        // Today's call is still to make.
        assertNull(db.callDao().observeScheduledBetween(today, today.plusDays(2)).first())
    }

    @Test
    fun onlyOneScheduledCallPerDay() = runBlocking {
        add("A", "B", "C")
        friends.recordCall(head(), today, random)
        val tomorrowsHead = head()
        friends.recordCall(tomorrowsHead, today, random) // calling ahead: off-schedule
        assertFalse(db.callDao().observeCallOn(tomorrowsHead, today).first()!!.scheduled)
    }

    @Test
    fun undoingTodaysCallPutsTheFriendBackAtTheHead() = runBlocking {
        add("A", "B", "C", "D")
        val head = head()
        friends.recordCall(head, today, random)
        assertTrue(friends.undoCall(head, today, random))
        assertEquals(head, head())
        assertNull(order().first().lastCalled)
        assertTrue(db.callDao().observeAll().first().isEmpty())
        // A second Undo finds nothing to undo.
        assertFalse(friends.undoCall(head, today, random))
    }

    @Test
    fun removingAFriendKeepsTheirCallsWithTheNameSnapshot() = runBlocking {
        add("A", "B", "C")
        val head = head()
        val name = order().first().friend.name
        friends.recordCall(head, today, random)
        friends.removeFriends(listOf(head))
        val call = db.callDao().observeAll().first().single()
        assertNull(call.friendId)
        assertEquals(name, call.friendName)
        assertEquals((0..1).toList(), order().map { it.friend.position })
        // Today's call stays done even though that friend is gone.
        assertTrue(db.callDao().observeScheduledBetween(today, today.plusDays(2)).first()!!.scheduled)
    }

    @Test
    fun editingDetailsLeavesHistoryAndPlaceAlone() = runBlocking {
        add("A", "B", "C")
        val head = head()
        friends.recordCall(head, today, random)
        val before = order().first { it.friend.id == head }
        friends.updateDetails(head, "Renamed", "+1 555 0100")
        val after = order().first { it.friend.id == head }
        assertEquals(before.friend.position, after.friend.position)
        assertEquals(before.lastCalled, after.lastCalled)
        assertEquals(head, db.callDao().observeAll().first().single().friendId)
    }

    @Test
    fun aNewFriendAddedWhileTodayIsLockedNeverBecomesTodaysFriend() = runBlocking {
        add("A", "B", "C")
        val head = head()
        repeat(10) { add("New$it") }
        assertEquals(head, head())
    }

    @Test
    fun aFriendWhoBecomesTodaysAfterAnEarlierCallTodayCountsAsDone() = runBlocking {
        // [A, B]: call B off-schedule, then remove A — B becomes today's friend, already called.
        val (a, b) = add("A", "B").let { ids -> order().map { it.friend.id }.also { assertEquals(ids.toSet(), it.toSet()) } }
        friends.recordCall(b, today, random)
        friends.removeFriends(listOf(a))
        assertEquals(b, head())
        assertEquals(CallOutcome.ALREADY_RECORDED, friends.recordCall(b, today, random))
        assertTrue(db.callDao().observeCallOn(b, today).first()!!.scheduled)
    }
}
