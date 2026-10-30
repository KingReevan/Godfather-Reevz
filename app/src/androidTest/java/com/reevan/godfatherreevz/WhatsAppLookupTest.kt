package com.reevan.godfatherreevz

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.reevan.godfatherreevz.contacts.PhoneContacts
import com.reevan.godfatherreevz.contacts.WHATSAPP_VOICE_CALL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Finding WhatsApp's voice-call entry through the real contacts provider: a contact made here,
 * with a number and the entry WhatsApp would add, then deleted. The app's own contacts permission
 * is never granted (that would change other tests' flows): the test borrows the shell's for its
 * duration. Run on the emulator, like every instrumented test (see CLAUDE.md).
 */
@RunWith(AndroidJUnit4::class)
class WhatsAppLookupTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val resolver = context.contentResolver
    private val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    private var rawContact = -1L

    @Before
    fun setUp() {
        automation.adoptShellPermissionIdentity(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
    }

    @After
    fun tearDown() {
        try {
            if (rawContact >= 0) {
                val uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContact).buildUpon()
                    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true") // gone, not just hidden
                    .build()
                resolver.delete(uri, null, null)
            }
        } finally {
            automation.dropShellPermissionIdentity()
        }
    }

    private fun insertData(mimeType: String, value: String): Long {
        val values = ContentValues().apply {
            put(Data.RAW_CONTACT_ID, rawContact)
            put(Data.MIMETYPE, mimeType)
            put(Data.DATA1, value)
        }
        return ContentUris.parseId(resolver.insert(Data.CONTENT_URI, values)!!)
    }

    @Test
    fun findsTheVoiceCallForTheNumber_howeverItIsWritten() {
        rawContact = ContentUris.parseId(resolver.insert(RawContacts.CONTENT_URI, ContentValues())!!)
        insertData(Phone.CONTENT_ITEM_TYPE, "+91 98765 43210")
        val call = insertData(WHATSAPP_VOICE_CALL, "919876543210@s.whatsapp.net")

        assertEquals(ContentUris.withAppendedId(Data.CONTENT_URI, call), PhoneContacts.queryWhatsAppCall(resolver, "098765 43210"))
        assertNull(PhoneContacts.queryWhatsAppCall(resolver, "90000 00000"))
    }
}
