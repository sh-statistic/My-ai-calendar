package com.example

import com.example.ui.components.ImageDownscale
import org.junit.Assert.assertEquals
import org.junit.Test

/** The downscale math that keeps note photos small without visible quality loss. */
class ImageDownscaleTest {

    @Test
    fun photoUnderCapKeepsItsSize() {
        // A typical phone screenshot (1080x2400 has a long side over the cap, so use 1080x1600)
        assertEquals(1080 to 1600, ImageDownscale.targetSize(1080, 1600))
        assertEquals(800 to 600, ImageDownscale.targetSize(800, 600))
    }

    @Test
    fun bigCameraPhotoShrinksProportionally() {
        assertEquals(1920 to 1440, ImageDownscale.targetSize(4000, 3000))
        assertEquals(1440 to 1920, ImageDownscale.targetSize(3000, 4000))
    }

    @Test
    fun sampleSizeHalvesUntilNearCap() {
        assertEquals(1, ImageDownscale.sampleSize(1000, 800))
        assertEquals(1, ImageDownscale.sampleSize(1920, 1080))
        assertEquals(2, ImageDownscale.sampleSize(4000, 3000))
        assertEquals(4, ImageDownscale.sampleSize(8000, 6000))
    }

    @Test
    fun invalidDimensionsPassThrough() {
        assertEquals(0 to 0, ImageDownscale.targetSize(0, 0))
    }
}
