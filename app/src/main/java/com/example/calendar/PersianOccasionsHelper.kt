package com.example.calendar

data class PersianOccasion(
    val title: String,
    val isHoliday: Boolean = false,
    val isReligious: Boolean = false,
    val isNational: Boolean = false
)

object PersianOccasionsHelper {

    // Key: "Month-Day", e.g. "1-1" for 1 Farvardin
    private val SOLAR_OCCASIONS = mapOf(
        // فروردین
        "1-1" to listOf(PersianOccasion("جشن نوروز / آغاز سال نو هجری شمسی", isHoliday = true, isNational = true)),
        "1-2" to listOf(PersianOccasion("عید نوروز", isHoliday = true, isNational = true)),
        "1-3" to listOf(PersianOccasion("عید نوروز", isHoliday = true, isNational = true)),
        "1-4" to listOf(PersianOccasion("عید نوروز", isHoliday = true, isNational = true)),
        "1-6" to listOf(PersianOccasion("روز امید / زادروز زرتشت پیامبر", isHoliday = false, isNational = true)),
        "1-12" to listOf(PersianOccasion("روز جمهوری اسلامی ایران", isHoliday = true, isNational = true)),
        "1-13" to listOf(PersianOccasion("روز طبیعت (سیزده‌به‌در)", isHoliday = true, isNational = true)),
        "1-18" to listOf(PersianOccasion("روز سلامت و روز جهانی بهداشت", isHoliday = false)),
        "1-25" to listOf(PersianOccasion("روز بزرگداشت عطار نیشابوری", isHoliday = false, isNational = true)),
        "1-29" to listOf(PersianOccasion("روز ارتش جمهوری اسلامی ایران", isHoliday = false)),

        // اردیبهشت
        "2-1" to listOf(PersianOccasion("روز بزرگداشت سعدی شیرازی", isHoliday = false, isNational = true)),
        "2-2" to listOf(PersianOccasion("جشن گیاه‌آوری / روز زمین پاک", isHoliday = false)),
        "2-3" to listOf(PersianOccasion("روز بزرگداشت شیخ بهایی / روز معماری", isHoliday = false)),
        "2-9" to listOf(PersianOccasion("روز شوراها / روز روان‌شناس و مشاور", isHoliday = false)),
        "2-10" to listOf(PersianOccasion("روز ملی خلیج فارس", isHoliday = false, isNational = true)),
        "2-11" to listOf(PersianOccasion("روز جهانی کار و کارگر", isHoliday = false)),
        "2-12" to listOf(PersianOccasion("روز معلم / شهادت استاد مرتضی مطهری", isHoliday = false)),
        "2-15" to listOf(PersianOccasion("جشن بهاربد / روز شیراز / روز جهانی ماما", isHoliday = false)),
        "2-24" to listOf(PersianOccasion("لغو امتیاز تنباکو به فتوای آیت‌الله میرزای شیرازی", isHoliday = false)),
        "2-25" to listOf(PersianOccasion("روز بزرگداشت حکیم ابوالقاسم فردوسی و پاسداشت زبان فارسی", isHoliday = false, isNational = true)),
        "2-28" to listOf(PersianOccasion("روز بزرگداشت حکیم عمر خیام نیشابوری", isHoliday = false, isNational = true)),

        // خرداد
        "3-1" to listOf(PersianOccasion("روز بزرگداشت ملاصدرا / روز بهره‌وری", isHoliday = false)),
        "3-3" to listOf(PersianOccasion("روز آزادسازی خرمشهر در عملیات بیت‌المقدس / روز مقاومت و ایثار", isHoliday = false, isNational = true)),
        "3-14" to listOf(PersianOccasion("رحلت حضرت امام خمینی (ره)", isHoliday = true, isNational = true)),
        "3-15" to listOf(PersianOccasion("قیام خونین ۱۵ خرداد", isHoliday = true, isNational = true)),
        "3-20" to listOf(PersianOccasion("روز ملی فرش دستباف / روز جهانی صنایع دستی", isHoliday = false)),
        "3-27" to listOf(PersianOccasion("روز جهاد کشاورزی / روز جهانی بیابان‌زدایی", isHoliday = false)),

        // تیر
        "4-1" to listOf(PersianOccasion("جشن آب‌پاشونک (آغاز تابستان) / روز اصناف", isHoliday = false, isNational = true)),
        "4-7" to listOf(PersianOccasion("شهادت آیت‌الله دکتر بهشتی و ۷۲ تن از یاران / روز قوه قضائیه", isHoliday = false)),
        "4-8" to listOf(PersianOccasion("روز مبارزه با سلاح‌های شیمیایی و میکروبی", isHoliday = false)),
        "4-10" to listOf(PersianOccasion("روز صنعت و معدن", isHoliday = false)),
        "4-13" to listOf(PersianOccasion("جشن تیرگان / روز ملی دماوند", isHoliday = false, isNational = true)),
        "4-14" to listOf(PersianOccasion("روز قلم", isHoliday = false)),
        "4-25" to listOf(PersianOccasion("روز بهزیستی و تامین اجتماعی", isHoliday = false)),

        // مرداد
        "5-6" to listOf(PersianOccasion("روز ترویج آموزش‌های فنی و حرفه‌ای", isHoliday = false)),
        "5-8" to listOf(PersianOccasion("روز بزرگداشت شیخ شهاب‌الدین سهروردی (شیخ اشراق)", isHoliday = false)),
        "5-14" to listOf(PersianOccasion("صدور فرمان مشروطیت", isHoliday = false, isNational = true)),
        "5-17" to listOf(PersianOccasion("روز خبرنگار", isHoliday = false)),
        "5-26" to listOf(PersianOccasion("آغاز بازگشت آزادگان سرافراز به میهن اسلامی", isHoliday = false, isNational = true)),
        "5-28" to listOf(PersianOccasion("کودتای ۲۸ مرداد علیه دولت دکتر مصدق / سالروز آتش‌سوزی سینما رکس آبادان", isHoliday = false)),

        // شهریور
        "6-1" to listOf(PersianOccasion("روز بزرگداشت ابوعلی سینا / روز پزشک", isHoliday = false, isNational = true)),
        "6-2" to listOf(PersianOccasion("آغاز هفته دولت", isHoliday = false)),
        "6-4" to listOf(PersianOccasion("جشن شهریورگان / روز کارمند", isHoliday = false)),
        "6-5" to listOf(PersianOccasion("روز بزرگداشت محمد بن زکریای رازی / روز داروساز", isHoliday = false, isNational = true)),
        "6-8" to listOf(PersianOccasion("روز مبارزه با تروریسم (انفجار دفتر نخست‌وزیری)", isHoliday = false)),
        "6-13" to listOf(PersianOccasion("روز بزرگداشت ابوریحان بیرونی / روز علوم پایه", isHoliday = false)),
        "6-21" to listOf(PersianOccasion("روز سینما", isHoliday = false)),
        "6-27" to listOf(PersianOccasion("روز شعر و ادب فارسی / روز بزرگداشت استاد شهریار", isHoliday = false, isNational = true)),
        "6-31" to listOf(PersianOccasion("آغاز هفته دفاع مقدس", isHoliday = false, isNational = true)),

        // مهر
        "7-1" to listOf(PersianOccasion("آغاز سال تحصیلی جدید / روز بازگشایی مدارس", isHoliday = false)),
        "7-7" to listOf(PersianOccasion("روز آتش‌نشانی و ایمنی / بزرگداشت شمس تبریزی", isHoliday = false)),
        "7-8" to listOf(PersianOccasion("روز بزرگداشت مولوی (جلال‌الدین محمد بلخی)", isHoliday = false, isNational = true)),
        "7-10" to listOf(PersianOccasion("جشن مهرگان", isHoliday = false, isNational = true)),
        "7-13" to listOf(PersianOccasion("روز نیروی انتظامی / روز جهانی معلم", isHoliday = false)),
        "7-14" to listOf(PersianOccasion("روز دامپزشکی", isHoliday = false)),
        "7-20" to listOf(PersianOccasion("روز بزرگداشت حافظ شیرازی", isHoliday = false, isNational = true)),
        "7-26" to listOf(PersianOccasion("روز تربیت‌بدنی و ورزش", isHoliday = false)),

        // آبان
        "8-1" to listOf(PersianOccasion("روز بزرگداشت ابوالفضل بیهقی / روز آمار و برنامه‌ریزی", isHoliday = false)),
        "8-7" to listOf(PersianOccasion("روز بزرگداشت کوروش بزرگ", isHoliday = false, isNational = true)),
        "8-8" to listOf(PersianOccasion("شهادت حسین فهمیده / روز نوجوان و بسیج دانش‌آموزی", isHoliday = false)),
        "8-10" to listOf(PersianOccasion("جشن آبانگان", isHoliday = false, isNational = true)),
        "8-13" to listOf(PersianOccasion("روز دانش‌آموز / تسخیر لانه جاسوسی / روز ملی مبارزه با استکبار", isHoliday = false)),
        "8-24" to listOf(PersianOccasion("روز کتاب، کتابخوانی و کتابدار / بزرگداشت علامه طباطبایی", isHoliday = false)),

        // آذر
        "9-5" to listOf(PersianOccasion("روز بسیج مستضعفان", isHoliday = false)),
        "9-9" to listOf(PersianOccasion("جشن آذرگان", isHoliday = false, isNational = true)),
        "9-10" to listOf(PersianOccasion("روز مجلس (شهادت آیت‌الله سید حسن مدرس)", isHoliday = false)),
        "9-16" to listOf(PersianOccasion("روز دانشجو", isHoliday = false, isNational = true)),
        "9-25" to listOf(PersianOccasion("روز پژوهش", isHoliday = false)),
        "9-30" to listOf(PersianOccasion("شب یلدا (طولانی‌ترین شب سال / جشن شب چله)", isHoliday = false, isNational = true)),

        // دی
        "10-1" to listOf(PersianOccasion("جشن خرم‌روز (نخستین جشن دی‌گان)", isHoliday = false, isNational = true)),
        "10-5" to listOf(PersianOccasion("سالروز زلزله بم / روز ایمنی در برابر زلزله و بلایای طبیعی", isHoliday = false)),
        "10-13" to listOf(PersianOccasion("شهادت سردار سپهبد قاسم سلیمانی / روز جهانی مقاومت", isHoliday = false)),
        "10-19" to listOf(PersianOccasion("قیام خونین مردم قم در سال ۱۳۵۶", isHoliday = false)),
        "10-29" to listOf(PersianOccasion("روز ملی هوای پاک / روز غزه", isHoliday = false)),

        // بهمن
        "11-1" to listOf(PersianOccasion("زادروز حکیم ابوالقاسم فردوسی", isHoliday = false, isNational = true)),
        "11-12" to listOf(PersianOccasion("بازگشت امام خمینی (ره) به میهن / آغاز دهه فجر انقلاب اسلامی", isHoliday = false)),
        "11-19" to listOf(PersianOccasion("روز نیروی هوایی (بیعت همافران با امام)", isHoliday = false)),
        "11-22" to listOf(PersianOccasion("پیروزی شکوهمند انقلاب اسلامی ایران", isHoliday = true, isNational = true)),
        "11-29" to listOf(PersianOccasion("جشن سپندارمذگان (روز عشق ایرانی و پاسداشت بانوان) / قیام مردم تبریز", isHoliday = false, isNational = true)),

        // اسفند
        "12-5" to listOf(PersianOccasion("روز بزرگداشت خواجه نصیرالدین طوسی / روز مهندس", isHoliday = false, isNational = true)),
        "12-14" to listOf(PersianOccasion("روز احسان و نیکوکاری (تاسیس کمیته امداد)", isHoliday = false)),
        "12-15" to listOf(PersianOccasion("روز درختکاری / آغاز هفته منابع طبیعی", isHoliday = false)),
        "12-25" to listOf(PersianOccasion("پایان سرایش شاهنامه فردوسی / بزرگداشت پروین اعتصامی", isHoliday = false, isNational = true)),
        "12-29" to listOf(PersianOccasion("روز ملی شدن صنعت نفت ایران", isHoliday = true, isNational = true))
    )

    // Hijri Islamic Occasions (Dynamic mapping for current/upcoming calendar year 1404-1406)
    private val LUNAR_HOLIDAYS_1405 = mapOf(
        "1-1" to listOf(PersianOccasion("عید سعید فطر", isHoliday = true, isReligious = true)),
        "1-2" to listOf(PersianOccasion("تعطیلی به مناسبت عید سعید فطر", isHoliday = true, isReligious = true)),
        "1-25" to listOf(PersianOccasion("شهادت حضرت امام جعفر صادق (ع)", isHoliday = true, isReligious = true)),
        "3-6" to listOf(PersianOccasion("عید سعید قربان", isHoliday = true, isReligious = true)),
        "3-14" to listOf(PersianOccasion("عید سعید غدیر خم", isHoliday = true, isReligious = true)),
        "4-4" to listOf(PersianOccasion("تاسوعای حسینی (۹ محرم)", isHoliday = true, isReligious = true)),
        "4-5" to listOf(PersianOccasion("عاشورای حسینی (۱۰ محرم)", isHoliday = true, isReligious = true)),
        "5-14" to listOf(PersianOccasion("اربعین حسینی (۲۰ صفر)", isHoliday = true, isReligious = true)),
        "5-22" to listOf(PersianOccasion("رحلت حضرت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)", isHoliday = true, isReligious = true)),
        "5-24" to listOf(PersianOccasion("شهادت حضرت امام رضا (ع)", isHoliday = true, isReligious = true)),
        "6-1" to listOf(PersianOccasion("شهادت حضرت امام حسن عسکری (ع) و آغاز امامت حضرت مهدی (عج)", isHoliday = true, isReligious = true)),
        "6-10" to listOf(PersianOccasion("میلاد حضرت رسول اکرم (ص) و امام جعفر صادق (ع) / هفته وحدت", isHoliday = true, isReligious = true)),
        "8-24" to listOf(PersianOccasion("شهادت حضرت فاطمه زهرا (س)", isHoliday = true, isReligious = true)),
        "10-3" to listOf(PersianOccasion("ولادت حضرت امام علی (ع) و روز پدر", isHoliday = true, isReligious = true)),
        "10-17" to listOf(PersianOccasion("مبعث حضرت رسول اکرم (ص)", isHoliday = true, isReligious = true)),
        "11-5" to listOf(PersianOccasion("ولادت با سعادت حضرت قائم عجل‌الله تعالی فرجه (نیمه شعبان)", isHoliday = true, isReligious = true)),
        "12-19" to listOf(PersianOccasion("شهادت حضرت علی (ع) و شب‌های قدر", isHoliday = true, isReligious = true))
    )

    fun getOccasionsForDate(month: Int, day: Int): List<PersianOccasion> {
        val key = "$month-$day"
        val solar = SOLAR_OCCASIONS[key] ?: emptyList()
        val lunar = LUNAR_HOLIDAYS_1405[key] ?: emptyList()
        return solar + lunar
    }

    fun isHoliday(month: Int, day: Int): Boolean {
        val occasions = getOccasionsForDate(month, day)
        return occasions.any { it.isHoliday }
    }
}
