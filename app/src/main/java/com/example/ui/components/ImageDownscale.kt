package com.example.ui.components

/**
 * Pure math for shrinking note photos (unit-tested): a photo's longest side is
 * capped at [MAX_DIMENSION] pixels and it is stored as JPEG at [JPEG_QUALITY].
 * Phone screenshots are usually below the cap and keep their size; multi-megabyte
 * camera photos drop to a few hundred KB with no visible difference inside a note.
 */
object ImageDownscale {
    const val MAX_DIMENSION = 1920
    const val JPEG_QUALITY = 90

    fun targetSize(width: Int, height: Int, maxDimension: Int = MAX_DIMENSION): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return width to height
        val longest = maxOf(width, height)
        if (longest <= maxDimension) return width to height
        val scale = maxDimension.toFloat() / longest.toFloat()
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }

    /** Power-of-two decode sample so a huge photo is never fully decoded into memory. */
    fun sampleSize(width: Int, height: Int, maxDimension: Int = MAX_DIMENSION): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxDimension) {
            sample *= 2
        }
        return sample
    }
}
