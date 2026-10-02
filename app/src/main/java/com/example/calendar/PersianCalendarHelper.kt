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

    fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        
        val gy = gYear - 1600
        val gm = gMonth - 1
        val gd = gDay - 1

        var gDayNo = 365 * gy + Math.floorDiv(gy + 3, 4) - Math.floorDiv(gy + 99, 100) + Math.floorDiv(gy + 399, 400)
        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79
        val jNp = Math.floorDiv(jDayNo, 12053)
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * Math.floorDiv(jDayNo, 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += Math.floorDiv(jDayNo - 1, 365)
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        while (jm < 11 && jDayNo >= jDaysInMonth[jm]) {
            jDayNo -= jDaysInMonth[jm]
            jm++
        }
        return JalaliDate(jy, jm + 1, jDayNo + 1)
    }

    fun jalaliToGregorian(jYear: Int, jMonth: Int, jDay: Int): GregorianDate {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        
        val jy = jYear - 979
        val jm = jMonth - 1
        val jd = jDay - 1

        var jDayNo = 365 * jy + Math.floorDiv(jy, 33) * 8 + Math.floorDiv((jy % 33) + 3, 4)
        for (i in 0 until jm) {
            jDayNo += jDaysInMonth[i]
        }
        jDayNo += jd

        var gDayNo = jDayNo + 79
        var gy = 1600 + 400 * Math.floorDiv(gDayNo, 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * Math.floorDiv(gDayNo, 36524)
            gDayNo %= 36524
            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * Math.floorDiv(gDayNo, 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += Math.floorDiv(gDayNo, 365)
            gDayNo %= 365
        }

        var gm = 0
        while (gm < 11) {
            var days = gDaysInMonth[gm]
            if (gm == 1 && leap) {
                days++
            }
            if (gDayNo >= days) {
                gDayNo -= days
                gm++
            } else {
                break
            }
        }

        return GregorianDate(gy, gm + 1, gDayNo + 1)
    }

    fun getDaysInJalaliMonth(year: Int, month: Int): Int {
        return when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            month == 12 -> if (isJalaliLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    private val OFFICIAL_LEAP_YEARS = setOf(
        1210, 1214, 1218, 1222, 1226, 1230, 1234, 1238, 1243, 1247, 1251, 1255, 1259, 1263, 1267, 1271, 1276, 1280, 1284, 1288, 1292, 1296, 1300, 1304, 1309, 1313, 1317, 1321, 1325, 1329, 1333, 1337, 1342, 1346, 1350, 1354, 1358, 1362, 1366, 1370, 1375, 1379, 1383, 1387, 1391, 1395, 1399, 1403, 1408, 1412, 1416, 1420, 1424, 1428, 1432, 1436, 1441, 1445, 1449, 1453, 1457, 1461, 1465, 1469, 1474, 1478, 1482, 1486, 1490, 1494, 1498
    )

    fun isJalaliLeapYear(year: Int): Boolean {
        if (year in 1206..1498) {
            return OFFICIAL_LEAP_YEARS.contains(year)
        }
        // Fallback for years outside the provided official table
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
        val greg = jalaliToGregorian(jYear, jMonth, jDay)
        val cal = Calendar.getInstance()
        cal.set(greg.year, greg.month - 1, greg.day)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        // Calendar.SATURDAY = 7 -> 0
        // Calendar.SUNDAY = 1 -> 1
        // Calendar.MONDAY = 2 -> 2
        // ...
        // Calendar.FRIDAY = 6 -> 6
        return dow % 7
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
