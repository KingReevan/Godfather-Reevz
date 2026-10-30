package com.reevan.godfatherreevz.photos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FramingTest {

    @Test
    fun `a picture opens framed on its whole middle square`() {
        // Landscape: the square spans the full height, centred left to right.
        assertEquals(Square(left = 500, top = 0, side = 3000), Framing.centred(4000, 3000).square(4000, 3000))
        // Portrait: the full width, centred top to bottom.
        assertEquals(Square(left = 0, top = 250, side = 1000), Framing.centred(1000, 1500).square(1000, 1500))
        assertEquals(Square(0, 0, 640), Framing.centred(640, 640).square(640, 640))
    }

    @Test
    fun `dragging moves the picture with the finger`() {
        // Zoomed in 2x on a 1000 x 1000 picture: a 500 px square. Shown at 2 screen px per pixel.
        val framing = Framing(500f, 500f, 2f)
        // Dragging the picture right by 100 screen px shows 50 picture px further left.
        val moved = framing.dragged(dx = 100f, dy = 0f, scale = 2f, width = 1000, height = 1000)
        assertEquals(450f, moved.centerX, 0.001f)
        assertEquals(500f, moved.centerY, 0.001f)
    }

    @Test
    fun `the square never leaves the picture`() {
        val far = Framing(500f, 500f, 2f).dragged(dx = 10_000f, dy = -10_000f, scale = 2f, width = 1000, height = 1000)
        assertEquals(Square(left = 0, top = 500, side = 500), far.square(1000, 1000))
        // At zoom 1 on a landscape picture it can only slide sideways.
        val slid = Framing.centred(4000, 3000).dragged(dx = 0f, dy = 500f, scale = 0.3f, width = 4000, height = 3000)
        assertEquals(1500f, slid.centerY, 0.001f)
    }

    @Test
    fun `zoom stays between the whole short side and five times in`() {
        val start = Framing.centred(1000, 800)
        assertEquals(1f, start.zoomed(0.5f, 0f, 0f, scale = 1f, width = 1000, height = 800).zoom, 0.001f)
        assertEquals(Framing.MAX_ZOOM, start.zoomed(50f, 0f, 0f, scale = 1f, width = 1000, height = 800).zoom, 0.001f)
        assertEquals(160, start.zoomed(50f, 0f, 0f, scale = 1f, width = 1000, height = 800).square(1000, 800).side)
    }

    @Test
    fun `pinching keeps the spot under the fingers in place`() {
        val width = 2000
        val height = 2000
        val diameter = 800f
        val before = Framing(1000f, 1000f, 1.5f)
        val scaleBefore = diameter / before.side(width, height)
        // Fingers 120 px right of and 60 px above the circle's middle.
        val focusX = 120f
        val focusY = -60f
        val spotX = before.centerX + focusX / scaleBefore
        val spotY = before.centerY + focusY / scaleBefore

        val after = before.zoomed(1.6f, focusX, focusY, scaleBefore, width, height)
        val scaleAfter = diameter / after.side(width, height)
        assertEquals(spotX, after.centerX + focusX / scaleAfter, 0.01f)
        assertEquals(spotY, after.centerY + focusY / scaleAfter, 0.01f)
    }

    @Test
    fun `any gesture leaves a square inside the picture`() {
        val random = Random(11)
        repeat(2_000) {
            val width = random.nextInt(1, 5000)
            val height = random.nextInt(1, 5000)
            var framing = Framing.centred(width, height)
            repeat(20) {
                val scale = random.nextDouble(0.05, 20.0).toFloat()
                framing = if (random.nextBoolean()) {
                    framing.dragged(random.nextInt(-3000, 3000).toFloat(), random.nextInt(-3000, 3000).toFloat(), scale, width, height)
                } else {
                    framing.zoomed(random.nextDouble(0.2, 4.0).toFloat(), random.nextInt(-500, 500).toFloat(), random.nextInt(-500, 500).toFloat(), scale, width, height)
                }
                val square = framing.square(width, height)
                assertTrue(square.side >= 1)
                assertTrue("$square in $width x $height", square.left >= 0 && square.top >= 0)
                assertTrue("$square in $width x $height", square.left + square.side <= width && square.top + square.side <= height)
            }
        }
    }

    @Test
    fun `pictures are opened no bigger than needed`() {
        assertEquals(1, sampleToFit(2000, 2048))
        assertEquals(2, sampleToFit(4000, 2048))
        assertEquals(4, sampleToFit(8192, 2048))
        assertEquals(8, sampleToFit(8200, 2048))
    }

    @Test
    fun `photos are shown at their size or a little over`() {
        assertEquals(1, sampleToCover(512, 300))
        assertEquals(2, sampleToCover(512, 231)) // the friend's page, at 88dp
        assertEquals(2, sampleToCover(512, 189)) // today's card, at 72dp
        assertEquals(4, sampleToCover(512, 116)) // a list row, at 44dp
        assertEquals(4, sampleToCover(512, 128))
        assertEquals(8, sampleToCover(512, 64))
        assertEquals(1, sampleToCover(512, 1000))
    }
}
