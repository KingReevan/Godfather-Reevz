package com.reevan.godfatherreevz.contacts

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.Collator
import java.util.Locale

class GroupContactsTest {

    private val collator = Collator.getInstance(Locale.UK)

    @Test
    fun `a contact's numbers are grouped, default first, duplicates merged`() {
        val contacts = groupContacts(
            listOf(
                ContactRow("m", "Marco Rossi", "098765 43210", "Mobile"),
                ContactRow("m", "Marco Rossi", "+91 22 2345 6789", "Work", primary = true),
                ContactRow("m", "Marco Rossi", "+91 98765 43210", "Mobile"),
            ),
            collator,
        )
        assertEquals(1, contacts.size)
        assertEquals(
            listOf(ContactNumber("+91 22 2345 6789", "Work"), ContactNumber("098765 43210", "Mobile")),
            contacts.single().numbers,
        )
    }

    @Test
    fun `contacts are sorted alphabetically ignoring case`() {
        val names = groupContacts(
            listOf(
                ContactRow("1", "zoe", "111"),
                ContactRow("2", "Aditya", "222"),
                ContactRow("3", "émile", "333"),
                ContactRow("4", "Bianca", "444"),
            ),
            collator,
        ).map { it.name }
        assertEquals(listOf("Aditya", "Bianca", "émile", "zoe"), names)
    }

    @Test
    fun `blank numbers are dropped and nameless contacts use their number`() {
        val contacts = groupContacts(
            listOf(
                ContactRow("1", "Empty", "  "),
                ContactRow("2", "", "  +1 555 0100 "),
            ),
            collator,
        )
        assertEquals(listOf("+1 555 0100"), contacts.map { it.name })
    }
}
