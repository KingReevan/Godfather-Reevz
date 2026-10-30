package com.reevan.godfatherreevz.ui.editfriend

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.godfatherreevz.R
import com.reevan.godfatherreevz.data.Friend
import com.reevan.godfatherreevz.ui.common.FriendRow
import com.reevan.godfatherreevz.ui.common.FriendSearchField
import com.reevan.godfatherreevz.ui.common.ListMessage
import com.reevan.godfatherreevz.ui.common.LocalAppMessages
import com.reevan.godfatherreevz.ui.common.filterFriends
import com.reevan.godfatherreevz.ui.common.rememberSubmission
import com.reevan.godfatherreevz.ui.friendform.FriendDraft
import com.reevan.godfatherreevz.ui.friendform.FriendFormScaffold
import com.reevan.godfatherreevz.ui.friendform.FriendNameField
import com.reevan.godfatherreevz.ui.friendform.FriendPhoneField
import com.reevan.godfatherreevz.ui.navigation.Destination
import com.reevan.godfatherreevz.ui.navigation.InterceptLeaving
import com.reevan.godfatherreevz.ui.navigation.LeaveGuard

/** Edit Friend: every friend A–Z, searchable; tapping one opens the editor. */
@Composable
fun EditFriendListScreen(
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditFriendViewModel = viewModel(factory = EditFriendViewModel.Factory),
) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val all = friends ?: return

    Column(modifier.fillMaxSize()) {
        if (all.isNotEmpty()) FriendSearchField(query, { query = it })
        val shown = remember(all, query) { filterFriends(all, query) }
        when {
            all.isEmpty() -> ListMessage("No friends to edit yet. Add some from the Menu.")
            shown.isEmpty() -> ListMessage("No friends match \"$query\".")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(shown, key = { it.id }) { friend ->
                    FriendRow(
                        friend = friend,
                        modifier = Modifier.clickable(onClickLabel = "Edit ${friend.name}") { onOpen(friend.id) },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.graphicsLayer { rotationZ = 180f },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

/**
 * The editor for one friend's name and number. Save is enabled only for a real, valid change.
 * Leaving with unsaved changes — system back, the header arrow, a breadcrumb, or the header's
 * Settings/Menu buttons — asks "Discard changes?" first and then goes exactly where the owner
 * was heading.
 */
@Composable
fun EditFriendScreen(
    friendId: Long,
    leaveGuard: LeaveGuard,
    onLeave: (target: List<Destination>) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditFriendViewModel = viewModel(factory = EditFriendViewModel.Factory),
) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current
    // The stored friend, re-read on every entry, as the baseline for "changed".
    var original by remember { mutableStateOf<Friend?>(null) }
    val name = rememberTextFieldState()
    val phone = rememberTextFieldState()
    var filled by rememberSaveable { mutableStateOf(false) }
    // Where the owner tried to go with unsaved changes, while "Discard changes?" is up. Saved as
    // routes, so after a rotation Discard still lands on the screen they chose.
    var pendingLeave by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val saving = rememberSubmission(viewModel.submissions) {
        messages.show("Saved")
        onDone()
    }

    LaunchedEffect(friendId) {
        val friend = viewModel.load(friendId)
        if (friend == null) {
            // Removed while away; there is nothing to edit.
            onDone()
            return@LaunchedEffect
        }
        if (!filled) {
            name.setTextAndPlaceCursorAtEnd(friend.name)
            phone.setTextAndPlaceCursorAtEnd(friend.phone)
            filled = true
        }
        original = friend
    }

    val stored = original ?: return
    val draft = FriendDraft(name.text.toString(), phone.text.toString())
    val changed = draft.changes(stored)
    val check = draft.check(friends.orEmpty(), exceptId = stored.id)

    leaveGuard.InterceptLeaving(enabled = changed && !saving.inProgress) { target ->
        pendingLeave = target.map { it.route }
    }

    FriendFormScaffold(
        actionLabel = "Save",
        actionEnabled = changed && check.canSave && friends != null && !saving.inProgress,
        onAction = { saving.start()?.let { viewModel.save(it, stored.id, draft.toNewFriend()) } },
        modifier = modifier,
    ) {
        FriendNameField(state = name)
        FriendPhoneField(state = phone, error = check.phoneError)
        Text(
            text = "Their place in line, call history and notes stay as they are.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val leaving = pendingLeave
    if (leaving != null) {
        AlertDialog(
            onDismissRequest = { pendingLeave = null },
            title = { Text("Discard changes?") },
            text = { Text("Your changes to ${stored.name} will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingLeave = null
                    onLeave(leaving.map(Destination::fromRoute))
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { pendingLeave = null }) { Text("Keep editing") }
            },
        )
    }
}
