package com.example

import com.example.prayer.PrayerTimesCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the expansions of the prayer city list
 * (specs: khorasan-cities-spec.md and more-cities-spec.md, 2026-11-14).
 * Pure logic only — no Android/Robolectric needed.
 */
class PrayerTimesCitiesTest {

    @Test
    fun cityCount_is427() {
        assertEquals(427, PrayerTimesCalculator.CITIES.size)
    }

    @Test
    fun persianNames_areUnique() {
        val names = PrayerTimesCalculator.CITIES.map { it.nameFa }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun coordinates_areInsideIran() {
        for (city in PrayerTimesCalculator.CITIES) {
            assertTrue("${city.nameEn} lat", city.latitude in 24.0..40.0)
            assertTrue("${city.nameEn} lon", city.longitude in 43.0..64.0)
        }
    }

    @Test
    fun mashhad_keepsItsPreciseEntry() {
        val mashhad = PrayerTimesCalculator.CITIES.single { it.nameFa == "مشهد" }
        assertEquals(36.2972, mashhad.latitude, 0.0)
        assertEquals(59.6067, mashhad.longitude, 0.0)
    }

    @Test
    fun importantCitiesOfOtherProvinces_arePresent() {
        val names = PrayerTimesCalculator.CITIES.map { it.nameFa }.toSet()
        for (expected in listOf(
            "کاشان", "آبادان", "بابل", "ساوه", "چابهار", "گنبد کاووس",
            "مراغه", "خوی", "سیرجان", "بروجرد", "لاهیجان", "میناب",
            "رودبار گیلان", "رودبار کرمان", "پرند", "بندر امام", "شادگان"
        )) {
            assertTrue("missing: $expected", expected in names)
        }
    }

    @Test
    fun khorasanRazaviMainCities_arePresent() {
        val names = PrayerTimesCalculator.CITIES.map { it.nameFa }.toSet()
        for (expected in listOf(
            "نیشابور", "سبزوار", "تربت حیدریه", "تربت جام", "کاشمر",
            "قوچان", "گناباد", "سرخس", "چناران", "فریمان"
        )) {
            assertTrue("missing: $expected", expected in names)
        }
    }

    @Test
    fun everyCity_producesOrderedTimes() {
        // Sample date: 2026-03-21 (Gregorian). For every city the five
        // daytime markers must come out in chronological order.
        for (city in PrayerTimesCalculator.CITIES) {
            val t = PrayerTimesCalculator.calculatePrayerTimes(
                city.latitude, city.longitude, 2026, 3, 21
            )
            assertTrue("${city.nameEn} fajr<sunrise", t.fajrHour < t.sunriseHour)
            assertTrue("${city.nameEn} sunrise<dhuhr", t.sunriseHour < t.dhuhrHour)
            assertTrue("${city.nameEn} dhuhr<sunset", t.dhuhrHour < t.sunsetHour)
            assertTrue("${city.nameEn} sunset<maghrib", t.sunsetHour < t.maghribHour)
        }
    }

    @Test
    fun khorasanBlock_isInPersianDictionaryOrder() {
        // Persian dictionary order: ا ب پ ت ث ج چ ح خ د ذ ر ز ژ س ش ص ض ط ظ ع غ ف ق ک گ ل م ن و ه ی
        val alpha = "ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی"
        fun rankOf(name: String): List<Int> = name.mapNotNull { raw ->
            val ch = when (raw) {
                'آ' -> 'ا'
                'ي' -> 'ی'
                'ك' -> 'ک'
                '‌', ' ', '-' -> null
                else -> raw
            }
            ch?.let { alpha.indexOf(it).takeIf { i -> i >= 0 } ?: 99 }
        }
        val khorasan = PrayerTimesCalculator.CITIES.drop(31).map { it.nameFa }
        assertEquals(88, khorasan.size)
        for (i in 0 until khorasan.size - 1) {
            val a = rankOf(khorasan[i])
            val b = rankOf(khorasan[i + 1])
            val cmp = a.zip(b).firstOrNull { it.first != it.second }
                ?.let { it.first - it.second } ?: (a.size - b.size)
            assertTrue("${khorasan[i]} باید قبل از ${khorasan[i + 1]} باشد", cmp <= 0)
        }
    }

    @Test
    fun defaultCity_isStillTehran() {
        assertEquals("تهران", PrayerTimesCalculator.CITIES.first().nameFa)
        assertEquals("تهران", PrayerTimesCalculator.getCityByName("شهر ناموجود").nameFa)
    }
}
