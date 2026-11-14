package com.example

import com.example.calendar.OccasionsDownloader
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tests for the background occasions download (spec: occasions-download-spec.md).
 * Robolectric is needed for the real org.json implementation, like the
 * other tests in this repo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OccasionsDownloaderTest {

    private fun loadAsset(year: Int): String {
        val candidates = listOf(
            File("src/main/assets/occasions_$year.json"),
            File("app/src/main/assets/occasions_$year.json")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw AssertionError("occasions_$year.json not found in test working dir ${File(".").absolutePath}")
        return file.readText()
    }

    @Test
    fun urlFor_buildsTheRawDataBranchAddress() {
        assertEquals(
            "https://raw.githubusercontent.com/sh-statistic/My-ai-calendar/occasions-data/occasions_1407.json",
            OccasionsDownloader.urlFor(1407)
        )
    }

    @Test
    fun validate_acceptsTheReal1406File() {
        assertTrue(OccasionsDownloader.validate(loadAsset(1406), 1406))
    }

    @Test
    fun validate_rejectsTruncatedJson() {
        val text = loadAsset(1406)
        assertFalse(OccasionsDownloader.validate(text.substring(0, text.length / 2), 1406))
    }

    @Test
    fun validate_rejectsAMissingDay() {
        val root = JSONObject(loadAsset(1406))
        root.getJSONObject("5").remove("10")
        assertFalse(OccasionsDownloader.validate(root.toString(), 1406))
    }

    @Test
    fun validate_rejectsADuplicatedHijriDate() {
        val root = JSONObject(loadAsset(1406))
        val day1 = root.getJSONObject("1").getJSONObject("1")
        val day2 = root.getJSONObject("1").getJSONObject("2")
        // Force day 2 to repeat day 1's Hijri date: continuity must break.
        day2.put("hYear", day1.getInt("hYear"))
        day2.put("hMonth", day1.getInt("hMonth"))
        day2.put("hDay", day1.getInt("hDay"))
        assertFalse(OccasionsDownloader.validate(root.toString(), 1406))
    }

    @Test
    fun validate_rejectsAWrongTypedField() {
        val root = JSONObject(loadAsset(1406))
        root.getJSONObject("3").getJSONObject("3").put("isHoliday", "yes")
        assertFalse(OccasionsDownloader.validate(root.toString(), 1406))
    }

    @Test
    fun validate_rejectsTheWrongYear() {
        // 1406 data checked as 1407 still has 365 days (both non-leap), but
        // Esfand length rules differ per year only in leap years; use 1408
        // (leap) instead: its Esfand must have 30 days, so 1406 data fails.
        assertFalse(OccasionsDownloader.validate(loadAsset(1406), 1408))
    }
}
