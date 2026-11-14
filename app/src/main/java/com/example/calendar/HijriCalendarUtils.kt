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
     * لیست مناسبت‌های مذهبی بر اساس تاریخ ثابت قمری؛ تعطیل‌های رسمی با isHoliday = true
     * و مناسبت‌های غیرتعطیل با isHoliday = false مشخص شده‌اند.
     */
    val HIJRI_OCCASIONS = listOf(
        // ─── محرم (ماه ۱) ───
        HijriOccasion(1, 9, "تاسوعای حسینی"),
        HijriOccasion(1, 10, "عاشورای حسینی"),
        HijriOccasion(1, 12, "شهادت حضرت امام زین‌العابدین (ع)", isHoliday = false), // مناسبت است، تعطیل رسمی نیست

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
        HijriOccasion(12, 18, "عید سعید غدیر خم"),

        // ─── مناسبت‌های غیرتعطیل (از تقویم رسمی؛ داده بادصبا) ───
        // ─── محرم (غیرتعطیل) ───
        HijriOccasion(1, 1, "آغاز سال هجری قمری", isHoliday = false),
        HijriOccasion(1, 1, "روز شعر و ادبیات آیینی", isHoliday = false),
        HijriOccasion(1, 1, "روز بزرگداشت محتشم کاشانی", isHoliday = false),
        HijriOccasion(1, 2, "ورود امام حسین علیه السلام به کربلا (61 هـ ق)", isHoliday = false),
        HijriOccasion(1, 2, "روز امر به معروف و نهی از منکر", isHoliday = false),
        HijriOccasion(1, 11, "روز تجلیل از اسرا و مفقودان", isHoliday = false),
        HijriOccasion(1, 12, "ورود اسیران کربلا به کوفه(61 هـ ق)", isHoliday = false),
        HijriOccasion(1, 25, "شهادت حضرت امام زین العابدین علیه السلام (95 هـ ق - به روایتی)", isHoliday = false),
        HijriOccasion(1, 25, "یادروز قیام مردم سیستان به خون خواهی شهدای کربلا (62 هـ ق)", isHoliday = false),
        // ─── صفر (غیرتعطیل) ───
        HijriOccasion(2, 3, "ولادت حضرت امام محمدباقر علیه السلام( 57 هـ ق - به روایتی)", isHoliday = false),
        HijriOccasion(2, 5, "شهادت حضرت رقیه (س) دختر سه ساله امام حسین علیه السلام(61 هـ ق)", isHoliday = false),
        HijriOccasion(2, 7, "شهادت حضرت امام حسن مجتبی علیه السلام (50 هـ ق) (به روایتی)", isHoliday = false),
        HijriOccasion(2, 7, "روز بزرگداشت سلمان فارسی", isHoliday = false),
        HijriOccasion(2, 9, "شهادت عمار بن یاسر(37 هـ ق)", isHoliday = false),
        HijriOccasion(2, 20, "ورود جابربن عبدالله انصاری به کربلا (61 هـ ق)", isHoliday = false),
        // ─── ربیع‌الاول (غیرتعطیل) ───
        HijriOccasion(3, 1, "هجرت حضرت رسول اکرم صلی الله علیه و آله از مکه به مدینه", isHoliday = false),
        HijriOccasion(3, 1, "شهادت حضرت امام حسن عسکری علیه السلام (به روایتی)", isHoliday = false),
        HijriOccasion(3, 4, "خروج حضرت رسول اکرم صلی الله علیه و آله از غار(ثور) و حرکت به سوی مدینه (1 هـ ق)", isHoliday = false),
        HijriOccasion(3, 5, "وفات سکینه خاتون دختر امام حسین علیه السلام (117 هـ ق)", isHoliday = false),
        HijriOccasion(3, 10, "ازدواج رسول اکرم صلی الله علیه و آله با حضرت خدیجه کبری سلام الله علیها", isHoliday = false),
        HijriOccasion(3, 12, "ولادت حضرت رسول اکرم صلی الله علیه و آله به روایت اهل سنت(53 سال قبل از هجرت) -  آغاز هفته وحدت", isHoliday = false),
        HijriOccasion(3, 16, "روز وقف", isHoliday = false),
        HijriOccasion(3, 22, "غزوه بنی النضیر", isHoliday = false),
        HijriOccasion(3, 26, "صلح حضرت امام حسن مجتبی علیه السلام (41 هـ ق)", isHoliday = false),
        // ─── ربیع‌الثانی (غیرتعطیل) ───
        HijriOccasion(4, 4, "ولادت حضرت عبدالعظیم حسنی علیه السلام", isHoliday = false),
        HijriOccasion(4, 8, "ولادت حضرت امام حسن عسکری علیه السلام (232 هـ ق)", isHoliday = false),
        HijriOccasion(4, 10, "وفات حضرت معصومه سلام الله علیها (201 هـ ق)", isHoliday = false),
        HijriOccasion(4, 10, "میلاد حضرت امام حسن عسکری علیه السلام به روایتی (232 هـ ق)", isHoliday = false),
        // ─── جمادی‌الاول (غیرتعطیل) ───
        HijriOccasion(5, 5, "ولادت حضرت زینب سلام الله علیها (5 هـ ق) و روز پرستار", isHoliday = false),
        HijriOccasion(5, 10, "جنگ جمل(36 هـ ق)", isHoliday = false),
        HijriOccasion(5, 13, "شهادت حضرت فاطمة زهرا سلام االله علیها (11 هـ ق) به روایتی", isHoliday = false),
        HijriOccasion(5, 15, "ولادت حضرت سجاد علیه السلام به روایتی (38 هـ ق)", isHoliday = false),
        HijriOccasion(5, 27, "رحلت حضرت عبدالمطلب (ع) جد پیامبر اکرم حضرت محمد صلی الله علیه و آله", isHoliday = false),
        // ─── جمادی‌الثانی (غیرتعطیل) ───
        HijriOccasion(6, 13, "سالروز وفات حضرت ام البنین سلام الله علیها", isHoliday = false),
        HijriOccasion(6, 13, "روز تکریم مادران و همسران شهدا", isHoliday = false),
        HijriOccasion(6, 20, "ولادت حضرت فاطمه زهرا سلام الله علیها (هشتم قبل از هجرت) و روز زن", isHoliday = false),
        HijriOccasion(6, 20, "تولد حضرت امام خمینی(رحمة الله علیه) رهبر کبیر انقلاب اسلامی(1320 هـ ق)", isHoliday = false),
        HijriOccasion(6, 29, "رحلت حضرت ام کلثوم سلام الله علیها، دختر بزرگوار امام علی علیه اسلام و حضرت فاطمه زهرا سلام الله علیها", isHoliday = false),
        // ─── رجب (غیرتعطیل) ───
        HijriOccasion(7, 1, "ولادت حضرت امام محمد باقر علیه السلام (57 هـ ق)", isHoliday = false),
        HijriOccasion(7, 1, "شروع ماه رجب", isHoliday = false),
        HijriOccasion(7, 3, "شهادت حضرت امام علی النقی الهادی علیه السلام (254 هـ ق)", isHoliday = false),
        HijriOccasion(7, 6, "لیلة الرغائب (شب آرزوها)", isHoliday = false),
        HijriOccasion(7, 10, "ولادت حضرت امام محمد تقی علیه السلام (جواد الائمه) (195 هـ ق)", isHoliday = false),
        HijriOccasion(7, 13, "آغاز ایام البیض (اعتکاف)", isHoliday = false),
        HijriOccasion(7, 15, "ارتحال حضرت زینب سلام الله علیها (62 هـ ق)", isHoliday = false),
        HijriOccasion(7, 15, "تغییر قبله مسلمین از بیت المقدس به مکه معظمه(2 هـ ق)", isHoliday = false),
        HijriOccasion(7, 18, "وفات ابراهیم فرزند پیامبر اکرم صلی الله علیه و آله (10 هـ ق)", isHoliday = false),
        HijriOccasion(7, 24, "فتح خیبر توسط حضرت امیرالمومنین علی علیه السلام (7 هـ ق)", isHoliday = false),
        HijriOccasion(7, 25, "شهادت حضرت امام موسی کاظم علیه السلام (183 هـ ق)", isHoliday = false),
        HijriOccasion(7, 26, "وفات حضرت ابوطالب عموی پیامبر اکرم (صلی الله علیه و آله) و پدر امیرالمومنین علیه السلام (3 سال قبل از هجرت)", isHoliday = false),
        // ─── شعبان (غیرتعطیل) ───
        HijriOccasion(8, 3, "ولادت حضرت امام حسین علیه السلام ( 4 هـ ق) و روز پاسدار", isHoliday = false),
        HijriOccasion(8, 4, "ولادت حضرت ابوالفضل العباس علیه السلام ( 26 هـ ق) و روز جانباز", isHoliday = false),
        HijriOccasion(8, 5, "ولادت حضرت امام زین العابدین علیه السلام 38 ه ق- روز صحیفه سجادیه", isHoliday = false),
        HijriOccasion(8, 11, "ولادت حضرت علی اکبر علیه السلام (33 هـ ق) و روز جوان", isHoliday = false),
        HijriOccasion(8, 15, "روز جهانی مستضعفان", isHoliday = false),
        HijriOccasion(8, 15, "روز سربازان گمنام امام زمان(عجل الله تعالی فرجه)", isHoliday = false),
        HijriOccasion(8, 19, "غزوه بنی المصطلق(6 هـ ق)", isHoliday = false),
        // ─── رمضان (غیرتعطیل) ───
        HijriOccasion(9, 1, "شروع ماه رمضان", isHoliday = false),
        HijriOccasion(9, 7, "روز جهانی قدس (آخرین جمعه ماه رمضان)", isHoliday = false),
        HijriOccasion(9, 10, "وفات حضرت خدیجه کبری سلام الله علیها (3 سال قبل از هجرت)", isHoliday = false),
        HijriOccasion(9, 15, "ولادت حضرت امام حسن مجتبی علیه السلام (3هـ ق)", isHoliday = false),
        HijriOccasion(9, 15, "روز اکرام و تکریم خیرین", isHoliday = false),
        HijriOccasion(9, 17, "معراج رسول اکرم (صلی الله علیه و آله)  (6 ماه قبل از هجرت)", isHoliday = false),
        HijriOccasion(9, 17, "جنگ بدر(2 هـ ق)", isHoliday = false),
        HijriOccasion(9, 18, "اولین شب قدر", isHoliday = false),
        HijriOccasion(9, 19, "ضربت خوردن حضرت علی علیه السلام (40 هـ ق)", isHoliday = false),
        HijriOccasion(9, 19, "روز نهج البلاغه", isHoliday = false),
        HijriOccasion(9, 20, "فتح مکه(8 هـ ق)", isHoliday = false),
        HijriOccasion(9, 20, "دومین شب قدر", isHoliday = false),
        HijriOccasion(9, 22, "سومین شب قدر", isHoliday = false),
        // ─── شوال (غیرتعطیل) ───
        HijriOccasion(10, 5, "ورود جناب مسلم ابن عقیل به کوفه(60 هـ ق)", isHoliday = false),
        HijriOccasion(10, 8, "تخریب قبور ائمه بقیع علیهم السلام", isHoliday = false),
        HijriOccasion(10, 15, "شهادت حضرت حمزه (3 هـ ق)", isHoliday = false),
        HijriOccasion(10, 15, "وفات حضرت عبدالعظیم حسنی (252 هـ ق)", isHoliday = false),
        HijriOccasion(10, 17, "غزوه خندق (5 هـ ق)", isHoliday = false),
        HijriOccasion(10, 17, "روز فرهنگ پهلوانی و ورزش زورخانه ای", isHoliday = false),
        HijriOccasion(10, 21, "فتح اندلس به دست مسلمانان (92 هـ ق)", isHoliday = false),
        // ─── ذی‌القعده (غیرتعطیل) ───
        HijriOccasion(11, 1, "روز میلاد حضرت معصومه (س) و روز دختران - آغاز دهه کرامت", isHoliday = false),
        HijriOccasion(11, 5, "روز تجلیل از امامزادگان و بقاع متبرکه", isHoliday = false),
        HijriOccasion(11, 5, "روز بزرگداشت حضرت صالح بن موسی کاظم علیه السلام", isHoliday = false),
        HijriOccasion(11, 6, "روز بزرگداشت حضرت احمدبن موسی شاهچراغ علیه السلام", isHoliday = false),
        HijriOccasion(11, 11, "ولادت حضرت امام رضا علیه السلام (148 هـ ق)", isHoliday = false),
        HijriOccasion(11, 23, "شهادت حضرت امام رضا علیه السلام به روایتی (202 هـ ق)", isHoliday = false),
        HijriOccasion(11, 23, "غزوه بنی قریظه", isHoliday = false),
        HijriOccasion(11, 25, "روز دحوالارض-حرکت امام رضا از مدینه به سمت خراسان(200 هـ ق)", isHoliday = false),
        HijriOccasion(11, 29, "شهادت حضرت امام محمد تقی علیه السلام (جواد الائمه)  (220 هـ ق)", isHoliday = false),
        // ─── ذی‌الحجه (غیرتعطیل) ───
        HijriOccasion(12, 1, "روز ازدواج", isHoliday = false),
        HijriOccasion(12, 1, "سالروز ازدواج حضرت امام علی علیه السلام و حضرت فاطمه سلام االله علیها ( 2 هـ ق)", isHoliday = false),
        HijriOccasion(12, 3, "ورود پیامبر اکرم (صلی الله علیه و آله)به مکه در سفر حجه الوداع(10 هـ ق)", isHoliday = false),
        HijriOccasion(12, 6, "شهادت مظلومانه زائران خانه خدا به دست ماموران آل سعود (1366 ه ش برابر با 6 ذی الحجه 1407 هـ ق)", isHoliday = false),
        HijriOccasion(12, 7, "شهادت حضرت  امام محمد باقر علیه السلام (114 هـ ق)", isHoliday = false),
        HijriOccasion(12, 8, "حرکت حضرت امام حسین علیه السلام از مکه بسوی کربلا (60 هـ ق)", isHoliday = false),
        HijriOccasion(12, 9, "روز عرفه (روز نیایش)", isHoliday = false),
        HijriOccasion(12, 9, "شهادت مسلم بن عقیل و هانی ابن عروه(60 هـ ق)", isHoliday = false),
        HijriOccasion(12, 15, "ولادت حضرت امام علی النقی الهادی علیه السلام (212 هـ ق)", isHoliday = false),
        HijriOccasion(12, 20, "ولادت حضرت امام موسی کاظم علیه السلام (128 هـ ق)", isHoliday = false),
        HijriOccasion(12, 22, "شهادت میثم تمار(60 هـ ق)", isHoliday = false),
        HijriOccasion(12, 24, "روز مباهله پیامبر اسلام صلی الله علیه و آله (10 هـ ق)", isHoliday = false),
        HijriOccasion(12, 25, "روز خانواده و تکریم بازنشستگان", isHoliday = false),
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

    /** آیا سال قمری در دوره ۳۰ ساله کبیسه است؟ */
    fun isIslamicLeap(year: Int): Boolean {
        return ((11 * year + 14) % 30) < 11
    }

    /**
     * تبدیل تاریخ هجری قمری به عدد روز ژولیَن
     */
    fun islamicToJdn(year: Int, month: Int, day: Int): Long {
        return day.toLong() +
            ceil(29.5 * (month - 1)).toLong() +
            (year - 1).toLong() * 354L +
            floor((3.0 + 11.0 * year) / 30.0).toLong() +
            ISLAMIC_EPOCH - 1L
    }

    /**
     * تبدیل عدد روز ژولیَن به تاریخ هجری قمری دقیق بدون خطای روز منفی یا صفر
     */
    fun jdnToIslamic(jdn: Long): SimpleDate {
        // -1L aligns the tabular algorithm with the typical Iranian observational calendar (Geophysics)
        val adjustedJdn = jdn - 1L + HijriOffsetManager.currentOffset
        var year = ((30L * (adjustedJdn - ISLAMIC_EPOCH) + 10646L) / 10631L).toInt()
        var dayOfYear = (adjustedJdn - islamicToJdn(year, 1, 1) + 1).toInt()
        while (dayOfYear <= 0) {
            year--
            dayOfYear = (adjustedJdn - islamicToJdn(year, 1, 1) + 1).toInt()
        }
        var month = 1
        while (month <= 12) {
            val daysInM = if (month % 2 == 1) 30 else if (month == 12 && isIslamicLeap(year)) 30 else 29
            if (dayOfYear <= daysInM) break
            dayOfYear -= daysInM
            month++
        }
        return SimpleDate(year, month, dayOfYear)
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
        return PersianCalendarHelper.isJalaliLeapYear(jy)
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
     *
     * جایگذاری از همان مسیر تبدیل [persianToIslamic] انجام می‌شود که تاریخ قمری
     * نمایشی را تولید می‌کند؛ بنابراین مناسبت هر روز دقیقاً روی تاریخی می‌نشیند
     * که کاربر در تقویم می‌بیند و دو مسیر هرگز از هم جدا نمی‌شوند.
     *
     * @param jalaliYear سال شمسی (مثلاً ۱۴۰۵)
     * @return نگاشت از "ماه-روز" شمسی به لیست مناسبت‌ها
     */
    fun getOccasionsForYear(jalaliYear: Int): Map<String, List<PersianOccasion>> {
        cache[jalaliYear]?.let { return it }

        val result = mutableMapOf<String, MutableList<PersianOccasion>>()
        val byHijriDay = HIJRI_OCCASIONS.groupBy { it.hijriMonth to it.hijriDay }

        var month = 1
        var day = 1
        val daysInYear = if (isPersianLeapYear(jalaliYear)) 366 else 365
        repeat(daysInYear) {
            val hijri = persianToIslamic(jalaliYear, month, day)
            byHijriDay[hijri.month to hijri.day]?.let { todays ->
                val list = result.getOrPut("$month-$day") { mutableListOf() }
                for (occasion in todays) {
                    list.add(
                        PersianOccasion(
                            title = occasion.title,
                            isHoliday = occasion.isHoliday,
                            isReligious = true
                        )
                    )
                }
            }
            val daysInMonth = when {
                month <= 6 -> 31
                month <= 11 -> 30
                else -> if (isPersianLeapYear(jalaliYear)) 30 else 29
            }
            day++
            if (day > daysInMonth) {
                day = 1
                month++
            }
        }

        cache[jalaliYear] = result
        return result
    }

    /** پاک‌سازی کش (در صورت نیاز) */
    fun clearCache() {
        cache.clear()
    }
}
