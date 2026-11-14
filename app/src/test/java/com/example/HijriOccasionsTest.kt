package com.example

import com.example.calendar.HijriCalendarUtils
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
 * Tests for the occasions renovation (spec: occasions-renovation-spec.md):
 * - occasions are placed through the same conversion that displays the Hijri date
 * - 12 Muharram is an occasion, not an official holiday
 * - the official 1406 JSON anchors, counts and continuity
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HijriOccasionsTest {

    private fun daysInJalaliMonth(year: Int, month: Int): Int = when {
        month <= 6 -> 31
        month <= 11 -> 30
        else -> if (HijriCalendarUtils.isPersianLeapYear(year)) 30 else 29
    }

    @Test
    fun occasions_sitExactlyOnDisplayedHijriDate() {
        for (year in listOf(1406, 1407)) {
            val placed = HijriCalendarUtils.getOccasionsForYear(year)
            for (month in 1..12) {
                for (day in 1..daysInJalaliMonth(year, month)) {
                    val hijri = HijriCalendarUtils.persianToIslamic(year, month, day)
                    val expected = HijriCalendarUtils.HIJRI_OCCASIONS.filter {
                        it.hijriMonth == hijri.month && it.hijriDay == hijri.day
                    }
                    val actual = placed["$month-$day"].orEmpty()
                    assertEquals(
                        "$year-$month-$day (Hijri ${hijri.month}/${hijri.day}) occasion count",
                        expected.size, actual.size
                    )
                    assertEquals(
                        "$year-$month-$day titles",
                        expected.map { it.title }.sorted(),
                        actual.map { it.title }.sorted()
                    )
                    assertEquals(
                        "$year-$month-$day holiday flags",
                        expected.map { it.isHoliday }.sorted(),
                        actual.map { it.isHoliday }.sorted()
                    )
                }
            }
        }
    }

    @Test
    fun hijriTable_flagsAndCoverage() {
        val table = HijriCalendarUtils.HIJRI_OCCASIONS
        // The only official-holiday entries are the 17 official Hijri holiday days.
        assertEquals(17, table.count { it.isHoliday })
        // 12 Muharram (Martyrdom of Imam Zayn al-Abidin) is an occasion, NOT an official holiday.
        val day12 = table.filter { it.hijriMonth == 1 && it.hijriDay == 12 }
        assertTrue("12 Muharram entries exist", day12.isNotEmpty())
        assertTrue("12 Muharram must not be a holiday", day12.none { it.isHoliday })
        // Coverage: holidays (18 days incl. 12 Muharram) + imported non-holiday days.
        val distinctDays = table.map { it.hijriMonth to it.hijriDay }.toSet()
        assertTrue("at least 90 distinct Hijri occasion days, was ${distinctDays.size}", distinctDays.size >= 90)
        // No exact duplicates.
        val triples = table.map { Triple(it.hijriMonth, it.hijriDay, it.title) }
        assertEquals("no duplicate (month, day, title)", triples.size, triples.toSet().size)
    }

    private fun load1406(): JSONObject {
        val candidates = listOf(
            File("src/main/assets/occasions_1406.json"),
            File("app/src/main/assets/occasions_1406.json")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw AssertionError("occasions_1406.json not found in test working dir ${File(".").absolutePath}")
        return JSONObject(file.readText())
    }

    @Test
    fun official1406Json_anchorsCountsAndContinuity() {
        val root = load1406()
        var dayCount = 0
        var holidayCount = 0
        var officialRuleCount = 0
        var prev: Triple<Int, Int, Int>? = null
        // 1 Farvardin 1406 was a Sunday; Friday is offset 5 in the week.
        var dayOfYear = 0
        // The statutory holidays: fixed Shamsi days + fixed Hijri days (12 Muharram excluded).
        val shamsiHolidays = setOf(
            1 to 1, 1 to 2, 1 to 3, 1 to 4, 1 to 12, 1 to 13,
            3 to 14, 3 to 15, 11 to 22, 12 to 29
        )
        val hijriHolidays = setOf(
            1 to 9, 1 to 10, 2 to 20, 2 to 28, 2 to 29, 3 to 8, 3 to 17, 6 to 3,
            7 to 13, 7 to 27, 8 to 15, 9 to 21, 10 to 1, 10 to 2, 10 to 25, 12 to 10, 12 to 18
        )
        for (month in 1..12) {
            val daysObj = root.getJSONObject(month.toString())
            for (day in 1..daysInJalaliMonth(1406, month)) {
                val d = daysObj.getJSONObject(day.toString())
                dayCount++
                dayOfYear++
                val h = Triple(d.getInt("hYear"), d.getInt("hMonth"), d.getInt("hDay"))
                val isFriday = (dayOfYear - 1) % 7 == 5
                val isOfficial = (month to day) in shamsiHolidays || (h.second to h.third) in hijriHolidays
                if (isOfficial) officialRuleCount++
                assertEquals(
                    "isHoliday of 1406-$month-$day",
                    isOfficial || isFriday,
                    d.getBoolean("isHoliday")
                )
                if (d.getBoolean("isHoliday")) holidayCount++
                prev?.let { p ->
                    val sameMonth = h.first == p.first && h.second == p.second && h.third == p.third + 1
                    val rollMonth = h.third == 1 && h.first == p.first && h.second == p.second + 1
                    val rollYear = h.third == 1 && h.second == 1 && h.first == p.first + 1
                    assertTrue("Hijri date must advance by one day: $p -> $h", sameMonth || rollMonth || rollYear)
                }
                prev = h
            }
        }
        assertEquals(365, dayCount)
        assertEquals(75, holidayCount) // Fridays + official holidays, same pattern as 1405
        assertEquals(27, officialRuleCount)

        fun day(m: Int, d: Int) = root.getJSONObject(m.toString()).getJSONObject(d.toString())
        fun hijri(m: Int, d: Int): Triple<Int, Int, Int> {
            val o = day(m, d)
            return Triple(o.getInt("hYear"), o.getInt("hMonth"), o.getInt("hDay"))
        }
        assertEquals(Triple(1448, 10, 12), hijri(1, 1))
        // Shawwal 1448 officially has 29 days: 19 Farvardin is 1 Dhu al-Qa'dah.
        assertEquals(Triple(1448, 11, 1), hijri(1, 19))
        assertTrue(day(1, 14).getBoolean("isHoliday")) // Martyrdom of Imam Sadiq, 25 Shawwal
        assertEquals(Triple(1448, 12, 10), hijri(2, 27))
        assertTrue(day(2, 27).getBoolean("isHoliday")) // Eid al-Adha
        assertEquals(Triple(1449, 10, 1), hijri(12, 8))
        assertTrue(day(12, 8).getBoolean("isHoliday")) // Eid al-Fitr
        assertTrue(day(12, 9).getBoolean("isHoliday")) // Eid al-Fitr, second day
        assertTrue(day(12, 8).getJSONArray("occasions").getString(0).contains("عید سعید فطر"))
        assertFalse(day(1, 19).getBoolean("isHoliday"))
    }
}
