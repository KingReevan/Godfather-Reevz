package com.reevan.godfatherreevz.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.util.UUID

/**
 * Tracks one-shot submissions — Create, Save, Remove — by token, in the (activity-scoped)
 * ViewModel that performs them.
 *
 * Why not just call the screen back when the write finishes: the ViewModel outlives a rotation,
 * the screen's composition does not. A callback captured before a rotation lands on the old,
 * discarded composition, so the new screen never closes — and with its in-progress flag reset,
 * Create could be tapped again and add the friend twice. Recording "token done" here instead lets
 * whichever composition is on screen *now* see it and act.
 *
 * Backed by Compose snapshot state, so a write from the click handler is visible to the very next
 * read in composition, with no gap where the token looks unknown.
 */
class SubmissionTracker {

    private val finished = mutableStateMapOf<String, Boolean>()

    fun begin(token: String) {
        finished[token] = false
    }

    fun complete(token: String) {
        finished[token] = true
    }

    /**
     * Whether [token]'s write is over. A token this tracker has never seen counts as over: that
     * only happens after the process was killed mid-write, and whatever the write did, it is done.
     */
    fun isFinished(token: String): Boolean = finished[token] ?: true
}

/** A screen's view of its submission: whether one is under way, and how to start one. */
class Submission internal constructor(
    /** True from the tap until the screen has moved on. Drives the action's enabled state. */
    val inProgress: Boolean,
    private val startFn: () -> String?,
) {
    /**
     * Starts a submission and returns its token for the ViewModel's write, or null if one is
     * already under way — checked at tap time, so a double tap can't start two.
     */
    fun start(): String? = startFn()
}

/**
 * The screen side of a [SubmissionTracker]. The token is saved with the screen, so a rotation
 * mid-write keeps the action disabled and still calls [onFinished] once the write is done.
 *
 * **Call it before any early return in the screen.** The write itself can trigger one — removing
 * the last friend empties Remove Friend's list — and if this isn't composed at that moment, it
 * never sees the write finish, and the screen never leaves.
 */
@Composable
fun rememberSubmission(tracker: SubmissionTracker, onFinished: () -> Unit): Submission {
    var token by rememberSaveable { mutableStateOf<String?>(null) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    val current = token
    if (current != null && tracker.isFinished(current)) {
        LaunchedEffect(current) { currentOnFinished() }
    }
    return Submission(inProgress = current != null) {
        if (token != null) {
            null
        } else {
            UUID.randomUUID().toString().also {
                token = it
                tracker.begin(it)
            }
        }
    }
}
