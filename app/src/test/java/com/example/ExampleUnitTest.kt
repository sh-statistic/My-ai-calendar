package com.example

import com.example.calendar.PersianCalendarHelper
import com.example.data.EventEntity
import com.example.data.NoteEntity
import com.example.data.SyncData
import com.example.data.TaskEntity
import com.example.prayer.PrayerTimesCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testPersianCalendarConversionRoundTrip() {
        // Test multiple dates roundtrip
        val testDates = listOf(
            Triple(2026, 3, 21),
            Triple(2026, 1, 1),
            Triple(2025, 12, 31),
            Triple(2024, 3, 20)
        )

        for ((gy, gm, gd) in testDates) {
            val jDate = PersianCalendarHelper.gregorianToJalali(gy, gm, gd)
            assertTrue(jDate.year in 1400..1410)
            assertTrue(jDate.month in 1..12)
            assertTrue(jDate.day in 1..31)

            val backG = PersianCalendarHelper.jalaliToGregorian(jDate.year, jDate.month, jDate.day)
            assertEquals(gy, backG.year)
            assertEquals(gm, backG.month)
            assertEquals(gd, backG.day)
        }
    }

    @Test
    fun testPersianDigitsFormatting() {
        val formatted = PersianCalendarHelper.toPersianDigits("1405/01/15 - 10:30")
        assertEquals("۱۴۰۵/۰۱/۱۵ - ۱۰:۳۰", formatted)
    }

    @Test
    fun testDateNormalizationAndEnglishDigits() {
        assertEquals("1403/07/21", PersianCalendarHelper.toEnglishDigits("۱۴۰۳/۰۷/۲۱"))
        assertEquals("1403/07/21", PersianCalendarHelper.normalizePersianDate("۱۴۰۳/۰۷/۲۱"))
        assertEquals("1403/07/05", PersianCalendarHelper.normalizePersianDate("1403/7/5"))
        assertEquals("1403/07/21", PersianCalendarHelper.normalizePersianDate("  ۱۴۰۳/۷/۲۱  "))
    }

    @Test
    fun testWeekDatesCalculation() {
        // Test week dates for 1403/07/21 (Saturday to Friday)
        val weekDates = PersianCalendarHelper.getWeekDates(1403, 7, 21)
        assertEquals(7, weekDates.size)
        // Check days are sequential
        for (i in 0 until 6) {
            val d1 = weekDates[i]
            val d2 = weekDates[i + 1]
            val dow1 = PersianCalendarHelper.getPersianDayOfWeek(d1.year, d1.month, d1.day)
            val dow2 = PersianCalendarHelper.getPersianDayOfWeek(d2.year, d2.month, d2.day)
            assertEquals((dow1 + 1) % 7, dow2)
        }
    }

    @Test
    fun testTehranPrayerTimesCalculation() {
        val tehran = PrayerTimesCalculator.getCityByName("تهران")
        assertEquals("تهران", tehran.nameFa)

        // Calculate for March 21, 2026 (Nowruz)
        val times = PrayerTimesCalculator.calculatePrayerTimes(
            tehran.latitude, tehran.longitude, 2026, 3, 21
        )

        assertNotNull(times.fajr)
        assertNotNull(times.sunrise)
        assertNotNull(times.dhuhr)
        assertNotNull(times.sunset)
        assertNotNull(times.maghrib)
        assertNotNull(times.midnight)

        // Fajr should be before sunrise
        assertTrue(times.fajrHour < times.sunriseHour)
        // Sunrise should be before dhuhr
        assertTrue(times.sunriseHour < times.dhuhrHour)
        // Dhuhr should be before sunset
        assertTrue(times.dhuhrHour < times.sunsetHour)
        // Sunset should be before maghrib
        assertTrue(times.sunsetHour < times.maghribHour)
    }

    @Test
    fun testSyncDataJsonSerialization() {
        val event = EventEntity(
            title = "جلسه تست",
            description = "تست همگام‌سازی",
            persianDate = "1405/01/01",
            gregorianDate = "2026-03-21"
        )
        val task = TaskEntity(
            title = "کار مهم",
            isCompleted = false,
            persianDueDate = "1405/01/05"
        )
        val note = NoteEntity(
            title = "یادداشت تست",
            content = "محتوای تست"
        )
        val syncData = SyncData(
            deviceId = "Test-Device",
            timestamp = 123456789L,
            events = listOf(event),
            tasks = listOf(task),
            notes = listOf(note)
        )

        val jsonStr = syncData.toJsonString()
        assertTrue(jsonStr.contains("جلسه تست"))
        assertTrue(jsonStr.contains("کار مهم"))
        assertTrue(jsonStr.contains("یادداشت تست"))

        val restored = SyncData.fromJsonString(jsonStr)
        assertEquals(1, restored.events.size)
        assertEquals("جلسه تست", restored.events[0].title)
        assertEquals(1, restored.tasks.size)
        assertEquals("کار مهم", restored.tasks[0].title)
        assertEquals(1, restored.notes.size)
        assertEquals("یادداشت تست", restored.notes[0].title)
    }

    @Test
    fun testEditOfficialOccasionAndDeduplication() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        com.example.calendar.PersianOccasionsHelper.init(context)

        // 1. Initial occasion on 7-20
        val initialOccasions = com.example.calendar.PersianOccasionsHelper.getOccasionsForDate(7, 20, 1405)
        assertTrue(initialOccasions.isNotEmpty())
        val hafezOfficial = initialOccasions.first { it.title.contains("حافظ") }

        // 2. Edit official occasion to "روز بزرگداشت حافظ"
        com.example.calendar.PersianOccasionsHelper.updateCustomOccasion(
            context,
            oldOccasion = hafezOfficial,
            newTitle = "روز بزرگداشت حافظ",
            newMonth = 7,
            newDay = 20,
            newIsHoliday = false
        )

        // 3. Verify it changed in getOccasionsForDate and NO duplicate exists
        val updatedOccasions = com.example.calendar.PersianOccasionsHelper.getOccasionsForDate(7, 20, 1405)
        val hafezList = updatedOccasions.filter { it.title.contains("حافظ") }
        assertEquals(1, hafezList.size)
        assertEquals("روز بزرگداشت حافظ", hafezList[0].title)

        // 4. Repeated edit attempt should NOT create duplicate in custom occasions
        val editedOccasion = hafezList[0]
        com.example.calendar.PersianOccasionsHelper.updateCustomOccasion(
            context,
            oldOccasion = editedOccasion,
            newTitle = "روز بزرگداشت حافظ",
            newMonth = 7,
            newDay = 20,
            newIsHoliday = false
        )
        val customList = com.example.calendar.PersianOccasionsHelper.getCustomOccasions().filter { it.customMonth == 7 && it.customDay == 20 }
        assertEquals(1, customList.size)
        assertEquals("روز بزرگداشت حافظ", customList[0].title)

        // 5. Clean up
        com.example.calendar.PersianOccasionsHelper.removeCustomOccasion(context, customList[0])
    }

    @Test
    fun testMultiProviderAiConfiguration() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()

        // 1. Verify providers resolution (only Gemini and OpenAI Compatible)
        assertEquals(com.example.chat.AiProvider.GEMINI, com.example.chat.AiProvider.fromId("gemini"))
        assertEquals(com.example.chat.AiProvider.OPENAI_COMPATIBLE, com.example.chat.AiProvider.fromId("openai_compatible"))
        assertEquals(com.example.chat.AiProvider.OPENAI_COMPATIBLE, com.example.chat.AiProvider.fromId("openai"))
        assertEquals(com.example.chat.AiProvider.OPENAI_COMPATIBLE, com.example.chat.AiProvider.fromId("custom"))

        // 2. Set provider and keys
        com.example.chat.ApiKeyManager.saveSelectedProvider(context, com.example.chat.AiProvider.OPENAI_COMPATIBLE)
        assertEquals(com.example.chat.AiProvider.OPENAI_COMPATIBLE, com.example.chat.ApiKeyManager.getSelectedProvider(context))

        com.example.chat.ApiKeyManager.saveApiKey(context, "sk-test-compatible-key", com.example.chat.AiProvider.OPENAI_COMPATIBLE)
        com.example.chat.ApiKeyManager.saveApiKey(context, "AIzaSyTestGeminiKey", com.example.chat.AiProvider.GEMINI)

        assertEquals("sk-test-compatible-key", com.example.chat.ApiKeyManager.getApiKey(context, com.example.chat.AiProvider.OPENAI_COMPATIBLE))
        assertEquals("AIzaSyTestGeminiKey", com.example.chat.ApiKeyManager.getApiKey(context, com.example.chat.AiProvider.GEMINI))
        assertTrue(com.example.chat.ApiKeyManager.isKeyConfigured(context, com.example.chat.AiProvider.OPENAI_COMPATIBLE))
        assertTrue(com.example.chat.ApiKeyManager.isKeyConfigured(context, com.example.chat.AiProvider.GEMINI))

        // 3. Test Base URL and Model customization for OpenAI Compatible
        com.example.chat.ApiKeyManager.saveBaseUrl(context, "https://api.deepseek.com/chat/completions", com.example.chat.AiProvider.OPENAI_COMPATIBLE)
        com.example.chat.ApiKeyManager.saveModel(context, "deepseek-chat", com.example.chat.AiProvider.OPENAI_COMPATIBLE)

        assertEquals("https://api.deepseek.com/chat/completions", com.example.chat.ApiKeyManager.getBaseUrl(context, com.example.chat.AiProvider.OPENAI_COMPATIBLE))
        assertEquals("deepseek-chat", com.example.chat.ApiKeyManager.getModel(context, com.example.chat.AiProvider.OPENAI_COMPATIBLE))

        // 4. Clean up keys
        com.example.chat.ApiKeyManager.clearApiKey(context, com.example.chat.AiProvider.OPENAI_COMPATIBLE)
        com.example.chat.ApiKeyManager.clearApiKey(context, com.example.chat.AiProvider.GEMINI)
        assertEquals("", com.example.chat.ApiKeyManager.getApiKey(context, com.example.chat.AiProvider.OPENAI_COMPATIBLE))
        assertEquals("", com.example.chat.ApiKeyManager.getApiKey(context, com.example.chat.AiProvider.GEMINI))
    }
}
