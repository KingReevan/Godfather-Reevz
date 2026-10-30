package com.reevan.godfatherreevz.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

/**
 * Emits once immediately, then every time the system clock, time zone or date is changed out from
 * under the app.
 *
 * [TodayClock.tickAtMidnight] sleeps with a coroutine delay, which counts elapsed time — it can't
 * see the wall clock jump. Without this, setting the time, crossing a time zone or a network time
 * correction while the app is open would leave "today" stale until the next restart (seen on the
 * emulator: clock set to 23:59:30, and at 00:00 Home still showed the old date).
 *
 * The receiver is registered only while collected, i.e. while the app is on screen. These are
 * system broadcasts, which reach a not-exported receiver.
 */
fun clockChanges(context: Context): Flow<Unit> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(Unit)
        }
    }
    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
        addAction(Intent.ACTION_DATE_CHANGED)
    }
    ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    awaitClose { context.unregisterReceiver(receiver) }
}.onStart { emit(Unit) }
