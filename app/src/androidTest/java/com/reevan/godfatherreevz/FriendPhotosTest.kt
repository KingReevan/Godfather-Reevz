package com.reevan.godfatherreevz

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reevan.godfatherreevz.photos.FriendPhotos
import com.reevan.godfatherreevz.photos.Square
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Opening, saving and tidying friends' photo files, on a real device's decoders. */
@RunWith(AndroidJUnit4::class)
class FriendPhotosTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val saved = mutableListOf<String>()

    @After
    fun tearDown() {
        saved.forEach { FriendPhotos.delete(context, it) }
    }

    /** A JPEG whose left half is red and right half blue, so its way up can be checked. */
    private fun picture(width: Int, height: Int, orientation: Int? = null): Uri {
        val file = File(context.cacheDir, "test-$width-$height-$orientation.jpg")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.BLUE)
            drawRect(0f, 0f, width / 2f, height.toFloat(), Paint().apply { color = Color.RED })
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        if (orientation != null) {
            ExifInterface(file.path).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                saveAttributes()
            }
        }
        return Uri.fromFile(file)
    }

    private fun decodeSaved(name: String): Bitmap = BitmapFactory.decodeFile(FriendPhotos.file(context, name).path)

    private fun isRed(pixel: Int) = Color.red(pixel) > 200 && Color.blue(pixel) < 60

    private fun isBlue(pixel: Int) = Color.blue(pixel) > 200 && Color.red(pixel) < 60

    @Test
    fun aSidewaysCameraPhotoOpensUpright() {
        val upright = FriendPhotos.open(context, picture(800, 600))!!
        assertEquals(800 to 600, upright.width to upright.height)

        val sideways = FriendPhotos.open(context, picture(800, 600, ExifInterface.ORIENTATION_ROTATE_90))!!
        assertEquals(600 to 800, sideways.width to sideways.height)
        // Turned a quarter clockwise: the red left half is now the top.
        assertTrue(isRed(sideways.getPixel(300, 100)))
        assertTrue(isBlue(sideways.getPixel(300, 700)))
    }

    @Test
    fun aHugePictureOpensScaledDown() {
        val opened = FriendPhotos.open(context, picture(4200, 1000))!!
        assertTrue(opened.width <= 2048)
        assertNull(FriendPhotos.open(context, Uri.fromFile(File(context.cacheDir, "nothing-here.jpg"))))
    }

    @Test
    fun theFramedSquareIsSavedAtMost512Square() {
        val opened = FriendPhotos.open(context, picture(1600, 1200))!!
        val framed = FriendPhotos.save(context, opened, Square(0, 0, 1200)).also { saved += it }
        val photo = decodeSaved(framed)
        assertEquals(512 to 512, photo.width to photo.height)
        assertTrue(isRed(photo.getPixel(100, 256)))
        assertTrue(isBlue(photo.getPixel(500, 256)))

        // A small square isn't blown up.
        val small = FriendPhotos.save(context, opened, Square(10, 10, 300)).also { saved += it }
        assertEquals(300, decodeSaved(small).width)
    }

    @Test
    fun aContactPhotoIsCopiedAsItsMiddleSquare() {
        val copy = FriendPhotos.copyContactPhoto(context, picture(900, 600))!!.also { saved += it }
        val photo = decodeSaved(copy)
        assertEquals(512 to 512, photo.width to photo.height)
        // The middle square straddles the halves: red on its left, blue on its right.
        assertTrue(isRed(photo.getPixel(40, 256)))
        assertTrue(isBlue(photo.getPixel(470, 256)))
    }

    @Test
    fun avatarsDecodeSmallCopies() {
        val name = FriendPhotos.save(context, FriendPhotos.open(context, picture(1024, 1024))!!).also { saved += it }
        assertEquals(128, FriendPhotos.load(context, name, 116)!!.width)
        assertEquals(512, FriendPhotos.load(context, name, 600)!!.width)
        assertNull(FriendPhotos.load(context, "missing.jpg", 100))
    }

    @Test
    fun sweepingDeletesOnlyUnusedFilesADayOld() {
        val opened = FriendPhotos.open(context, picture(600, 600))!!
        val used = FriendPhotos.save(context, opened).also { saved += it }
        val unused = FriendPhotos.save(context, opened).also { saved += it }

        // Just made: a form may still be holding it.
        FriendPhotos.sweep(context, inUse = setOf(used))
        assertTrue(FriendPhotos.file(context, unused).exists())

        FriendPhotos.sweep(context, inUse = setOf(used), now = System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000L)
        assertTrue(FriendPhotos.file(context, used).exists())
        assertFalse(FriendPhotos.file(context, unused).exists())
    }
}
