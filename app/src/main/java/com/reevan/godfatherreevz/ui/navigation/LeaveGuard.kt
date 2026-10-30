package com.reevan.godfatherreevz.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Lets the screen on top hold up the user leaving it — so the Edit Friend editor can ask
 * "Discard changes?" first.
 *
 * Every way of leaving goes through here — system back, the header's arrow and a tapped
 * breadcrumb — each carrying the back stack the user is heading for. A screen that intercepts
 * keeps that target and, once the user agrees, navigates to exactly it: tapping "Home" with
 * unsaved edits ends on Home after Discard, not one screen back.
 *
 * Screens finishing their own job (Create, Save, Remove) do not go through the guard; they have
 * nothing to lose and use [popIfCurrent].
 */
class LeaveGuard {

    private var interceptor: ((target: List<Destination>) -> Boolean)? = null

    /**
     * Offers the attempt to leave for [target] to the screen on top. Returns true if it held the
     * attempt up, in which case the caller must not navigate.
     */
    fun intercepts(target: List<Destination>): Boolean = interceptor?.invoke(target) == true

    internal fun register(interceptor: (List<Destination>) -> Boolean) {
        this.interceptor = interceptor
    }

    internal fun unregister(interceptor: (List<Destination>) -> Boolean) {
        if (this.interceptor === interceptor) this.interceptor = null
    }
}

/**
 * While this screen is shown and [enabled] is true, every attempt to leave it is held up and
 * handed to [onAttempt] with the back stack the user was heading for.
 */
@Composable
fun LeaveGuard.InterceptLeaving(
    enabled: Boolean,
    onAttempt: (target: List<Destination>) -> Unit,
) {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnAttempt by rememberUpdatedState(onAttempt)
    DisposableEffect(this) {
        val interceptor: (List<Destination>) -> Boolean = { target ->
            if (currentEnabled) {
                currentOnAttempt(target)
                true
            } else {
                false
            }
        }
        register(interceptor)
        onDispose { unregister(interceptor) }
    }
}
