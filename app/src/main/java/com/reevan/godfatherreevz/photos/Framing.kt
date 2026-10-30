package com.reevan.godfatherreevz.photos

import kotlin.math.min
import kotlin.math.roundToInt

/** A square part of a picture, in the picture's pixels. */
data class Square(val left: Int, val top: Int, val side: Int)

/**
 * How the owner has framed a picture in the photo editor: the point of the picture at the middle of
 * the circle ([centerX], [centerY], in picture pixels) and how far in they have zoomed ([zoom]; 1 =
 * the circle just spans the picture's short side). Kept in picture pixels rather than screen pixels,
 * so the framing survives the screen rotating.
 */
data class Framing(val centerX: Float, val centerY: Float, val zoom: Float) {

    /** The side of the framed square, in picture pixels. */
    fun side(width: Int, height: Int): Float = min(width, height) / zoom

    /**
     * After a drag of ([dx], [dy]) screen pixels, shown at [scale] screen pixels per picture pixel.
     * The picture follows the finger, so the framed point moves the other way.
     */
    fun dragged(dx: Float, dy: Float, scale: Float, width: Int, height: Int): Framing =
        copy(centerX = centerX - dx / scale, centerY = centerY - dy / scale).within(width, height)

    /**
     * After a pinch by [factor] around a point ([focusX], [focusY]) screen pixels from the circle's
     * middle: the part of the picture under the fingers stays under them.
     */
    fun zoomed(factor: Float, focusX: Float, focusY: Float, scale: Float, width: Int, height: Int): Framing {
        val newZoom = (zoom * factor).coerceIn(1f, MAX_ZOOM)
        val shift = 1 - zoom / newZoom
        return Framing(
            centerX = centerX + focusX / scale * shift,
            centerY = centerY + focusY / scale * shift,
            zoom = newZoom,
        ).within(width, height)
    }

    /** Kept to what can be framed: zoom 1 to [MAX_ZOOM], the square never past an edge. */
    fun within(width: Int, height: Int): Framing {
        val z = zoom.coerceIn(1f, MAX_ZOOM)
        val half = min(width, height) / z / 2
        return Framing(centerX.coerceIn(half, width - half), centerY.coerceIn(half, height - half), z)
    }

    /** The framed square in whole pixels, always inside the picture. */
    fun square(width: Int, height: Int): Square {
        val side = side(width, height).roundToInt().coerceIn(1, min(width, height))
        val left = (centerX - side / 2f).roundToInt().coerceIn(0, width - side)
        val top = (centerY - side / 2f).roundToInt().coerceIn(0, height - side)
        return Square(left, top, side)
    }

    companion object {
        /** Zooming in further would leave too few pixels for a sharp photo. */
        const val MAX_ZOOM = 5f

        /** The whole middle square — how a picture opens in the editor. */
        fun centred(width: Int, height: Int) = Framing(width / 2f, height / 2f, 1f)
    }
}

/**
 * The `inSampleSize` that decodes a picture whose long side is [longSide] pixels to at most [max]:
 * the smallest power of two that is enough.
 */
fun sampleToFit(longSide: Int, max: Int): Int {
    var sample = 1
    while (longSide / sample > max) sample *= 2
    return sample
}

/**
 * The `inSampleSize` that decodes a [side]-pixel photo as small as it can while still at least
 * [target] pixels: the largest power of two that keeps it that big.
 */
fun sampleToCover(side: Int, target: Int): Int {
    var sample = 1
    while (side / (sample * 2) >= target) sample *= 2
    return sample
}
