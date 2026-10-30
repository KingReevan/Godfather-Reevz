package com.reevan.godfatherreevz.ui.addfriend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.godfatherreevz.R
import com.reevan.godfatherreevz.ui.common.LocalAppMessages
import com.reevan.godfatherreevz.ui.common.rememberSubmission
import com.reevan.godfatherreevz.ui.friendform.FriendDraft
import com.reevan.godfatherreevz.ui.friendform.FriendFormScaffold
import com.reevan.godfatherreevz.ui.friendform.FriendNameField
import com.reevan.godfatherreevz.ui.friendform.FriendPhoneField
import com.reevan.godfatherreevz.ui.photo.EditablePhoto
import com.reevan.godfatherreevz.ui.theme.GodfatherTheme

/**
 * Add Friend: pick several from the phonebook (the importer), or type a name and number, with a
 * photo if wanted. Adding by hand stays on the page with the form cleared, so several can be added
 * in a row.
 */
@Composable
fun AddFriendScreen(
    onOpenContacts: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddFriendViewModel = viewModel(factory = AddFriendViewModel.Factory),
) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current
    val name = rememberTextFieldState()
    val phone = rememberTextFieldState()
    // A photo chosen for the friend being typed in (a saved file; one left unused is tidied away).
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var token by rememberSaveable { mutableStateOf<String?>(null) }

    val submission = rememberSubmission(viewModel.submissions, repeatable = true) {
        val result = token?.let(viewModel::resultOf)
        if (result != null && result.added.isNotEmpty()) {
            name.clearText()
            phone.clearText()
            photo = null
        }
        messages.show(result?.message() ?: "Friend added")
    }

    val draft = FriendDraft(name.text.toString(), phone.text.toString(), photo)
    val check = draft.check(friends.orEmpty())

    FriendFormScaffold(
        actionLabel = "Add Friend",
        actionEnabled = friends != null && check.canSave && !submission.inProgress,
        onAction = {
            val started = submission.start() ?: return@FriendFormScaffold
            token = started
            viewModel.add(started, listOf(draft.toNewFriend()))
        },
        modifier = modifier,
    ) {
        ContactsCard(onClick = onOpenContacts)
        OrDivider()
        EditablePhoto(
            owner = "add-friend",
            name = draft.name,
            photo = photo,
            phone = draft.phone,
            onChange = { photo = it },
            size = 88,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        FriendNameField(state = name)
        FriendPhoneField(state = phone, error = check.phoneError)
        Text(
            text = "New friends are shuffled in, and always get their call before anyone gets a repeat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ContactsCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .heightIn(min = 88.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_contacts),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Choose from contacts",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = GodfatherTheme.displayFont,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Tick several friends from your phonebook at once",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = "or add by hand",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
