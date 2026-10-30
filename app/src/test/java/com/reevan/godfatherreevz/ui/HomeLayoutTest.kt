package com.reevan.godfatherreevz.ui

import com.reevan.godfatherreevz.data.Call
import com.reevan.godfatherreevz.data.Friend
import com.reevan.godfatherreevz.data.FriendWithLastCall
import com.reevan.godfatherreevz.ui.home.PAST_CARDS
import com.reevan.godfatherreevz.ui.home.buildHome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeLayoutTest {

    private val today = LocalDate.of(2026, 10, 30)
    private fun day(offset: Long) = today.plusDays(offset)

    private fun friend(id: Long, lastCalled: LocalDate? = null) = FriendWithLastCall(
        Friend(id, "F$id", "98765 4321$id", addedOn = day(-30), createdAt = id, position = id.toInt()),
        lastCalled,
    )

    private var nextCallId = 1L
    private fun call(friendId: Long?, date: LocalDate, scheduled: Boolean = true) =
        Call(nextCallId++, friendId, "F$friendId", date, scheduled, consumedTurn = true)

    @Test
    fun `no friends and no call today is empty`() {
        val home = buildHome(emptyList(), emptyList(), null)
        assertTrue(home.isEmpty)
        assertNull(home.today)
    }

    @Test
    fun `before today's call the head is in focus and everyone else follows in order`() {
        val home = buildHome(listOf(friend(3), friend(1), friend(2)), emptyList(), null)
        assertEquals(3L, home.today!!.friendId)
        assertFalse(home.today!!.done)
        assertEquals(listOf(1L, 2L), home.upcoming.map { it.friendId })
        assertTrue(home.past.isEmpty())
    }

    @Test
    fun `after today's call that friend stays in focus, done`() {
        // Friend 3 was called today and has moved to the back of the order.
        val scheduled = call(3, today)
        val home = buildHome(listOf(friend(1), friend(2), friend(3, today)), listOf(scheduled), scheduled)
        assertEquals(3L, home.today!!.friendId)
        assertTrue(home.today!!.done)
        assertEquals(today, home.today!!.lastCalled)
        assertEquals(listOf(1L, 2L), home.upcoming.map { it.friendId })
        assertTrue("today's friend is not also in the past", home.past.isEmpty())
    }

    @Test
    fun `today's call to a friend since removed shows their saved name`() {
        val scheduled = Call(9, null, "Gone Friend", today, scheduled = true, consumedTurn = true)
        val home = buildHome(listOf(friend(1), friend(2)), listOf(scheduled), scheduled)
        assertNull(home.today!!.friendId)
        assertEquals("Gone Friend", home.today!!.name)
        assertTrue(home.today!!.done)
        assertEquals(listOf(1L, 2L), home.upcoming.map { it.friendId })
    }

    @Test
    fun `the past shows recent calls, one card per friend, oldest first`() {
        val calls = listOf( // newest first, as the DAO returns them
            call(2, day(-1)),
            call(4, day(-2)),
            call(2, day(-5)),
            call(null, day(-6)), // a removed friend
            call(5, day(-7)),
        )
        val home = buildHome((1L..5L).map { friend(it) }, calls, null)
        assertEquals(listOf(5L, 4L, 2L), home.past.map { it.friendId })
        assertEquals(listOf(day(-7), day(-2), day(-1)), home.past.map { it.date })
    }

    @Test
    fun `the past is capped`() {
        val calls = (1L..12L).map { call(it, day(-it)) }
        // Friend 13, never called, is today's; the other twelve were called on recent days.
        val home = buildHome(listOf(friend(13)) + (1L..12L).map { friend(it) }, calls, null)
        assertEquals(PAST_CARDS, home.past.size)
        assertEquals(day(-1), home.past.last().date)
    }

    @Test
    fun `every friend appears exactly once in focus plus upcoming`() {
        val friends = (1L..9L).map { friend(it) }
        val home = buildHome(friends, emptyList(), null)
        val shown = listOfNotNull(home.today?.friendId) + home.upcoming.map { it.friendId }
        assertEquals(friends.map { it.friend.id }.toSet(), shown.toSet())
        assertEquals(friends.size, shown.size)
    }

    @Test
    fun `every card shows its friend's photo`() {
        fun withPhoto(id: Long, lastCalled: LocalDate? = null) =
            friend(id, lastCalled).let { it.copy(friend = it.friend.copy(photo = "p$id.jpg")) }
        val friends = listOf(withPhoto(1), withPhoto(2, day(-1)), friend(3))
        val home = buildHome(friends, listOf(call(2, day(-1))), null)
        assertEquals("p1.jpg", home.today!!.photo)
        assertEquals(listOf("p2.jpg", null), home.upcoming.map { it.photo })
        assertEquals(listOf("p2.jpg"), home.past.map { it.photo })

        // Today's call made to a friend since removed: their saved name, and no photo.
        val gone = buildHome(friends, emptyList(), call(99, today))
        assertNull(gone.today!!.photo)
    }
}
