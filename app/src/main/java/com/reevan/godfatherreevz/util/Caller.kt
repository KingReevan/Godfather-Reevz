package com.reevan.godfatherreevz.util

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.reevan.godfatherreevz.contacts.PhoneContacts
import com.reevan.godfatherreevz.contacts.WHATSAPP_VOICE_CALL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** How a WhatsApp call went. */
enum class WhatsAppResult {
    /** WhatsApp is calling them. */
    CALLING,

    /**
     * Their WhatsApp chat is open instead (its call button is a tap away): the phonebook has no
     * WhatsApp voice call for the number.
     */
    CHAT,

    /** Their WhatsApp chat is open instead: without contacts access, the voice call can't be found. */
    CHAT_NO_CONTACTS,

    /** WhatsApp couldn't be opened. */
    FAILED,
}

/** Places calls. An interface so UI tests can swap in a fake that never dials anyone. */
interface Caller {

    /** Whether there is a phone app to call with at all (a tablet without one: no). */
    fun canCall(context: Context): Boolean

    /** Whether the app may place calls itself (CALL_PHONE granted). */
    fun mayCallDirectly(context: Context): Boolean

    /**
     * Calls [phone]: straight away when [direct] and allowed, else by opening the dialer with the
     * number filled in. Returns false if nothing could be opened.
     */
    fun call(context: Context, phone: String, direct: Boolean): Boolean

    /** Whether WhatsApp is installed. */
    fun hasWhatsApp(context: Context): Boolean

    /**
     * Calls [phone] on WhatsApp: a voice call through WhatsApp's entry for the number in the
     * phonebook (needs contacts access), else by opening their WhatsApp chat.
     */
    suspend fun callOnWhatsApp(context: Context, phone: String): WhatsAppResult
}

/** The real one: `ACTION_CALL` (direct, needs CALL_PHONE) or `ACTION_DIAL` (no permission). */
object AndroidCaller : Caller {

    private fun telUri(phone: String): Uri = Uri.fromParts("tel", PhoneNumbers.dialString(phone), null)

    override fun canCall(context: Context): Boolean =
        Intent(Intent.ACTION_DIAL, telUri("100")).resolveActivity(context.packageManager) != null

    override fun mayCallDirectly(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    override fun call(context: Context, phone: String, direct: Boolean): Boolean {
        val uri = telUri(phone)
        if (direct && mayCallDirectly(context)) {
            try {
                context.startActivity(Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: ActivityNotFoundException) {
                // Fall through to the dialer.
            } catch (_: SecurityException) {
                // Permission revoked between the check and the call: the dialer still works.
            }
        }
        return try {
            context.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private const val WHATSAPP = "com.whatsapp"

    override fun hasWhatsApp(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(WHATSAPP) != null

    /**
     * WhatsApp has no public "call this number" intent. What it does have is a "Voice call" entry
     * on each phonebook contact who is on WhatsApp, which the Contacts app opens with it — so the
     * app does the same. Without one, WhatsApp's click-to-chat link opens the chat.
     */
    override suspend fun callOnWhatsApp(context: Context, phone: String): WhatsAppResult {
        val voiceCall = withContext(Dispatchers.IO) { PhoneContacts.findWhatsAppCall(context, phone) }
        if (voiceCall != null) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(voiceCall, WHATSAPP_VOICE_CALL)
                        .setPackage(WHATSAPP)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                return WhatsAppResult.CALLING
            } catch (_: ActivityNotFoundException) {
                // A WhatsApp that no longer takes these: the chat still works.
            } catch (_: SecurityException) {
                // Likewise.
            }
        }
        return try {
            val chat = "https://wa.me/${internationalDigits(context, phone)}".toUri()
            context.startActivity(
                Intent(Intent.ACTION_VIEW, chat).setPackage(WHATSAPP).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            if (PhoneContacts.hasPermission(context)) WhatsAppResult.CHAT else WhatsAppResult.CHAT_NO_CONTACTS
        } catch (_: ActivityNotFoundException) {
            WhatsAppResult.FAILED
        }
    }

    /**
     * [phone] with its country code, as WhatsApp's links need ("98765 43210" in India →
     * "919876543210"); a number without one is taken to be from the country the phone is in.
     */
    private fun internationalDigits(context: Context, phone: String): String {
        val telephony = context.getSystemService(TelephonyManager::class.java)
        val country = listOfNotNull(telephony?.networkCountryIso, telephony?.simCountryIso, Locale.getDefault().country)
            .firstOrNull(String::isNotBlank)
            .orEmpty()
            .uppercase(Locale.ROOT)
        val international = PhoneNumberUtils.formatNumberToE164(PhoneNumbers.dialString(phone), country)
        return PhoneNumbers.digits(international ?: phone)
    }
}

val LocalCaller = staticCompositionLocalOf<Caller> { AndroidCaller }
