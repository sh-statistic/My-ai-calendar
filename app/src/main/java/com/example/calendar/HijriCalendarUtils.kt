package com.example.calendar

import kotlin.math.ceil
import kotlin.math.floor

/**
 * مبدّل تقویم هجری قمری ↔ هجری شمسی (جلالی)
 *
 * مناسبت‌های مذهبی بر اساس تاریخ قمری ثابت هستند (مثلاً عاشورا همیشه ۱۰ محرم است).
 * این کلاس تاریخ‌های قمری را برای هر سال شمسی به صورت خودکار محاسبه می‌کند
 * و دیگر نیازی به آپدیت دستی نیست.
 *
 * از الگوریتم تقویم جدولی اسلامی (Tabular Islamic Calendar) استفاده می‌کند.
 * توجه: تقویم رسمی ایران رصدی (بر اساس رؤیت هلال ماه) است و ممکن است
 * تا ±۱ روز با محاسبات جدولی تفاوت داشته باشد.
 */
object HijriCalendarUtils {

    data class SimpleDate(val year: Int, val month: Int, val day: Int)

    // ========== ثوابت تقویمی ==========
    // Persian (Jalali) epoch: JDN of reference point for the 2820-year cycle
    private const val PERSIAN_EPOCH = 1948320L
    // Islamic (Hijri) epoch: JDN for 1 Muharram 1 AH
    private const val ISLAMIC_EPOCH = 1948439L

    // ========== مناسبت‌های مذهبی بر اساس تاریخ ثابت قمری ==========

    data class HijriOccasion(
        val hijriMonth: Int,   // ماه قمری (۱=محرم ... ۱۲=ذی‌الحجه)
        val hijriDay: Int,     // روز قمری
        val title: String,
        val isHoliday: Boolean = true
    )

    /**
     * لیست تمام مناسبت‌های مذهبی تعطیل رسمی
     * این تاریخ‌ها در تقویم قمری ثابت هستند و هرگز تغییر نمی‌کنند.
     */
    val HIJRI_OCCASIONS = listOf(
        // ─── محرم (ماه ۱) ───
        HijriOccasion(1, 9, "تاسوعای حسینی"),
        HijriOccasion(1, 10, "عاشورای حسینی"),
        HijriOccasion(1, 12, "شهادت حضرت امام زین‌العابدین (ع)"),

        // ─── صفر (ماه ۲) ───
        HijriOccasion(2, 20, "اربعین حسینی"),
        HijriOccasion(2, 28, "رحلت حضرت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)"),
        HijriOccasion(2, 29, "شهادت حضرت امام رضا (ع)"),

        // ─── ربیع‌الاول (ماه ۳) ───
        HijriOccasion(3, 8, "شهادت حضرت امام حسن عسکری (ع) و آغاز امامت حضرت مهدی (عج)"),
        HijriOccasion(3, 17, "میلاد حضرت رسول اکرم (ص) و امام جعفر صادق (ع) / هفته وحدت"),

        // ─── جمادی‌الثانی (ماه ۶) ───
        HijriOccasion(6, 3, "شهادت حضرت فاطمه زهرا (س)"),

        // ─── رجب (ماه ۷) ───
        HijriOccasion(7, 13, "ولادت حضرت امام علی (ع) و روز پدر"),
        HijriOccasion(7, 27, "مبعث حضرت رسول اکرم (ص)"),

        // ─── شعبان (ماه ۸) ───
        HijriOccasion(8, 15, "ولادت با سعادت حضرت قائم عجل‌الله تعالی فرجه (نیمه شعبان)"),

        // ─── رمضان (ماه ۹) ───
        HijriOccasion(9, 21, "شهادت حضرت علی (ع)"),

        // ─── شوال (ماه ۱۰) ───
        HijriOccasion(10, 1, "عید سعید فطر"),
        HijriOccasion(10, 2, "تعطیلی به مناسبت عید سعید فطر"),
        HijriOccasion(10, 25, "شهادت حضرت امام جعفر صادق (ع)"),

        // ─── ذی‌الحجه (ماه ۱۲) ───
        HijriOccasion(12, 10, "عید سعید قربان"),
        HijriOccasion(12, 18, "عید سعید غدیر خم")
    )

    // ========== تبدیل تقویم جلالی (شمسی) ↔ عدد روز ژولیَن (JDN) ==========
    // الگوریتم FourMiLab Calendar Converter — دقیق برای تمام سال‌ها

    private fun positiveModulo(a: Int, b: Int): Int {
        val result = a % b
        return if (result < 0) result + b else result
    }

    private fun positiveModulo(a: Long, b: Long): Long {
        val result = a % b
        return if (result < 0) result + b else result
    }

    /**
     * تبدیل تاریخ جلالی (شمسی) به عدد روز ژولیَن (Julian Day Number)
     */
    fun persianToJdn(year: Int, month: Int, day: Int): Long {
        val epbase = year - if (year >= 0) 474 else 473
        val epyear = 474 + positiveModulo(epbase, 2820)

        val daysPassed = if (month <= 7) (month - 1) * 31L else 186L + (month - 7) * 30L

        return day.toLong() +
            daysPassed +
            (epyear.toLong() * 682L - 110L) / 2816L +
            (epyear - 1L) * 365L +
            floorDiv(epbase.toLong(), 2820L) * 1029983L +
            PERSIAN_EPOCH
    }

    /**
     * تبدیل عدد روز ژولیَن به تاریخ جلالی (شمسی)
     */
    fun jdnToPersian(jdn: Long): SimpleDate {
        val depoch = jdn - persianToJdn(475, 1, 1)
        val cycle = floorDiv(depoch, 1029983L)
        val cyear = positiveModulo(depoch, 1029983L)

        val ycycle: Long
        if (cyear == 1029982L) {
            ycycle = 2820L
        } else {
            val aux1 = cyear / 366L
            val aux2 = cyear % 366L
            ycycle = (2134L * aux1 + 2816L * aux2 + 2815L) / 1028522L + aux1 + 1L
        }

        var year = (ycycle + 2820L * cycle + 474L).toInt()
        if (year <= 0) year--

        val yday = (jdn - persianToJdn(year, 1, 1) + 1).toInt()

        val month = if (yday <= 186) {
            ceil(yday / 31.0).toInt()
        } else {
            ceil((yday - 186) / 30.0).toInt() + 6
        }

        val day = (jdn - persianToJdn(year, month, 1) + 1).toInt()

        return SimpleDate(year, month, day)
    }

    // ========== تبدیل تقویم هجری قمری ↔ JDN (الگوریتم جدولی) ==========

    /**
     * تبدیل تاریخ هجری قمری به عدد روز ژولیَن
     */
    fun islamicToJdn(year: Int, month: Int, day: Int): Long {
        return day.toLong() +
            ceil(29.5001 * (month - 1)).toLong() +
            (year - 1).toLong() * 354L +
            floor((3.0 + 11.0 * year) / 30.0).toLong() +
            ISLAMIC_EPOCH - 1L
    }

    /**
     * تبدیل عدد روز ژولیَن به تاریخ هجری قمری
     */
    fun jdnToIslamic(jdn: Long): SimpleDate {
        // -1L aligns the tabular algorithm with the typical Iranian observational calendar (Geophysics)
        val adjustedJdn = jdn - 1L + HijriOffsetManager.currentOffset
        val l = adjustedJdn - ISLAMIC_EPOCH + 1
        val year = ((30L * l + 10646L) / 10631L).toInt()
        var month = (ceil((l - 29L - islamicToJdn(year, 1, 1) + ISLAMIC_EPOCH).toDouble() / 29.5) + 1).toInt()
        if (month < 1) month = 1
        if (month > 12) month = 12
        val day = (adjustedJdn - islamicToJdn(year, month, 1) + 1).toInt()
        return SimpleDate(year, month, day)
    }

    /**
     * تبدیل تاریخ شمسی به هجری قمری
     */
    fun persianToIslamic(year: Int, month: Int, day: Int): SimpleDate {
        return jdnToIslamic(persianToJdn(year, month, day))
    }

    // ========== توابع کمکی ==========

    /** آیا سال شمسی کبیسه است؟ */
    fun isPersianLeapYear(jy: Int): Boolean {
        val base = if (jy > 0) jy - 474 else jy - 473
        return positiveModulo((base + 38) * 682, 2816) < 682
    }

    /** تقسیم عدد صحیح به سمت منفی بی‌نهایت (floor division) */
    private fun floorDiv(a: Long, b: Long): Long {
        val d = a / b
        return if (a xor b < 0 && d * b != a) d - 1 else d
    }

    // ========== محاسبه خودکار مناسبت‌های مذهبی برای هر سال شمسی ==========

    // کش نتایج برای جلوگیری از محاسبه مجدد
    private val cache = mutableMapOf<Int, Map<String, List<PersianOccasion>>>()

    /**
     * محاسبه تمام مناسبت‌های مذهبی (قمری) برای یک سال شمسی مشخص.
     * تاریخ‌های قمری ثابت هستند و این تابع آن‌ها را به صورت خودکار
     * به تاریخ شمسی معادل تبدیل می‌کند.
     *
     * @param jalaliYear سال شمسی (مثلاً ۱۴۰۵)
     * @return نگاشت از "ماه-روز" شمسی به لیست مناسبت‌ها
     */
    fun getOccasionsForYear(jalaliYear: Int): Map<String, List<PersianOccasion>> {
        cache[jalaliYear]?.let { return it }

        val result = mutableMapOf<String, MutableList<PersianOccasion>>()

        // محاسبه محدوده JDN برای این سال شمسی
        val yearStart = persianToJdn(jalaliYear, 1, 1)
        val lastDay = if (isPersianLeapYear(jalaliYear)) 30 else 29
        val yearEnd = persianToJdn(jalaliYear, 12, lastDay)

        // پیدا کردن سال‌های قمری که با این سال شمسی همپوشانی دارند
        val hijriStart = jdnToIslamic(yearStart)
        val hijriEnd = jdnToIslamic(yearEnd)

        // برای هر سال قمری همپوشان، مناسبت‌ها را محاسبه کن
        for (hijriYear in hijriStart.year..hijriEnd.year) {
            for (occasion in HIJRI_OCCASIONS) {
                val jdn = islamicToJdn(hijriYear, occasion.hijriMonth, occasion.hijriDay)
                val adjustedJdn = jdn - HijriOffsetManager.currentOffset

                // آیا این تاریخ در محدوده سال شمسی ماست؟
                if (adjustedJdn in yearStart..yearEnd) {
                    val jalaliDate = jdnToPersian(adjustedJdn)
                    val key = "${jalaliDate.month}-${jalaliDate.day}"

                    val persianOccasion = PersianOccasion(
                        title = occasion.title,
                        isHoliday = occasion.isHoliday,
                        isReligious = true
                    )

                    result.getOrPut(key) { mutableListOf() }.add(persianOccasion)
                }
            }
        }

        // ذخیره در کش
        cache[jalaliYear] = result
        return result
    }

    /** پاک‌سازی کش (در صورت نیاز) */
    fun clearCache() {
        cache.clear()
    }
}
