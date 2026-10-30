package com.reevan.godfatherreevz.ui

import com.reevan.godfatherreevz.contacts.ContactNumber
import com.reevan.godfatherreevz.contacts.PhoneContact
import com.reevan.godfatherreevz.data.Friend
import com.reevan.godfatherreevz.data.NewFriend
import com.reevan.godfatherreevz.ui.addfriend.AddResult
import com.reevan.godfatherreevz.ui.addfriend.planAdd
import com.reevan.godfatherreevz.ui.contactpicker.filterContacts
import com.reevan.godfatherreevz.ui.contactpicker.sectionOf
import com.reevan.godfatherreevz.ui.friendform.FriendDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ContactImportTest {

    private fun contact(name: String, vararg numbers: String) =
        PhoneContact(name, name, numbers.map { ContactNumber(it, "Mobile") })

    private val book = listOf(
        contact("Émile Zola", "+33 6 12 34 56 78"),
        contact("Marco Rossi", "+91 98765 43210", "022 2345 6789"),
        contact("Neha Joseph", "98200 11122"),
    )

    private fun friend(id: Long, name: String, phone: String) =
        Friend(id, name, phone, addedOn = LocalDate.of(2026, 10, 1), createdAt = 0, position = 0)

    @Test
    fun `search matches names ignoring case and accents`() {
        assertEquals(listOf("Émile Zola"), filterContacts(book, "emile").map { it.name })
        assertEquals(listOf("Marco Rossi"), filterContacts(book, "ROSS").map { it.name })
        assertEquals(book, filterContacts(book, "  "))
    }

    @Test
    fun `search matches numbers from three digits`() {
        assertEquals(listOf("Marco Rossi"), filterContacts(book, "6789").map { it.name })
        assertEquals(emptyList<PhoneContact>(), filterContacts(book, "98"))
    }

    @Test
    fun `contacts are filed under their first letter`() {
        assertEquals('E', sectionOf("Émile"))
        assertEquals('M', sectionOf(" marco"))
        assertEquals('#', sectionOf("+91 98765"))
        assertEquals('#', sectionOf(""))
    }

    @Test
    fun `planAdd skips numbers already saved and repeats within the batch`() {
        val existing = listOf(friend(1, "Marco Rossi", "098765 43210"))
        val (toAdd, skipped) = planAdd(
            listOf(
                NewFriend("Marco R", "+91 98765 43210"),
                NewFriend("Neha Joseph", "98200 11122"),
                NewFriend("Neha (work)", "+91 98200 11122"),
            ),
            existing,
        )
        assertEquals(listOf("Neha Joseph"), toAdd.map { it.name })
        assertEquals(listOf("Marco Rossi", "Neha Joseph"), skipped)
    }

    @Test
    fun `add results read naturally`() {
        assertEquals("Marco Rossi added", AddResult(listOf("Marco Rossi"), emptyList()).message())
        assertEquals("3 friends added · 1 already saved", AddResult(listOf("a", "b", "c"), listOf("d")).message())
        assertEquals("Already saved as Marco Rossi", AddResult(emptyList(), listOf("Marco Rossi")).message())
        assertEquals("2 already saved", AddResult(emptyList(), listOf("a", "b")).message())
    }

    @Test
    fun `a draft needs a name and a valid, unsaved number`() {
        val existing = listOf(friend(1, "Marco Rossi", "+91 98765 43210"))
        assertFalse(FriendDraft("", "98200 11122").check(existing).canSave)
        assertNull(FriendDraft("Neha", "").check(existing).phoneError)
        assertEquals("Enter a valid phone number", FriendDraft("Neha", "12").check(existing).phoneError)
        assertEquals(
            "Already saved as Marco Rossi",
            FriendDraft("Marco", "9876543210").check(existing).phoneError,
        )
        assertTrue(FriendDraft("Neha", "98200 11122").check(existing).canSave)
        // Editing Marco may keep his own number.
        assertTrue(FriendDraft("Marco", "9876543210").check(existing, exceptId = 1).canSave)
    }

    @Test
    fun `a draft is saved tidy`() {
        val saved = FriendDraft("  marco   rossi ", "  +91  98765 43210 ").toNewFriend()
        assertEquals(NewFriend("Marco Rossi", "+91 98765 43210"), saved)
    }
}
