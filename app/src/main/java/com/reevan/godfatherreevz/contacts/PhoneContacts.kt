package com.reevan.godfatherreevz.contacts

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
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
    /** The contact's full-size photo (a contacts-provider URI), if they have one. */
    val photo: String? = null,
    /** A small version of [photo], for lists. */
    val thumbnail: String? = null,
)

data class ContactNumber(val number: String, val label: String)

/** A phonebook contact with at least one number; the first number is the one to suggest. */
data class PhoneContact(
    val key: String,
    val name: String,
    val numbers: List<ContactNumber>,
    val photo: String? = null,
    val thumbnail: String? = null,
)

/**
 * The photo of the contact with [phone]'s number (by [PhoneNumbers.sameNumber], the rule that
 * spots duplicates), or null if no contact with a photo has it.
 */
fun contactPhotoFor(phone: String, contacts: List<PhoneContact>): String? =
    contacts.firstOrNull { contact ->
        contact.photo != null && contact.numbers.any { PhoneNumbers.sameNumber(it.number, phone) }
    }?.photo

/**
 * WhatsApp's "Voice call" entry, which it adds to every phonebook contact on WhatsApp (while its
 * contact sync is on). Viewing it with WhatsApp starts a voice call.
 */
const val WHATSAPP_VOICE_CALL = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"

/** A contacts-provider data row: a phone number ([value] = the number) or [WHATSAPP_VOICE_CALL]. */
data class DataRow(val id: Long, val contactId: Long, val mimeType: String, val value: String)

/**
 * The id of WhatsApp's voice-call entry for [phone]: the one for that number (its value is
 * "919876543210@s.whatsapp.net"), else one on a contact that has the number. Numbers match by
 * [PhoneNumbers.sameNumber]. Null if there is none — not on WhatsApp, not in the phonebook, or
 * WhatsApp's contact sync is off.
 */
fun whatsAppCallFor(phone: String, rows: List<DataRow>): Long? {
    val calls = rows.filter { it.mimeType == WHATSAPP_VOICE_CALL }
    calls.firstOrNull { PhoneNumbers.sameNumber(it.value.substringBefore('@'), phone) }?.let { return it.id }
    val contacts = rows
        .filter { it.mimeType == Phone.CONTENT_ITEM_TYPE && PhoneNumbers.sameNumber(it.value, phone) }
        .map { it.contactId }
        .toSet()
    return calls.firstOrNull { it.contactId in contacts }?.id
}

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
                photo = numbers.firstNotNullOfOrNull { it.photo },
                thumbnail = numbers.firstNotNullOfOrNull { it.thumbnail },
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
            Phone.PHOTO_URI,
            Phone.PHOTO_THUMBNAIL_URI,
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
                    photo = cursor.string(Phone.PHOTO_URI),
                    thumbnail = cursor.string(Phone.PHOTO_THUMBNAIL_URI),
                )
            }
        }
        return groupContacts(rows)
    }

    /**
     * The phonebook photo for [phone]'s number ([contactPhotoFor]); null if there's none, or no
     * contacts access. Call off the main thread.
     */
    fun photoFor(context: Context, phone: String): String? =
        if (hasPermission(context)) contactPhotoFor(phone, loadAll(context)) else null

    /**
     * WhatsApp's voice-call entry for [phone] ([whatsAppCallFor]) as a data URI; null if there's
     * none, or no contacts access. Call off the main thread.
     */
    fun findWhatsAppCall(context: Context, phone: String): Uri? =
        if (hasPermission(context)) queryWhatsAppCall(context.contentResolver, phone) else null

    /** [findWhatsAppCall] without the permission check, for a test reading with a borrowed one. */
    internal fun queryWhatsAppCall(resolver: ContentResolver, phone: String): Uri? {
        val projection = arrayOf(Data._ID, Data.CONTACT_ID, Data.MIMETYPE, Data.DATA1)
        val rows = mutableListOf<DataRow>()
        resolver.query(
            Data.CONTENT_URI,
            projection,
            "${Data.MIMETYPE} IN (?, ?)",
            arrayOf(Phone.CONTENT_ITEM_TYPE, WHATSAPP_VOICE_CALL),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                rows += DataRow(
                    id = cursor.long(Data._ID),
                    contactId = cursor.long(Data.CONTACT_ID),
                    mimeType = cursor.string(Data.MIMETYPE).orEmpty(),
                    value = cursor.string(Data.DATA1).orEmpty(),
                )
            }
        }
        return whatsAppCallFor(phone, rows)?.let { ContentUris.withAppendedId(Data.CONTENT_URI, it) }
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
            // The classic picker: a single phone-number data row. Its photo can usually only be
            // read with contacts access — the grant covers just this row — so it is a bonus.
            val projection = arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER, Phone.PHOTO_URI)
            context.contentResolver.query(uri, projection, null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        rows += ContactRow(
                            key = uri.toString(),
                            name = cursor.string(Phone.DISPLAY_NAME).orEmpty(),
                            number = cursor.string(Phone.NUMBER).orEmpty(),
                            photo = cursor.string(Phone.PHOTO_URI),
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

    private fun Cursor.long(column: String): Long =
        getColumnIndex(column).takeIf { it >= 0 }?.let { if (isNull(it)) 0L else getLong(it) } ?: 0L
}
