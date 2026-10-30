package com.reevan.godfatherreevz.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.reevan.godfatherreevz.photos.FriendPhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A friend's round photo, or the [Initials] badge when they have none (or while it loads, or if
 * the file is missing). [size] in dp; [container] and [content] colour the initials.
 */
@Composable
fun FriendAvatar(
    name: String,
    photo: String?,
    modifier: Modifier = Modifier,
    size: Int = 44,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    val image = rememberFriendPhoto(photo, size)
    if (image != null) {
        RoundImage(image, size, modifier)
    } else {
        Initials(name, modifier, size, container, content)
    }
}

/** A phonebook contact's photo (a contacts-provider URI), or their initials. For the importer. */
@Composable
fun ContactAvatar(name: String, thumbnail: String?, modifier: Modifier = Modifier, size: Int = 40) {
    val image = rememberImage(thumbnail?.let { "contact:$it" }) { context ->
        thumbnail?.let { context.contentResolver.openInputStream(it.toUri())?.use(BitmapFactory::decodeStream) }
    }
    if (image != null) RoundImage(image, size, modifier) else Initials(name, modifier, size)
}

@Composable
private fun RoundImage(image: ImageBitmap, size: Int, modifier: Modifier) {
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape),
    )
}

/** [photo] decoded for a [sizeDp] avatar: at its size or a little over, not the full file. */
@Composable
private fun rememberFriendPhoto(photo: String?, sizeDp: Int): ImageBitmap? {
    val target = with(LocalDensity.current) { sizeDp.dp.roundToPx() }
    return rememberImage(photo?.let { "friend:$it@$target" }) { context ->
        photo?.let { FriendPhotos.load(context, it, target) }
    }
}

/**
 * The image [load]ed for [key] (null for none) — at once if it was decoded before, otherwise
 * decoded off the main thread, so a list scrolls without stutter.
 */
@Composable
private fun rememberImage(key: String?, load: (Context) -> Bitmap?): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    var image by remember(key) { mutableStateOf(key?.let(DecodedImages::get)) }
    if (key != null && image == null) {
        LaunchedEffect(key) {
            image = withContext(Dispatchers.IO) {
                runCatching { load(context) }.getOrNull()?.asImageBitmap()?.also { DecodedImages.put(key, it) }
            }
        }
    }
    return image
}

/** Recently shown photos, decoded, so coming back to a list doesn't decode them all again. */
private object DecodedImages {
    private val cache = object : LruCache<String, ImageBitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.asAndroidBitmap().byteCount
    }

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun put(key: String, image: ImageBitmap) {
        cache.put(key, image)
    }
}
