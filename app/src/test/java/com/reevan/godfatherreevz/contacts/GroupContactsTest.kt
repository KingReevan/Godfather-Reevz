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

    @Test
    fun `a contact keeps their photo`() {
        val contact = groupContacts(
            listOf(
                ContactRow("m", "Marco Rossi", "98765 43210", photo = "content://photo/1", thumbnail = "content://thumb/1"),
                ContactRow("m", "Marco Rossi", "+91 22 2345 6789"),
            ),
            collator,
        ).single()
        assertEquals("content://photo/1", contact.photo)
        assertEquals("content://thumb/1", contact.thumbnail)
    }

    @Test
    fun `a friend's contact photo is found by their number, however it is written`() {
        val contacts = groupContacts(
            listOf(
                ContactRow("a", "Ana", "+91 98765 43210"),
                ContactRow("m", "Marco", "+91 22 2345 6789", photo = "content://photo/m"),
                ContactRow("m", "Marco", "098765 11111", photo = "content://photo/m"),
                ContactRow("n", "Neha", "98200 11122", photo = "content://photo/n"),
            ),
            collator,
        )
        assertEquals("content://photo/m", contactPhotoFor("+91 98765 11111", contacts))
        assertEquals("content://photo/n", contactPhotoFor("+919820011122", contacts))
        // Ana is in the phonebook, but without a photo; nobody has this other number.
        assertEquals(null, contactPhotoFor("98765 43210", contacts))
        assertEquals(null, contactPhotoFor("90000 00000", contacts))
    }

    private fun number(id: Long, contact: Long, number: String) = DataRow(id, contact, PHONE, number)
    private fun whatsApp(id: Long, contact: Long, jid: String) = DataRow(id, contact, WHATSAPP_VOICE_CALL, jid)

    @Test
    fun `a friend's WhatsApp call is found by its WhatsApp number, however theirs is written`() {
        val rows = listOf(
            number(1, contact = 10, "+91 98765 43210"),
            whatsApp(2, contact = 10, "919876543210@s.whatsapp.net"),
            number(3, contact = 20, "098200 11122"),
            whatsApp(4, contact = 20, "919820011122@s.whatsapp.net"),
        )
        assertEquals(2L, whatsAppCallFor("98765 43210", rows))
        assertEquals(4L, whatsAppCallFor("+91 98200 11122", rows))
    }

    @Test
    fun `without a matching WhatsApp number, the call on the contact with the friend's number`() {
        // WhatsApp's entry doesn't spell out the number (a format it may change to).
        val rows = listOf(
            number(1, contact = 10, "+91 98765 43210"),
            whatsApp(2, contact = 10, "abc123@lid"),
            whatsApp(3, contact = 30, "xyz789@lid"),
        )
        assertEquals(2L, whatsAppCallFor("98765 43210", rows))
    }

    @Test
    fun `no WhatsApp call when the number isn't on WhatsApp or in the phonebook`() {
        val rows = listOf(
            number(1, contact = 10, "+91 98765 43210"), // in the phonebook, not on WhatsApp
            number(3, contact = 20, "98200 11122"),
            whatsApp(4, contact = 20, "919820011122@s.whatsapp.net"),
        )
        assertEquals(null, whatsAppCallFor("98765 43210", rows))
        assertEquals(null, whatsAppCallFor("90000 00000", rows))
        assertEquals(null, whatsAppCallFor("98765 43210", emptyList()))
    }

    private companion object {
        /** `Phone.CONTENT_ITEM_TYPE`. */
        const val PHONE = "vnd.android.cursor.item/phone_v2"
    }
}
