package com.example.calendar

import java.util.Calendar

data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    val formatted: String get() = "%04d/%02d/%02d".format(year, month, day)
    val formattedPersian: String get() = PersianCalendarHelper.toPersianDigits(formatted)
    val monthName: String get() = PersianCalendarHelper.PERSIAN_MONTH_NAMES.getOrElse(month - 1) { "" }
}

data class GregorianDate(val year: Int, val month: Int, val day: Int) {
    val formatted: String get() = "%04d-%02d-%02d".format(year, month, day)
    val monthName: String get() = PersianCalendarHelper.GREGORIAN_MONTH_NAMES.getOrElse(month - 1) { "" }
}

object PersianCalendarHelper {

    val PERSIAN_MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val GREGORIAN_MONTH_NAMES = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    val WEEKDAY_NAMES_PERSIAN = listOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    val WEEKDAY_ABBR = listOf(
        "ش", "ی", "د", "س", "چ", "پ", "ج"
    )

    fun toPersianDigits(text: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = java.lang.StringBuilder()
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun gregorianToJdn(year: Int, month: Int, day: Int): Long {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day + (153 * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045
    }

    fun jdnToGregorian(jdn: Long): GregorianDate {
        val a = jdn + 32044
        val b = (4 * a + 3) / 146097
        val c = a - (146097 * b) / 4
        val d = (4 * c + 3) / 1461
        val e = c - (1461 * d) / 4
        val m = (5 * e + 2) / 153
        val day = (e - (153 * m + 2) / 5 + 1).toInt()
        val month = (m + 3 - 12 * (m / 10)).toInt()
        val year = (100 * b + d - 4800 + m / 10).toInt()
        return GregorianDate(year, month, day)
    }

    fun jalaliToJdn(year: Int, month: Int, day: Int): Long {
        val epBase = year - 474
        val epYear = 474 + ((epBase % 2820) + 2820) % 2820
        val md = if (month <= 7) (month - 1) * 31 else (month - 1) * 30 + 6
        return day.toLong() + md + ((epYear * 682) - 110) / 2816 + (epYear - 1) * 365L + (epBase / 2820) * 1029983L + (1948320 - 1)
    }

    fun jdnToJalali(jdn: Long): JalaliDate {
        val dep = jdn - 2121445L
        val cycle = dep / 1029983L
        val cDay = ((dep % 1029983L) + 1029983L) % 1029983L
        var yCycle = 0L
        if (cDay == 1029982L) {
            yCycle = 2820
        } else {
            val aux1 = cDay / 366L
            val aux2 = cDay % 366L
            yCycle = ((2134L * aux1 + 2816L * aux2 + 2815L) / 1028522L) + aux1 + 1
        }
        val year = (yCycle + 2820 * cycle + 474).toInt()
        val dayOfYear = (jdn - jalaliToJdn(year, 1, 1) + 1).toInt()
        val month: Int
        val day: Int
        if (dayOfYear <= 186) {
            month = Math.max(1, Math.min(6, (Math.ceil(dayOfYear / 31.0)).toInt()))
            val rem = dayOfYear % 31
            day = if (rem == 0) 31 else rem
        } else {
            val remOfYear = dayOfYear - 186
            month = Math.max(7, Math.min(12, 6 + (Math.ceil(remOfYear / 30.0)).toInt()))
            val rem = remOfYear % 30
            day = if (rem == 0) 30 else rem
        }
        return JalaliDate(year, month, day)
    }

    fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): JalaliDate {
        val jdn = gregorianToJdn(gYear, gMonth, gDay)
        return jdnToJalali(jdn)
    }

    fun jalaliToGregorian(jYear: Int, jMonth: Int, jDay: Int): GregorianDate {
        val jdn = jalaliToJdn(jYear, jMonth, jDay)
        return jdnToGregorian(jdn)
    }

    fun getDaysInJalaliMonth(year: Int, month: Int): Int {
        return when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            month == 12 -> if (isJalaliLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    fun isJalaliLeapYear(year: Int): Boolean {
        val a = year - 474
        val b = ((a % 2820) + 2820) % 2820 + 474
        return ((b + 38) * 682) % 2816 < 682
    }

    /**
     * Persian day of week index:
     * 0 -> شنبه (Saturday)
     * 1 -> یکشنبه (Sunday)
     * ...
     * 6 -> جمعه (Friday)
     */
    fun getPersianDayOfWeek(jYear: Int, jMonth: Int, jDay: Int): Int {
        val jdn = jalaliToJdn(jYear, jMonth, jDay)
        // Saturday is jdn % 7 == 1, so (jdn + 1 + 1) % 7
        val dow = ((jdn + 2) % 7).toInt()
        return if (dow < 0) dow + 7 else dow
    }

    fun getCurrentJalaliDate(): JalaliDate {
        val cal = Calendar.getInstance()
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        return gregorianToJalali(gy, gm, gd)
    }

    fun getCurrentGregorianDate(): GregorianDate {
        val cal = Calendar.getInstance()
        return GregorianDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
