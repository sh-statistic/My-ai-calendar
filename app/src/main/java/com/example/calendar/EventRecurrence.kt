package com.example.calendar

import com.example.data.EventEntity
import java.util.Calendar
import java.util.UUID

enum class RecurrenceFrequency(val titleFa: String, val titleEn: String) {
    NONE("بدون تکرار", "Does not repeat"),
    DAILY("هر روز", "Every day"),
    WEEKLY("هر هفته", "Every week"),
    MONTHLY("هر ماه", "Every month"),
    YEARLY("هر سال", "Every year")
}

enum class RecurrenceEndType(val titleFa: String, val titleEn: String) {
    NEVER("تکرار برای همیشه", "Forever"),
    UNTIL_DATE("تا تاریخ مشخص", "Until date"),
    AFTER_COUNT("بعد از چند تکرار", "After count")
}

data class EventRecurrenceConfig(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val endType: RecurrenceEndType = RecurrenceEndType.NEVER,
    val untilDate: String = "", // تاریخ شمسی YYYY/MM/DD
    val count: Int = 10         // تعداد تکرار
) {
    fun getSummaryText(isFa: Boolean): String {
        if (frequency == RecurrenceFrequency.NONE) {
            return if (isFa) "بدون تکرار" else "Does not repeat"
        }
        val freqStr = if (isFa) frequency.titleFa else frequency.titleEn
        val endStr = when (endType) {
            RecurrenceEndType.NEVER -> if (isFa) "تکرار برای همیشه" else "Forever"
            RecurrenceEndType.UNTIL_DATE -> {
                val formatted = if (isFa) PersianCalendarHelper.toPersianDigits(untilDate) else untilDate
                if (isFa) "تا تاریخ $formatted" else "Until $formatted"
            }
            RecurrenceEndType.AFTER_COUNT -> {
                val countDigits = if (isFa) PersianCalendarHelper.toPersianDigits(count.toString()) else count.toString()
                if (isFa) "بعد از $countDigits تکرار" else "After $countDigits times"
            }
        }
        return "$freqStr ($endStr)"
    }
}

object EventRecurrenceHelper {

    fun generateRecurringEvents(
        baseEvent: EventEntity,
        config: EventRecurrenceConfig
    ): List<EventEntity> {
        if (config.frequency == RecurrenceFrequency.NONE) {
            return listOf(baseEvent)
        }

        val normalizedDate = PersianCalendarHelper.normalizePersianDate(baseEvent.persianDate)
        val parts = normalizedDate.split("/").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) {
            return listOf(baseEvent)
        }

        val startJYear = parts[0]
        val startJMonth = parts[1]
        val startJDay = parts[2]

        val maxAllowed = when (config.endType) {
            RecurrenceEndType.AFTER_COUNT -> config.count.coerceIn(1, 365)
            RecurrenceEndType.UNTIL_DATE -> 500
            RecurrenceEndType.NEVER -> when (config.frequency) {
                RecurrenceFrequency.DAILY -> 365
                RecurrenceFrequency.WEEKLY -> 104
                RecurrenceFrequency.MONTHLY -> 36
                RecurrenceFrequency.YEARLY -> 10
                RecurrenceFrequency.NONE -> 1
            }
        }

        val recurrenceGroupId = baseEvent.recurrenceId ?: UUID.randomUUID().toString()
        val ruleName = config.frequency.name
        val events = mutableListOf<EventEntity>()

        val initialGregorian = PersianCalendarHelper.jalaliToGregorian(startJYear, startJMonth, startJDay)

        for (step in 0 until maxAllowed) {
            val (currentJalali, gDateFormatted) = when (config.frequency) {
                RecurrenceFrequency.DAILY -> {
                    val cal = Calendar.getInstance().apply {
                        set(initialGregorian.year, initialGregorian.month - 1, initialGregorian.day)
                        add(Calendar.DAY_OF_YEAR, step)
                    }
                    val jDate = PersianCalendarHelper.gregorianToJalali(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                    val gDate = "%04d-%02d-%02d".format(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                    jDate to gDate
                }
                RecurrenceFrequency.WEEKLY -> {
                    val cal = Calendar.getInstance().apply {
                        set(initialGregorian.year, initialGregorian.month - 1, initialGregorian.day)
                        add(Calendar.DAY_OF_YEAR, step * 7)
                    }
                    val jDate = PersianCalendarHelper.gregorianToJalali(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                    val gDate = "%04d-%02d-%02d".format(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                    jDate to gDate
                }
                RecurrenceFrequency.MONTHLY -> {
                    val totalMonths = (startJMonth - 1) + step
                    val curYear = startJYear + (totalMonths / 12)
                    val curMonth = (totalMonths % 12) + 1
                    val daysInMonth = PersianCalendarHelper.getDaysInJalaliMonth(curYear, curMonth)
                    val curDay = startJDay.coerceAtMost(daysInMonth)
                    val jDate = JalaliDate(curYear, curMonth, curDay)
                    val g = PersianCalendarHelper.jalaliToGregorian(curYear, curMonth, curDay)
                    jDate to g.formatted
                }
                RecurrenceFrequency.YEARLY -> {
                    val curYear = startJYear + step
                    val daysInMonth = PersianCalendarHelper.getDaysInJalaliMonth(curYear, startJMonth)
                    val curDay = startJDay.coerceAtMost(daysInMonth)
                    val jDate = JalaliDate(curYear, startJMonth, curDay)
                    val g = PersianCalendarHelper.jalaliToGregorian(curYear, startJMonth, curDay)
                    jDate to g.formatted
                }
                RecurrenceFrequency.NONE -> {
                    JalaliDate(startJYear, startJMonth, startJDay) to initialGregorian.formatted
                }
            }

            // شرط پایان بر اساس تاریخ مشخص
            if (config.endType == RecurrenceEndType.UNTIL_DATE && config.untilDate.isNotBlank()) {
                val normalizedUntil = PersianCalendarHelper.normalizePersianDate(config.untilDate)
                if (currentJalali.formatted > normalizedUntil) {
                    break
                }
            }

            events.add(
                baseEvent.copy(
                    id = if (step == 0) baseEvent.id else UUID.randomUUID().toString(),
                    persianDate = currentJalali.formatted,
                    gregorianDate = gDateFormatted,
                    recurrenceId = recurrenceGroupId,
                    recurrenceRule = ruleName
                )
            )
        }

        return events
    }
}
