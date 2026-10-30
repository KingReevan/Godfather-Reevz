package com.reevan.godfatherreevz.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.godfatherreevz.GodfatherApplication
import com.reevan.godfatherreevz.R
import com.reevan.godfatherreevz.contacts.PhoneContacts
import com.reevan.godfatherreevz.contacts.contactPhotoFor
import com.reevan.godfatherreevz.data.GodfatherDatabase
import com.reevan.godfatherreevz.photos.FriendPhotos
import com.reevan.godfatherreevz.ui.common.LocalAppMessages
import com.reevan.godfatherreevz.util.TodayClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Settings > Order. */
class ShuffleViewModel(private val application: GodfatherApplication) : ViewModel() {
    private val friendDao = GodfatherDatabase.getInstance(application).friendDao()

    val friendCount: StateFlow<Int?> = friendDao.observeInCallOrder().map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Reshuffles everyone still waiting (Rotation.reshuffle); [onDone] once it is saved. */
    fun reshuffle(onDone: () -> Unit) {
        val today = TodayClock.instance.today.value
        application.appScope.launch {
            friendDao.reshuffle(today, Random.Default)
            viewModelScope.launch { onDone() }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ShuffleViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GodfatherApplication)
            }
        }
    }
}

/** Settings > Contacts > Photos from contacts. */
class ContactPhotosViewModel(private val application: GodfatherApplication) : ViewModel() {
    private val friendDao = GodfatherDatabase.getInstance(application).friendDao()

    var copying by mutableStateOf(false)
        private set

    /**
     * Gives each friend without a photo a copy of their contact's, where the phonebook has one for
     * their number. Friends who have a photo keep it. [onDone] gets how many got one.
     */
    fun copy(onDone: (Int) -> Unit) {
        if (copying) return
        copying = true
        application.appScope.launch(Dispatchers.IO) {
            val copied = runCatching { copyMissing() }.getOrDefault(0)
            viewModelScope.launch {
                copying = false
                onDone(copied)
            }
        }
    }

    private suspend fun copyMissing(): Int {
        val contacts = PhoneContacts.loadAll(application)
        var copied = 0
        for (friend in friendDao.getAll().filter { it.photo == null }) {
            val contactPhoto = contactPhotoFor(friend.phone, contacts) ?: continue
            val photo = FriendPhotos.copyContactPhoto(application, contactPhoto.toUri()) ?: continue
            // Chosen one meanwhile? Theirs stays.
            if (friendDao.setPhotoIfNone(friend.id, photo)) copied++ else FriendPhotos.delete(application, photo)
        }
        return copied
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ContactPhotosViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GodfatherApplication)
            }
        }
    }
}

/** "Call directly" and the phone permission it needs. */
@Composable
internal fun CallingSection(callDirectly: Boolean, onCallDirectlyChange: (Boolean) -> Unit) {
    SettingRow(
        title = "Call directly",
        detail = if (callDirectly) {
            "Pressing Call dials straight away."
        } else {
            "Pressing Call opens the phone app with the number ready to dial."
        },
        modifier = Modifier.toggleable(value = callDirectly, role = Role.Switch, onValueChange = onCallDirectlyChange),
    ) {
        Switch(checked = callDirectly, onCheckedChange = null)
    }
    if (callDirectly) {
        PermissionRow(
            permission = Manifest.permission.CALL_PHONE,
            title = "Phone permission",
            grantedText = "Allowed — calls go straight through.",
            deniedText = "Not allowed — Call opens the phone app instead.",
        )
    }
}

/** Contacts access, and copying contact photos to friends who have none. */
@Composable
internal fun ContactsSection(viewModel: ContactPhotosViewModel = viewModel(factory = ContactPhotosViewModel.Factory)) {
    val context = LocalContext.current
    val messages = LocalAppMessages.current
    var allowed by remember { mutableStateOf(context.has(Manifest.permission.READ_CONTACTS)) }
    LifecycleResumeEffect(Unit) {
        allowed = context.has(Manifest.permission.READ_CONTACTS)
        onPauseOrDispose {}
    }
    PermissionRow(
        permission = Manifest.permission.READ_CONTACTS,
        title = "Contacts access",
        grantedText = "Allowed — Add Friend can list your phonebook, photos included.",
        deniedText = "Not allowed — Add Friend uses the system contact picker.",
        onChange = { allowed = it },
    )
    SettingRow(
        title = "Photos from contacts",
        detail = if (allowed) {
            "Friends without a photo get the one saved in your contacts."
        } else {
            "Needs contacts access."
        },
    ) {
        FilledTonalButton(
            onClick = {
                viewModel.copy { copied ->
                    messages.show(
                        when (copied) {
                            0 -> "No new photos found in your contacts"
                            1 -> "1 photo added"
                            else -> "$copied photos added"
                        },
                    )
                }
            },
            enabled = allowed && !viewModel.copying,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Copy")
        }
    }
}

/** Shuffle again, behind a confirmation (the brief: "to prevent accidental shuffling"). */
@Composable
internal fun OrderSection(viewModel: ShuffleViewModel = viewModel(factory = ShuffleViewModel.Factory)) {
    val friendCount by viewModel.friendCount.collectAsStateWithLifecycle()
    val messages = LocalAppMessages.current
    var confirming by rememberSaveable { mutableStateOf(false) }
    val enoughToShuffle = (friendCount ?: 0) >= 2

    SettingRow(
        title = "Shuffle again",
        detail = if (enoughToShuffle) {
            "Gives everyone still waiting for a call a new random place in line."
        } else {
            "Add at least two friends to shuffle."
        },
    ) {
        FilledTonalButton(
            onClick = { confirming = true },
            enabled = enoughToShuffle,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(painterResource(R.drawable.ic_shuffle), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Shuffle")
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            icon = { Icon(painterResource(R.drawable.ic_shuffle), contentDescription = null) },
            title = { Text("Shuffle the order?") },
            text = {
                Text(
                    "Everyone still waiting for a call gets a new random place — including today's " +
                        "friend, if you haven't called them yet. Friends you've already called won't " +
                        "jump the queue, and new friends still come before any repeat.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    viewModel.reshuffle { messages.show("Shuffled") }
                }) { Text("Shuffle") }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
internal fun AboutSection() {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    SettingRow(
        title = "Godfather Reevz" + (version?.let { " $it" } ?: ""),
        detail = "One call a day, so nobody drifts away. Everything stays on this phone.",
    )
}

/** A setting: title and explanation on the left, its control on the right. */
@Composable
private fun SettingRow(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    control: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        control()
    }
}

/**
 * A runtime permission's status with the way to change it: "Allow" asks; once Android stops
 * asking (refused twice), "Open settings" goes to the app's page in the phone's Settings. Re-read
 * whenever the screen comes back, since it can be changed there.
 */
@Composable
private fun PermissionRow(
    permission: String,
    title: String,
    grantedText: String,
    deniedText: String,
    onChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var granted by remember { mutableStateOf(context.has(permission)) }
    var blocked by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(permission) {
        granted = context.has(permission)
        onPauseOrDispose {}
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        onChange(ok)
        if (!ok) blocked = activity == null || !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
    SettingRow(title = title, detail = if (granted) grantedText else deniedText) {
        if (!granted) {
            if (blocked) {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Open settings") }
            } else {
                TextButton(onClick = { request.launch(permission) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Allow")
                }
            }
        }
    }
}

private fun Context.has(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
