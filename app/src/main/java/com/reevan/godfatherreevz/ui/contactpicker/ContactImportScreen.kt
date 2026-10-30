package com.reevan.godfatherreevz.ui.contactpicker

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.godfatherreevz.R
import com.reevan.godfatherreevz.contacts.PhoneContact
import com.reevan.godfatherreevz.contacts.PhoneContacts
import com.reevan.godfatherreevz.data.Friend
import com.reevan.godfatherreevz.ui.addfriend.AddFriendViewModel
import com.reevan.godfatherreevz.ui.addfriend.ContactPick
import com.reevan.godfatherreevz.ui.common.ContactAvatar
import com.reevan.godfatherreevz.ui.common.LocalAppMessages
import com.reevan.godfatherreevz.ui.common.rememberSubmission
import com.reevan.godfatherreevz.ui.friendform.FriendDraft
import com.reevan.godfatherreevz.ui.friendform.findSameNumber
import com.reevan.godfatherreevz.ui.theme.GodfatherTheme
import kotlinx.coroutines.launch

/**
 * The contact importer (Add Friend › Contacts): the phonebook as a searchable A–Z list with
 * checkboxes, so several friends can be added at once — each with their contact photo. Contacts already saved as friends are shown
 * but can't be ticked; for a contact with several numbers, the one to call is chosen under it.
 *
 * Needs READ_CONTACTS, asked for here with an explanation first. Refused, it offers the system
 * contact picker instead, which needs no permission (multi-select on Android 17+, one contact at a
 * time before) — those picks are added straight away.
 */
@Composable
fun ContactImportScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    importViewModel: ContactImportViewModel = viewModel(factory = ContactImportViewModel.Factory),
    addViewModel: AddFriendViewModel = viewModel(factory = AddFriendViewModel.Factory),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val messages = LocalAppMessages.current
    val scope = rememberCoroutineScope()

    var granted by remember { mutableStateOf(PhoneContacts.hasPermission(context)) }
    var refusedForGood by rememberSaveable { mutableStateOf(false) }
    // Back from the phone's Settings with access turned on (or off).
    LifecycleResumeEffect(Unit) {
        granted = PhoneContacts.hasPermission(context)
        onPauseOrDispose {}
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        // Refused twice, Android stops asking: only the phone's Settings can turn it on.
        if (!ok && activity != null) {
            refusedForGood = !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)
        }
    }

    // Both submissions before any early return (see rememberSubmission).
    var bulkToken by rememberSaveable { mutableStateOf<String?>(null) }
    val bulk = rememberSubmission(addViewModel.submissions) {
        bulkToken?.let(addViewModel::resultOf)?.let { messages.show(it.message()) }
        onDone()
    }
    var pickToken by rememberSaveable { mutableStateOf<String?>(null) }
    val picked = rememberSubmission(addViewModel.submissions, repeatable = true) {
        pickToken?.let(addViewModel::resultOf)?.let { messages.show(it.message()) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        scope.launch {
            val contacts = importViewModel.readPicked(uri)
            if (contacts.isEmpty()) {
                messages.show("No phone number found for that contact")
                return@launch
            }
            val token = picked.start() ?: return@launch
            pickToken = token
            addViewModel.addPicks(
                token,
                contacts.map { ContactPick(FriendDraft(it.name, it.numbers.first().number).toNewFriend(), it.photo) },
            )
        }
    }
    val openSystemPicker = {
        try {
            picker.launch(PhoneContacts.systemPickerIntent())
        } catch (_: ActivityNotFoundException) {
            messages.show("No contacts app to pick from")
        }
    }

    if (!granted) {
        PermissionPrompt(
            refusedForGood = refusedForGood,
            onAllow = { permission.launch(Manifest.permission.READ_CONTACTS) },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
            onUseSystemPicker = openSystemPicker,
            modifier = modifier,
        )
        return
    }

    LaunchedEffect(Unit) { importViewModel.load() }
    val state by importViewModel.contacts.collectAsStateWithLifecycle()
    val friends by addViewModel.friends.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val selection = rememberSaveable(saver = SelectionSaver) { mutableStateMapOf() }

    Column(modifier.fillMaxSize().imePadding()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search name or number") },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear search")
                    }
                }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Box(Modifier.weight(1f)) {
            when (val current = state) {
                ContactsState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ContactsState.Failed -> Notice(
                    text = "Couldn't read your contacts.",
                    action = "Use the system picker",
                    onAction = openSystemPicker,
                )
                is ContactsState.Loaded -> {
                    val shown = remember(current, query) { filterContacts(current.contacts, query) }
                    when {
                        current.contacts.isEmpty() -> Notice(
                            text = "No contacts with phone numbers on this phone.",
                            action = null,
                            onAction = {},
                        )
                        shown.isEmpty() -> Notice(text = "No contacts match \"$query\".", action = null, onAction = {})
                        else -> ContactList(
                            contacts = shown,
                            friends = friends.orEmpty(),
                            selection = selection,
                        )
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
                text = if (selection.isEmpty()) "Tick the friends to add" else "${selection.size} selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    val contacts = (state as? ContactsState.Loaded)?.contacts ?: return@Button
                    val chosen = contacts.mapNotNull { contact ->
                        selection[contact.key]?.let { ContactPick(FriendDraft(contact.name, it).toNewFriend(), contact.photo) }
                    }
                    if (chosen.isEmpty()) return@Button
                    val token = bulk.start() ?: return@Button
                    bulkToken = token
                    addViewModel.addPicks(token, chosen)
                },
                enabled = selection.isNotEmpty() && friends != null && !bulk.inProgress,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(if (selection.size == 1) "Add 1 friend" else "Add ${selection.size} friends")
            }
        }
    }
}

/** Selected contacts: lookup key → the number chosen for them. Saved across rotation. */
private val SelectionSaver = listSaver<SnapshotStateMap<String, String>, String>(
    save = { map -> map.flatMap { (key, number) -> listOf(key, number) } },
    restore = { flat -> mutableStateMapOf<String, String>().apply { flat.chunked(2).forEach { put(it[0], it[1]) } } },
)

@Composable
private fun ContactList(
    contacts: List<PhoneContact>,
    friends: List<Friend>,
    selection: SnapshotStateMap<String, String>,
) {
    val sections = remember(contacts) { contacts.groupBy { sectionOf(it.name) } }
    LazyColumn(Modifier.fillMaxSize()) {
        sections.forEach { (letter, inSection) ->
            stickyHeader(key = "header-$letter") {
                Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = letter.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }
            items(inSection, key = { it.key }) { contact ->
                val savedAs = remember(contact, friends) {
                    contact.numbers.firstNotNullOfOrNull { findSameNumber(friends, it.number) }?.name
                }
                ContactItem(
                    contact = contact,
                    savedAs = savedAs,
                    chosen = selection[contact.key],
                    onToggle = {
                        if (selection.containsKey(contact.key)) {
                            selection.remove(contact.key)
                        } else {
                            selection[contact.key] = contact.numbers.first().number
                        }
                    },
                    onChoose = { selection[contact.key] = it },
                )
            }
        }
    }
}

@Composable
private fun ContactItem(
    contact: PhoneContact,
    savedAs: String?,
    chosen: String?,
    onToggle: () -> Unit,
    onChoose: (String) -> Unit,
) {
    val enabled = savedAs == null
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = chosen != null,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                )
                .heightIn(min = 64.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Checkbox(checked = chosen != null, onCheckedChange = null, enabled = enabled)
            Spacer(Modifier.width(8.dp))
            ContactAvatar(contact.name, contact.thumbnail, size = 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = GodfatherTheme.displayFont,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val first = contact.numbers.first()
                val detail = when {
                    savedAs != null && savedAs == contact.name -> "Already added"
                    savedAs != null -> "Already added as $savedAs"
                    contact.numbers.size == 1 -> numberLine(first.number, first.label)
                    else -> "${numberLine(chosen ?: first.number, contact.numbers.first { it.number == (chosen ?: first.number) }.label)}  ·  ${contact.numbers.size} numbers"
                }
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Several numbers: pick which one this friend is called on.
        if (chosen != null && contact.numbers.size > 1) {
            Column(
                Modifier
                    .selectableGroup()
                    .padding(start = 64.dp, end = 16.dp, bottom = 8.dp),
            ) {
                contact.numbers.forEach { number ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = number.number == chosen,
                                onClick = { onChoose(number.number) },
                                role = Role.RadioButton,
                            ),
                    ) {
                        RadioButton(selected = number.number == chosen, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(numberLine(number.number, number.label), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun numberLine(number: String, label: String) = if (label.isBlank()) number else "$number  ·  $label"

@Composable
private fun PermissionPrompt(
    refusedForGood: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onUseSystemPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_contacts),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text(
            text = "Pick friends from your contacts",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = GodfatherTheme.displayFont,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Godfather Reevz reads your contacts only to list them here, so you can tick " +
                "several friends at once. Nothing ever leaves your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (refusedForGood) {
            Text(
                text = "Contacts access is turned off. You can turn it on in the phone's settings, " +
                    "under Permissions.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 48.dp)) { Text("Open settings") }
        } else {
            Button(onClick = onAllow, modifier = Modifier.heightIn(min = 48.dp)) { Text("Allow contacts access") }
        }
        OutlinedButton(onClick = onUseSystemPicker, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (Build.VERSION.SDK_INT >= 37) "Use the system picker instead" else "Pick one at a time instead")
        }
        Text(
            text = if (Build.VERSION.SDK_INT >= 37) {
                "The system picker needs no permission."
            } else {
                "The system picker needs no permission, but picks one contact at a time."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Notice(text: String, action: String?, onAction: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) OutlinedButton(onClick = onAction) { Text(action) }
    }
}
