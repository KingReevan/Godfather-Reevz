package com.reevan.godfatherreevz.util

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat

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
}

val LocalCaller = staticCompositionLocalOf<Caller> { AndroidCaller }
