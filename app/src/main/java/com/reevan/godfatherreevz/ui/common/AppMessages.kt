package com.reevan.godfatherreevz.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The app-wide snackbar. Owned by the app shell, so a message shown just before a screen closes
 * (e.g. "5 friends added" as the contact importer returns Home) still appears on the next one.
 */
class AppMessages(
    val host: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    /** Replaces whatever message is showing. */
    fun show(message: String) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            host.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    /** Shows [message] with an [action] button; [onAction] runs if it is tapped. */
    fun show(message: String, action: String, onAction: () -> Unit) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val result = host.showSnackbar(message, actionLabel = action, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

val LocalAppMessages = staticCompositionLocalOf<AppMessages> {
    error("AppMessages is provided by GodfatherApp")
}
