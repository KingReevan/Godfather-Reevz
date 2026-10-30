package com.reevan.godfatherreevz.ui

import com.reevan.godfatherreevz.data.Friend
import com.reevan.godfatherreevz.ui.common.filterFriends
import com.reevan.godfatherreevz.ui.friendform.FriendDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FriendEditsTest {

    private val marco = Friend(1, "Marco Rossi", "+91 98765 43210", addedOn = LocalDate.of(2026, 10, 1), createdAt = 0, position = 0)

    @Test
    fun `unchanged or only tidied is no change`() {
        assertFalse(FriendDraft("Marco Rossi", "+91 98765 43210").changes(marco))
        assertFalse(FriendDraft(" marco  rossi ", "+91 98765 43210 ").changes(marco))
    }

    @Test
    fun `a new name or number is a change`() {
        assertTrue(FriendDraft("Marco Rossi Jr", "+91 98765 43210").changes(marco))
        assertTrue(FriendDraft("Marco Rossi", "+91 98765 43211").changes(marco))
        // Same number written differently is still a change of what is shown and dialled.
        assertTrue(FriendDraft("Marco Rossi", "098765 43210").changes(marco))
    }

    @Test
    fun `friend lists search by name or number`() {
        val friends = listOf(marco, marco.copy(id = 2, name = "Émile Zola", phone = "+33 6 12 34 56 78"))
        assertEquals(listOf(2L), filterFriends(friends, "EMIL").map { it.id })
        assertEquals(listOf(1L), filterFriends(friends, "43210").map { it.id })
        assertEquals(friends, filterFriends(friends, ""))
    }

    @Test
    fun `a new or removed photo is a change`() {
        assertFalse(FriendDraft("Marco Rossi", "+91 98765 43210", photo = null).changes(marco))
        assertTrue(FriendDraft("Marco Rossi", "+91 98765 43210", photo = "a.jpg").changes(marco))
        val withPhoto = marco.copy(photo = "a.jpg")
        assertFalse(FriendDraft("Marco Rossi", "+91 98765 43210", photo = "a.jpg").changes(withPhoto))
        assertTrue(FriendDraft("Marco Rossi", "+91 98765 43210", photo = "b.jpg").changes(withPhoto))
        assertTrue(FriendDraft("Marco Rossi", "+91 98765 43210", photo = null).changes(withPhoto))
        assertEquals("b.jpg", FriendDraft("marco rossi", "98765 43210", photo = "b.jpg").toNewFriend().photo)
    }
}
