package com.reevan.godfatherreevz.ui.removefriend

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.godfatherreevz.data.Friend
import com.reevan.godfatherreevz.data.FriendDao
import com.reevan.godfatherreevz.data.GodfatherDatabase
import com.reevan.godfatherreevz.photos.FriendPhotos
import com.reevan.godfatherreevz.ui.common.FriendRow
import com.reevan.godfatherreevz.ui.common.FriendSearchField
import com.reevan.godfatherreevz.ui.common.ListMessage
import com.reevan.godfatherreevz.ui.common.LocalAppMessages
import com.reevan.godfatherreevz.ui.common.SubmissionTracker
import com.reevan.godfatherreevz.ui.common.filterFriends
import com.reevan.godfatherreevz.ui.common.rememberSubmission
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemoveFriendViewModel(
    private val application: Application,
    private val friendDao: FriendDao,
) : ViewModel() {

    val friends: StateFlow<List<Friend>?> = friendDao.observeAlphabetical()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val submissions = SubmissionTracker()

    /** Removes [ids], photos and all; their past calls stay in Statistics (FriendDao.removeFriends). */
    fun remove(token: String, ids: List<Long>) {
        viewModelScope.launch {
            friendDao.removeFriends(ids).forEach { FriendPhotos.delete(application, it) }
            submissions.complete(token)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                RemoveFriendViewModel(application, GodfatherDatabase.getInstance(application).friendDao())
            }
        }
    }
}

/**
 * Remove Friend: every friend A–Z with a checkbox (the whole row toggles it), then "Remove (n)"
 * at the bottom right, behind a confirm dialog. Returns to the Menu when done.
 */
@Composable
fun RemoveFriendScreen(
    onRemoved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemoveFriendViewModel = viewModel(factory = RemoveFriendViewModel.Factory),
) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    var removedCount by rememberSaveable { mutableIntStateOf(0) }
    // Before the early return: removing the last friends empties the list (see rememberSubmission).
    val removing = rememberSubmission(viewModel.submissions) {
        messages.show(if (removedCount == 1) "Friend removed" else "$removedCount friends removed")
        onRemoved()
    }
    val all = friends ?: return
    // Friends removed elsewhere since they were ticked no longer count.
    val live = selected.filter { id -> all.any { it.id == id } }

    Column(modifier.fillMaxSize()) {
        if (all.isNotEmpty()) FriendSearchField(query, { query = it })
        val shown = remember(all, query) { filterFriends(all, query) }
        Column(Modifier.weight(1f)) {
            when {
                all.isEmpty() -> ListMessage("No friends to remove.")
                shown.isEmpty() -> ListMessage("No friends match \"$query\".")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(shown, key = { it.id }) { friend ->
                        val checked = friend.id in live
                        FriendRow(
                            friend = friend,
                            modifier = Modifier.toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { on -> selected = if (on) live + friend.id else live - friend.id },
                            ),
                            leading = { Checkbox(checked = checked, onCheckedChange = null) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = if (live.isEmpty()) "Tick the friends to remove" else "${live.size} selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { confirming = true },
                enabled = live.isNotEmpty() && !removing.inProgress,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(if (live.isEmpty()) "Remove" else "Remove (${live.size})")
            }
        }
    }

    if (confirming && live.isNotEmpty()) {
        val names = all.filter { it.id in live }.map { it.name }
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(if (names.size == 1) "Remove ${names.single()}?" else "Remove ${names.size} friends?") },
            text = {
                Text(
                    (if (names.size in 2..5) names.joinToString(", ") + ".\n\n" else "") +
                        "Their notes and photos go with them. Past calls stay in Statistics.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        val token = removing.start() ?: return@TextButton
                        removedCount = live.size
                        viewModel.remove(token, live)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Cancel") }
            },
        )
    }
}
