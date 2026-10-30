package com.reevan.godfatherreevz.contacts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.provider.ContactsPickerSessionContract
import androidx.core.content.ContextCompat
import com.reevan.godfatherreevz.util.PhoneNumbers
import java.text.Collator

/** One phone number as the contacts provider returns it. */
data class ContactRow(
    /** Groups a contact's numbers together (the contact's lookup key). */
    val key: String,
    val name: String,
    val number: String,
    /** "Mobile", "Work", ... */
    val label: String = "",
    /** The contact's default number. */
    val primary: Boolean = false,
)

data class ContactNumber(val number: String, val label: String)

/** A phonebook contact with at least one number; the first number is the one to suggest. */
data class PhoneContact(val key: String, val name: String, val numbers: List<ContactNumber>)

/**
 * Groups provider rows into contacts: one per key, numbers that reach the same phone merged (the
 * provider often lists "+91 98765 43210" and "098765 43210" separately), the default number
 * first, contacts in alphabetical order for the phone's language. A contact with no name is
 * listed under its number.
 */
fun groupContacts(rows: List<ContactRow>, collator: Collator = Collator.getInstance()): List<PhoneContact> =
    rows.filter { it.number.isNotBlank() }
        .groupBy { it.key }
        .map { (key, numbers) ->
            val ordered = numbers.sortedByDescending { it.primary }
            val distinct = mutableListOf<ContactRow>()
            for (row in ordered) {
                if (distinct.none { PhoneNumbers.sameNumber(it.number, row.number) }) distinct += row
            }
            val name = numbers.firstOrNull { it.name.isNotBlank() }?.name?.trim()
                ?: PhoneNumbers.clean(distinct.first().number)
            PhoneContact(
                key = key,
                name = name,
                numbers = distinct.map { ContactNumber(PhoneNumbers.clean(it.number), it.label) },
            )
        }
        .sortedWith(compareBy(collator) { it.name })

/** The phone's contacts, read through the contacts provider. */
object PhoneContacts {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /** Every contact with a phone number. Needs READ_CONTACTS; call off the main thread. */
    fun loadAll(context: Context): List<PhoneContact> {
        val projection = arrayOf(
            Phone.LOOKUP_KEY,
            Phone.DISPLAY_NAME_PRIMARY,
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL,
            Phone.IS_SUPER_PRIMARY,
        )
        val rows = mutableListOf<ContactRow>()
        context.contentResolver.query(Phone.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val type = cursor.int(Phone.TYPE)
                rows += ContactRow(
                    key = cursor.string(Phone.LOOKUP_KEY).orEmpty(),
                    name = cursor.string(Phone.DISPLAY_NAME_PRIMARY).orEmpty(),
                    number = cursor.string(Phone.NUMBER).orEmpty(),
                    label = Phone.getTypeLabel(context.resources, type, cursor.string(Phone.LABEL)).toString(),
                    primary = cursor.int(Phone.IS_SUPER_PRIMARY) != 0,
                )
            }
        }
        return groupContacts(rows)
    }

    /**
     * The system contact picker, which needs no permission (the fallback when READ_CONTACTS is
     * refused). On Android 17+ it is the new privacy picker with multi-select; before that, the
     * classic one-number-at-a-time picker.
     */
    fun systemPickerIntent(): Intent =
        if (Build.VERSION.SDK_INT >= 37) {
            Intent(ContactsPickerSessionContract.ACTION_PICK_CONTACTS).apply {
                putExtra(Intent.EXTRA_USE_SYSTEM_CONTACTS_PICKER, true)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                putExtra(ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT, 100)
                putStringArrayListExtra(
                    ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS,
                    arrayListOf(Phone.CONTENT_ITEM_TYPE),
                )
                putExtra(ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_MATCH_ALL_DATA_FIELDS, true)
            }
        } else {
            Intent(Intent.ACTION_PICK, Phone.CONTENT_URI)
        }

    /**
     * What the system picker returned: one number per picked contact. The URI grants temporary
     * read access to just those contacts, so read it straight away (call off the main thread).
     */
    fun readPicked(context: Context, uri: Uri): List<PhoneContact> {
        val rows = mutableListOf<ContactRow>()
        if (Build.VERSION.SDK_INT >= 37 && uri.authority == ContactsPickerSessionContract.AUTHORITY) {
            // A picker session: ContactsContract.Data rows. It rejects selection arguments.
            val projection = arrayOf(Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME_PRIMARY, Data.MIMETYPE, Data.DATA1)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.string(Data.MIMETYPE) != Phone.CONTENT_ITEM_TYPE) continue
                    rows += ContactRow(
                        key = cursor.string(Contacts.LOOKUP_KEY).orEmpty(),
                        name = cursor.string(Contacts.DISPLAY_NAME_PRIMARY).orEmpty(),
                        number = cursor.string(Data.DATA1).orEmpty(),
                    )
                }
            }
        } else {
            // The classic picker: a single phone-number data row.
            context.contentResolver.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        rows += ContactRow(
                            key = uri.toString(),
                            name = cursor.string(Phone.DISPLAY_NAME).orEmpty(),
                            number = cursor.string(Phone.NUMBER).orEmpty(),
                        )
                    }
                }
        }
        return groupContacts(rows)
    }

    private fun Cursor.string(column: String): String? =
        getColumnIndex(column).takeIf { it >= 0 }?.let { if (isNull(it)) null else getString(it) }

    private fun Cursor.int(column: String): Int =
        getColumnIndex(column).takeIf { it >= 0 }?.let { if (isNull(it)) 0 else getInt(it) } ?: 0
}
