package com.reevan.godfatherreevz.ui.details

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.godfatherreevz.GodfatherApplication
import com.reevan.godfatherreevz.R
import com.reevan.godfatherreevz.ui.common.Initials
import com.reevan.godfatherreevz.ui.common.LocalAppMessages
import com.reevan.godfatherreevz.ui.settings.PreferencesViewModel
import com.reevan.godfatherreevz.ui.theme.GodfatherTheme
import com.reevan.godfatherreevz.util.LocalCaller
import com.reevan.godfatherreevz.util.formatCallDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

/** Test tag on the Call button. */
const val CALL_BUTTON_TAG = "call-button"

/**
 * A friend's page: who they are, when they were last called, conversation notes (D4), and the
 * Call button at the bottom — the only thing that counts a call (the brief).
 *
 * Pressing Call records the call first (on the app-wide scope, so it lands even if the app is put
 * away for the call), then places it: straight away with the phone permission and "Call
 * directly" on, otherwise through the dialer. A second press the same day only redials.
 */
@Composable
fun FriendDetailsScreen(
    friendId: Long,
    onGone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendDetailsViewModel = viewModel(key = "friend-$friendId") {
        val application = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            as GodfatherApplication
        FriendDetailsViewModel(application, friendId)
    },
    preferences: PreferencesViewModel = viewModel(factory = PreferencesViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by preferences.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val caller = LocalCaller.current
    val messages = LocalAppMessages.current

    // The number waiting for the phone-permission answer; refused, the dialer opens instead.
    var pendingNumber by rememberSaveable { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingNumber?.let { number ->
            if (!caller.call(context, number, direct = granted)) messages.show("Couldn't open the phone app")
        }
        pendingNumber = null
    }

    when (val current = state) {
        null -> Unit
        DetailsState.Gone -> LaunchedEffect(Unit) { onGone() }
        is DetailsState.Loaded -> DetailsContent(
            state = current,
            onCall = {
                if (!caller.canCall(context)) {
                    messages.show("There's no phone app on this device to call with")
                    return@DetailsContent
                }
                if (!current.calledToday) viewModel.recordCall()
                val direct = settings?.callDirectly ?: true
                if (direct && !caller.mayCallDirectly(context)) {
                    pendingNumber = current.friend.phone
                    permission.launch(Manifest.permission.CALL_PHONE)
                } else if (!caller.call(context, current.friend.phone, direct)) {
                    messages.show("Couldn't open the phone app")
                }
            },
            onUndo = {
                viewModel.undoCall { undone ->
                    messages.show(if (undone) "Call undone" else "Nothing to undo")
                }
            },
            onNotesChanged = viewModel::saveNotes,
            modifier = modifier,
        )
    }
}

@Composable
private fun DetailsContent(
    state: DetailsState.Loaded,
    onCall: () -> Unit,
    onUndo: () -> Unit,
    onNotesChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val friend = state.friend
    val haptics = LocalHapticFeedback.current
    // Read once, when the page opens; from then on the field owns the text and saves it.
    val notes = rememberSaveable(friend.id, saver = TextFieldState.Saver) { TextFieldState(friend.notes) }
    val currentOnNotesChanged by rememberUpdatedState(onNotesChanged)
    val savedNotes by rememberUpdatedState(friend.notes)
    val saveNotes = { text: String -> if (text != savedNotes) currentOnNotesChanged(text) }
    LaunchedEffect(notes) {
        snapshotFlow { notes.text.toString() }.drop(1).collectLatest {
            delay(600)
            saveNotes(it)
        }
    }
    // Leaving mid-typing: save what's there now rather than waiting out the delay.
    DisposableEffect(notes) {
        onDispose { saveNotes(notes.text.toString()) }
    }

    var confirmingUndo by rememberSaveable { mutableStateOf(false) }
    // A short pause after each press, so a double tap can't open the dialer twice.
    var cooling by remember { mutableStateOf(false) }
    LaunchedEffect(cooling) {
        if (cooling) {
            delay(1_500)
            cooling = false
        }
    }
    val firstName = friend.name.substringBefore(' ')

    Column(modifier.fillMaxSize().imePadding()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Initials(friend.name, size = 64)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    RoleChip(state)
                    Text(
                        text = friend.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = GodfatherTheme.displayFont,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = friend.phone,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Fact(
                    label = "Last called",
                    value = state.lastCalled?.let(::formatCallDate) ?: "Never",
                    modifier = Modifier.weight(1f),
                )
                Fact(label = "Friend since", value = formatCallDate(friend.addedOn), modifier = Modifier.weight(1f))
            }

            if (state.calledToday) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = null,
                        tint = GodfatherTheme.colors.called,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Called today", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { confirmingUndo = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(painterResource(R.drawable.ic_undo), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Undo")
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_notes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Conversation notes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                OutlinedTextField(
                    state = notes,
                    placeholder = { Text("What you talked about, news, things to ask next time…") },
                    lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 5),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NOTES_TAG),
                )
                Text(
                    text = "Saved automatically. Only on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (state.role == TodayRole.OTHER && !state.calledToday) {
                Text(
                    text = "Not today's call — calling now counts as their call, and they move to the back of the line.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = {
                    if (cooling) return@Button
                    cooling = true
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onCall()
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .testTag(CALL_BUTTON_TAG),
            ) {
                Icon(painterResource(R.drawable.ic_call), contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (state.calledToday) "Call again" else "Call $firstName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (confirmingUndo) {
        AlertDialog(
            onDismissRequest = { confirmingUndo = false },
            title = { Text("Undo today's call?") },
            text = {
                Text("The call to ${friend.name} today won't count, and they go back to their place in line.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingUndo = false
                    onUndo()
                }) { Text("Undo call") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingUndo = false }) { Text("Keep it") }
            },
        )
    }
}

/** Test tag on the notes field. */
const val NOTES_TAG = "notes"

@Composable
private fun RoleChip(state: DetailsState.Loaded) {
    val text = when {
        state.role == TodayRole.TO_CALL -> "TODAY'S CALL"
        state.role == TodayRole.CALLED -> "TODAY'S CALL  ✓"
        state.calledToday -> "CALLED TODAY  ✓"
        else -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
