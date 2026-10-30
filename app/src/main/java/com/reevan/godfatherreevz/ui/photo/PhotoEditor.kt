package com.reevan.godfatherreevz.ui.photo

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.godfatherreevz.R
import com.reevan.godfatherreevz.contacts.PhoneContacts
import com.reevan.godfatherreevz.photos.FriendPhotos
import com.reevan.godfatherreevz.photos.Framing
import com.reevan.godfatherreevz.ui.common.FriendAvatar
import com.reevan.godfatherreevz.ui.common.LocalAppMessages
import com.reevan.godfatherreevz.ui.theme.GodfatherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * What picks a picture: the system photo picker, which needs no permission. UI tests provide one
 * that hands back a test image instead.
 */
val LocalPhotoPicker = staticCompositionLocalOf<ActivityResultContract<PickVisualMediaRequest, Uri?>> {
    ActivityResultContracts.PickVisualMedia()
}

/** A picture being framed, for the screen called [owner]. */
class Framed(val owner: String, val picture: Bitmap) {
    var framing by mutableStateOf(Framing.centred(picture.width, picture.height))
}

/** How changing a photo ended, for the screen called [owner]. */
sealed interface PhotoResult {
    val owner: String

    /** A new photo, saved as [photo]. */
    data class Chosen(override val owner: String, val photo: String) : PhotoResult

    data class Failed(override val owner: String, val message: String) : PhotoResult
}

/**
 * The photo editor's state. One for the whole app (the screens' ViewModels live as long as the
 * Activity), so a picture being framed survives the screen rotating; [Framed.owner] and
 * [PhotoResult.owner] say which screen it belongs to.
 */
class PhotoEditorViewModel(private val application: Application) : ViewModel() {

    var framed by mutableStateOf<Framed?>(null)
        private set

    /** Opening or saving a picture, or copying a contact photo. */
    var busy by mutableStateOf(false)
        private set

    var result by mutableStateOf<PhotoResult?>(null)
        private set

    /** Opens what the photo picker returned, to be framed. */
    fun open(owner: String, uri: Uri) = work(owner) {
        val picture = withContext(Dispatchers.IO) { FriendPhotos.open(application, uri) }
        if (picture == null) {
            PhotoResult.Failed(owner, "Couldn't open that picture")
        } else {
            framed = Framed(owner, picture)
            null
        }
    }

    fun cancel() {
        framed = null
    }

    /** Saves the framed square as a new photo. */
    fun save() {
        val current = framed ?: return
        framed = null
        work(current.owner) {
            val picture = current.picture
            val square = current.framing.square(picture.width, picture.height)
            val photo = withContext(Dispatchers.IO) {
                runCatching { FriendPhotos.save(application, picture, square) }.getOrNull()
            }
            if (photo != null) PhotoResult.Chosen(current.owner, photo) else PhotoResult.Failed(current.owner, "Couldn't save the photo")
        }
    }

    /** Copies the phonebook's photo for [phone]'s number. */
    fun useContactPhoto(owner: String, phone: String) = work(owner) {
        val photo = withContext(Dispatchers.IO) {
            runCatching {
                PhoneContacts.photoFor(application, phone)?.let { FriendPhotos.copyContactPhoto(application, it.toUri()) }
            }.getOrNull()
        }
        if (photo != null) PhotoResult.Chosen(owner, photo) else PhotoResult.Failed(owner, "No photo for this number in your contacts")
    }

    /** The screen has acted on [handled]. */
    fun consume(handled: PhotoResult) {
        if (result == handled) result = null
    }

    private fun work(owner: String, block: suspend () -> PhotoResult?) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            val outcome = runCatching { block() }.getOrElse { PhotoResult.Failed(owner, "Couldn't change the photo") }
            busy = false
            if (outcome != null) result = outcome
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PhotoEditorViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
            }
        }
    }
}

/**
 * A friend's photo that can be changed. Tapping it offers **Choose photo** (the system photo
 * picker, then framing the face in a circle), **Use contact photo** (the phonebook's photo for
 * [phone]'s number; offered with contacts access) and **Remove photo**. [onChange] gets the new
 * photo's file name, or null for Remove.
 *
 * [owner] names the screen (e.g. "details-12"), so the outcome — and the framing step, after a
 * rotation — comes back to the screen that asked.
 */
@Composable
fun EditablePhoto(
    owner: String,
    name: String,
    photo: String?,
    phone: String,
    onChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 96,
    editor: PhotoEditorViewModel = viewModel(factory = PhotoEditorViewModel.Factory),
) {
    val context = LocalContext.current
    val messages = LocalAppMessages.current
    val currentOnChange by rememberUpdatedState(onChange)
    val picker = rememberLauncherForActivityResult(LocalPhotoPicker.current) { uri ->
        if (uri != null) editor.open(owner, uri)
    }
    val choose = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val result = editor.result
    LaunchedEffect(result) {
        if (result == null || result.owner != owner) return@LaunchedEffect
        editor.consume(result)
        when (result) {
            is PhotoResult.Chosen -> currentOnChange(result.photo)
            is PhotoResult.Failed -> messages.show(result.message)
        }
    }

    var menuOpen by remember { mutableStateOf(false) }
    var contactsAllowed by remember { mutableStateOf(false) }
    val busy = editor.busy
    Box(modifier.size(size.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .clickable(enabled = !busy) {
                    contactsAllowed = PhoneContacts.hasPermission(context)
                    // Nothing to choose between: go straight to the photo picker.
                    if (photo == null && !contactsAllowed) choose() else menuOpen = true
                }
                .semantics { contentDescription = if (photo == null) "Add photo" else "Change photo" },
        ) {
            if (photo == null && name.isBlank()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(size.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_photo),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size((size * 0.36f).dp),
                    )
                }
            } else {
                FriendAvatar(name, photo, size = size)
            }
            if (busy) CircularProgressIndicator(Modifier.size((size * 0.5f).dp))
        }
        if (photo != null || name.isNotBlank()) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.32f).dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_photo),
                        contentDescription = null,
                        modifier = Modifier.size((size * 0.17f).dp),
                    )
                }
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Choose photo") },
                leadingIcon = { Icon(painterResource(R.drawable.ic_add_photo), contentDescription = null) },
                onClick = {
                    menuOpen = false
                    choose()
                },
            )
            if (contactsAllowed) {
                DropdownMenuItem(
                    text = { Text("Use contact photo") },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_contacts), contentDescription = null) },
                    enabled = phone.isNotBlank(),
                    onClick = {
                        menuOpen = false
                        editor.useContactPhoto(owner, phone)
                    },
                )
            }
            if (photo != null) {
                DropdownMenuItem(
                    text = { Text("Remove photo") },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        currentOnChange(null)
                    },
                )
            }
        }
    }

    editor.framed?.takeIf { it.owner == owner }?.let { framed ->
        FramingDialog(framed, onCancel = editor::cancel, onUse = editor::save)
    }
}

/**
 * Framing a picked picture: it fills the screen behind a circle; drag to move it, pinch to zoom.
 * "Use photo" saves what's inside the circle.
 */
@Composable
private fun FramingDialog(framed: Framed, onCancel: () -> Unit, onUse: () -> Unit) {
    val picture = remember(framed) { framed.picture.asImageBitmap() }
    val width = framed.picture.width
    val height = framed.picture.height
    val margin = with(LocalDensity.current) { 24.dp.toPx() }
    val scrim = MaterialTheme.colorScheme.scrim
    val ring = MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(
                        text = "Frame the photo",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = GodfatherTheme.displayFont,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Drag to move it, pinch to zoom.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Canvas(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .background(scrim)
                        .pointerInput(framed) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val diameter = min(size.width, size.height) - 2 * margin
                                val before = framed.framing
                                val focus = centroid - Offset(size.width / 2f, size.height / 2f)
                                val zoomed = before.zoomed(
                                    factor = zoom,
                                    focusX = focus.x,
                                    focusY = focus.y,
                                    scale = diameter / before.side(width, height),
                                    width = width,
                                    height = height,
                                )
                                framed.framing = zoomed.dragged(
                                    dx = pan.x,
                                    dy = pan.y,
                                    scale = diameter / zoomed.side(width, height),
                                    width = width,
                                    height = height,
                                )
                            }
                        },
                ) {
                    val diameter = min(size.width, size.height) - 2 * margin
                    val framing = framed.framing
                    val scale = diameter / framing.side(width, height)
                    drawImage(
                        image = picture,
                        dstOffset = IntOffset(
                            (center.x - framing.centerX * scale).roundToInt(),
                            (center.y - framing.centerY * scale).roundToInt(),
                        ),
                        dstSize = IntSize((width * scale).roundToInt(), (height * scale).roundToInt()),
                        filterQuality = FilterQuality.Medium,
                    )
                    val circle = Path().apply {
                        addOval(Rect(center, diameter / 2))
                    }
                    clipPath(circle, clipOp = ClipOp.Difference) { drawRect(scrim.copy(alpha = 0.6f)) }
                    drawCircle(ring, radius = diameter / 2, center = center, style = Stroke(2.dp.toPx()))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
                    Button(onClick = onUse, modifier = Modifier.heightIn(min = 48.dp)) { Text("Use photo") }
                }
            }
        }
    }
}
