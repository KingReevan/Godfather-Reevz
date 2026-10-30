// The framework ExifInterface reads the orientation tag correctly from API 24 (minSdk is 26); the
// AndroidX copy lint suggests would be a dependency for nothing.
@file:SuppressLint("ExifInterface")

package com.reevan.godfatherreevz.photos

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import java.io.File
import java.util.UUID
import kotlin.math.max

/**
 * Friends' photos: small square JPEGs in the app's private storage (`files/photos`), named at
 * random; `Friend.photo` holds the file name.
 *
 * Always a copy — of the square the owner framed in a picked picture, or of a contact's photo — so
 * a photo stays put when the original is deleted or the contact changes, and never leaves the
 * phone (the app has no backups). A changed photo is a new file, so nothing can show a stale copy.
 *
 * Everything here reads or writes files: call it off the main thread.
 */
object FriendPhotos {

    /** Saved photos are at most this many pixels square — sharp on the largest avatar. */
    const val SIZE = 512

    /** Pictures are opened at most this big for framing: room to zoom in, without the memory. */
    private const val MAX_FRAMING_SIDE = 2048

    /** How long an unused photo file is kept: a form may hold one that isn't saved yet. */
    private const val SWEEP_AFTER_MS = 24L * 60 * 60 * 1000

    fun file(context: Context, name: String): File = File(dir(context), name)

    /**
     * The picture at [uri] (the photo picker's, or a contact photo), upright — phone cameras often
     * save pictures sideways with a note to turn them — and at most [maxSide] pixels on its long
     * side. Null if it can't be read as a picture.
     */
    fun open(context: Context, uri: Uri, maxSide: Int = MAX_FRAMING_SIDE): Bitmap? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleToFit(max(bounds.outWidth, bounds.outHeight), maxSide)
        }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        upright(bitmap, orientation)
    }.getOrNull()

    /**
     * Saves the [square] of [picture] (its middle square if null) as a new photo of at most [SIZE]
     * pixels, and returns its file name.
     */
    fun save(context: Context, picture: Bitmap, square: Square? = null): String {
        val area = square ?: Framing.centred(picture.width, picture.height).square(picture.width, picture.height)
        val photo = render(picture, area, minOf(area.side, SIZE))
        val folder = dir(context).apply { mkdirs() }
        val name = "${UUID.randomUUID()}.jpg"
        // Written aside and renamed into place, so a half-written file is never shown.
        val partial = File(folder, "$name.partial")
        partial.outputStream().use { photo.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        check(partial.renameTo(File(folder, name))) { "Couldn't save the photo" }
        return name
    }

    /** Copies a contact's photo as a new photo; null if there's none to read. */
    fun copyContactPhoto(context: Context, uri: Uri): String? =
        open(context, uri, maxSide = SIZE * 2)?.let { save(context, it) }

    /** A photo at [target] pixels or a little more, for showing on screen; null if it's missing. */
    fun load(context: Context, name: String, target: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inSampleSize = sampleToCover(SIZE, target) }
        return BitmapFactory.decodeFile(file(context, name).path, options)
    }

    fun delete(context: Context, name: String?) {
        if (name != null) file(context, name).delete()
    }

    /**
     * Deletes photo files no friend uses — left by a form that was abandoned, or by the app being
     * closed mid-save — once they are a day old.
     */
    fun sweep(context: Context, inUse: Set<String>, now: Long = System.currentTimeMillis()) {
        dir(context).listFiles()?.forEach { file ->
            if (file.name !in inUse && now - file.lastModified() > SWEEP_AFTER_MS) file.delete()
        }
    }

    private fun dir(context: Context) = File(context.filesDir, "photos")

    /**
     * [area] of [picture] scaled to [side] pixels square, on white (a JPEG can't be see-through).
     * Large reductions are halved step by step first: one big bilinear jump would look grainy.
     */
    private fun render(picture: Bitmap, area: Square, side: Int): Bitmap {
        var source = Bitmap.createBitmap(picture, area.left, area.top, area.side, area.side)
        while (source.width / 2 >= side) {
            source = source.scale(source.width / 2, source.height / 2)
        }
        val out = createBitmap(side, side)
        Canvas(out).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, null, Rect(0, 0, side, side), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        return out
    }

    /** Applies a camera's EXIF orientation note, so the picture is the way up it was taken. */
    private fun upright(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> matrix.apply { setRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> matrix.apply { setRotate(-90f); postScale(-1f, 1f) }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
