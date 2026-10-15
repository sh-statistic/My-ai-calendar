package com.example.calendar

data class PersianOccasion(
    val title: String,
    val isHoliday: Boolean = false,
    val isReligious: Boolean = false,
    val isNational: Boolean = false,
    val customMonth: Int = -1,
    val customDay: Int = -1,
    val originalTitle: String = ""
)

data class OfficialDayData(
    val hYear: Int,
    val hMonth: Int,
    val hDay: Int,
    val isHoliday: Boolean,
    val occasions: List<String>
)

object PersianOccasionsHelper {

    private var appContext: android.content.Context? = null
    private val officialDataCache = mutableMapOf<Int, Map<String, OfficialDayData>>()

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
        loadCustomOccasions(context)
    }

    fun loadOfficialDataForYear(context: android.content.Context, year: Int) {
        appContext = context.applicationContext
        if (officialDataCache.containsKey(year)) return
        try {
            val fileName = "occasions_$year.json"
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val jsonObject = org.json.JSONObject(jsonString)
            val yearMap = mutableMapOf<String, OfficialDayData>()
            val months = jsonObject.keys()
            while (months.hasNext()) {
                val m = months.next()
                val daysObj = jsonObject.getJSONObject(m)
                val days = daysObj.keys()
                while (days.hasNext()) {
                    val d = days.next()
                    val dayObj = daysObj.getJSONObject(d)
                    val occArr = dayObj.getJSONArray("occasions")
                    val occList = mutableListOf<String>()
                    for (i in 0 until occArr.length()) occList.add(occArr.getString(i))
                    yearMap["$m-$d"] = OfficialDayData(
                        hYear = dayObj.getInt("hYear"),
                        hMonth = dayObj.getInt("hMonth"),
                        hDay = dayObj.getInt("hDay"),
                        isHoliday = dayObj.getBoolean("isHoliday"),
                        occasions = occList
                    )
                }
            }
            officialDataCache[year] = yearMap
        } catch (e: Exception) {
            // File not found or parse error - cache empty map so we don't try to reload repeatedly
            officialDataCache[year] = emptyMap()
        }
    }

    private fun ensureYearLoaded(year: Int) {
        if (!officialDataCache.containsKey(year)) {
            val ctx = appContext
            if (ctx != null) {
                loadOfficialDataForYear(ctx, year)
            }
        }
    }

    fun getHijriDate(year: Int, month: Int, day: Int): HijriCalendarUtils.SimpleDate? {
        ensureYearLoaded(year)
        val officialMap = officialDataCache[year] ?: return null
        val officialDay = officialMap["$month-$day"] ?: return null
        return HijriCalendarUtils.SimpleDate(officialDay.hYear, officialDay.hMonth, officialDay.hDay)
    }

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
        "1-18" to listOf(PersianOccasion("روز سلامتی / روز جهانی بهداشت", isHoliday = false)),
        "1-19" to listOf(PersianOccasion("جشن فروردینگان", isHoliday = false, isNational = true)),
        "1-20" to listOf(PersianOccasion("روز ملی فناوری هسته‌ای / روز هنر انقلاب اسلامی", isHoliday = false, isNational = true)),
        "1-25" to listOf(PersianOccasion("روز بزرگداشت عطار نیشابوری", isHoliday = false, isNational = true)),
        "1-29" to listOf(PersianOccasion("روز ارتش جمهوری اسلامی ایران و نیروی زمینی", isHoliday = false)),

        // اردیبهشت
        "2-1" to listOf(PersianOccasion("روز بزرگداشت سعدی شیرازی", isHoliday = false, isNational = true)),
        "2-2" to listOf(PersianOccasion("جشن گیاه‌آوری / روز زمین پاک / تأسیس سپاه پاسداران", isHoliday = false)),
        "2-3" to listOf(PersianOccasion("روز بزرگداشت شیخ بهایی / روز معماری", isHoliday = false)),
        "2-5" to listOf(PersianOccasion("شکست حمله نظامی آمریکا به ایران در طبس", isHoliday = false)),
        "2-9" to listOf(PersianOccasion("روز شوراها / روز روان‌شناس و مشاور", isHoliday = false)),
        "2-10" to listOf(PersianOccasion("روز ملی خلیج فارس", isHoliday = false, isNational = true)),
        "2-11" to listOf(PersianOccasion("روز جهانی کار و کارگر", isHoliday = false)),
        "2-12" to listOf(PersianOccasion("روز معلم / شهادت استاد مرتضی مطهری", isHoliday = false)),
        "2-15" to listOf(PersianOccasion("جشن میانه بهار / جشن بهاربد / روز شیراز / روز جهانی ماما", isHoliday = false)),
        "2-24" to listOf(PersianOccasion("لغو امتیاز تنباکو به فتوای آیت‌الله میرزای شیرازی", isHoliday = false)),
        "2-25" to listOf(PersianOccasion("روز بزرگداشت حکیم ابوالقاسم فردوسی و پاسداشت زبان فارسی", isHoliday = false, isNational = true)),
        "2-27" to listOf(PersianOccasion("روز ارتباطات و روابط عمومی", isHoliday = false)),
        "2-28" to listOf(PersianOccasion("روز بزرگداشت حکیم عمر خیام نیشابوری", isHoliday = false, isNational = true)),

        // خرداد
        "3-1" to listOf(PersianOccasion("روز بزرگداشت ملاصدرا / روز بهره‌وری", isHoliday = false)),
        "3-3" to listOf(PersianOccasion("روز آزادسازی خرمشهر / روز مقاومت، ایثار و پیروزی", isHoliday = false, isNational = true)),
        "3-4" to listOf(PersianOccasion("روز دزفول، روز مقاومت و پایداری", isHoliday = false)),
        "3-6" to listOf(PersianOccasion("خرداد روز، جشن خردادگان", isHoliday = false, isNational = true)),
        "3-14" to listOf(PersianOccasion("رحلت حضرت امام خمینی (ره)", isHoliday = true, isNational = true)),
        "3-15" to listOf(PersianOccasion("قیام خونین ۱۵ خرداد", isHoliday = true, isNational = true)),
        "3-20" to listOf(PersianOccasion("روز ملی فرش دستباف / روز جهانی صنایع دستی", isHoliday = false)),
        "3-27" to listOf(PersianOccasion("روز جهاد کشاورزی / روز جهانی بیابان‌زدایی", isHoliday = false)),
        "3-31" to listOf(PersianOccasion("شهادت دکتر مصطفی چمران / روز بسیج اساتید", isHoliday = false)),

        // تیر
        "4-1" to listOf(PersianOccasion("جشن آب‌پاشونک / آغاز تابستان / روز اصناف", isHoliday = false, isNational = true)),
        "4-6" to listOf(PersianOccasion("روز جهانی مبارزه با مواد مخدر", isHoliday = false)),
        "4-7" to listOf(PersianOccasion("شهادت آیت‌الله دکتر بهشتی و ۷۲ تن از یاران / روز قوه قضائیه", isHoliday = false)),
        "4-8" to listOf(PersianOccasion("روز مبارزه با سلاح‌های شیمیایی و میکروبی", isHoliday = false)),
        "4-10" to listOf(PersianOccasion("روز صنعت و معدن", isHoliday = false)),
        "4-12" to listOf(PersianOccasion("حمله ناوگان آمریکایی به هواپیمای مسافربری ایران", isHoliday = false)),
        "4-13" to listOf(PersianOccasion("تیر روز، جشن تیرگان / روز ملی دماوند", isHoliday = false, isNational = true)),
        "4-14" to listOf(PersianOccasion("روز قلم", isHoliday = false)),
        "4-25" to listOf(PersianOccasion("روز بهزیستی و تامین اجتماعی", isHoliday = false)),

        // مرداد
        "5-5" to listOf(PersianOccasion("سالروز عملیات مرصاد", isHoliday = false)),
        "5-6" to listOf(PersianOccasion("روز ترویج آموزش‌های فنی و حرفه‌ای", isHoliday = false)),
        "5-7" to listOf(PersianOccasion("امرداد روز، جشن امردادگان", isHoliday = false, isNational = true)),
        "5-8" to listOf(PersianOccasion("روز بزرگداشت شیخ شهاب‌الدین سهروردی (شیخ اشراق)", isHoliday = false)),
        "5-14" to listOf(PersianOccasion("صدور فرمان مشروطیت", isHoliday = false, isNational = true)),
        "5-17" to listOf(PersianOccasion("روز خبرنگار", isHoliday = false)),
        "5-26" to listOf(PersianOccasion("آغاز بازگشت آزادگان سرافراز به میهن اسلامی", isHoliday = false, isNational = true)),
        "5-28" to listOf(PersianOccasion("کودتای ۲۸ مرداد علیه دولت دکتر مصدق", isHoliday = false)),

        // شهریور
        "6-1" to listOf(PersianOccasion("روز بزرگداشت ابوعلی سینا / روز پزشک", isHoliday = false, isNational = true)),
        "6-2" to listOf(PersianOccasion("آغاز هفته دولت", isHoliday = false)),
        "6-4" to listOf(PersianOccasion("شهریور روز، جشن شهریورگان / روز کارمند", isHoliday = false, isNational = true)),
        "6-5" to listOf(PersianOccasion("روز بزرگداشت محمد بن زکریای رازی / روز داروساز", isHoliday = false, isNational = true)),
        "6-8" to listOf(PersianOccasion("روز مبارزه با تروریسم (انفجار دفتر نخست‌وزیری)", isHoliday = false)),
        "6-11" to listOf(PersianOccasion("روز صنعت چاپ", isHoliday = false)),
        "6-13" to listOf(PersianOccasion("روز بزرگداشت ابوریحان بیرونی / روز تعاون", isHoliday = false, isNational = true)),
        "6-21" to listOf(PersianOccasion("روز سینما", isHoliday = false)),
        "6-27" to listOf(PersianOccasion("روز شعر و ادب فارسی / روز بزرگداشت استاد شهریار", isHoliday = false, isNational = true)),
        "6-31" to listOf(PersianOccasion("آغاز هفته دفاع مقدس", isHoliday = false, isNational = true)),

        // مهر
        "7-1" to listOf(PersianOccasion("آغاز سال تحصیلی جدید / روز بازگشایی مدارس", isHoliday = false)),
        "7-7" to listOf(PersianOccasion("روز آتش‌نشانی و ایمنی / بزرگداشت شمس تبریزی / بزرگداشت فرماندهان شهید دفاع مقدس", isHoliday = false)),
        "7-8" to listOf(PersianOccasion("روز بزرگداشت مولوی (جلال‌الدین محمد بلخی)", isHoliday = false, isNational = true)),
        "7-9" to listOf(PersianOccasion("روز همبستگی و همدردی با کودکان و نوجوانان فلسطینی", isHoliday = false)),
        "7-10" to listOf(PersianOccasion("مهر روز، جشن مهرگان", isHoliday = false, isNational = true)),
        "7-13" to listOf(PersianOccasion("روز نیروی انتظامی / روز جهانی معلم", isHoliday = false)),
        "7-14" to listOf(PersianOccasion("روز دامپزشکی", isHoliday = false)),
        "7-20" to listOf(PersianOccasion("روز بزرگداشت حافظ شیرازی", isHoliday = false, isNational = true)),
        "7-23" to listOf(PersianOccasion("شهادت پنجمین شهید محراب آیت‌الله اشرفی اصفهانی / روز جهانی عصای سفید", isHoliday = false)),
        "7-26" to listOf(PersianOccasion("روز تربیت‌بدنی و ورزش", isHoliday = false)),

        // آبان
        "8-1" to listOf(PersianOccasion("روز بزرگداشت ابوالفضل بیهقی / روز آمار و برنامه‌ریزی", isHoliday = false)),
        "8-4" to listOf(PersianOccasion("اعتراض امام خمینی (ره) به کاپیتولاسیون", isHoliday = false)),
        "8-7" to listOf(PersianOccasion("روز بزرگداشت کوروش بزرگ", isHoliday = false, isNational = true)),
        "8-8" to listOf(PersianOccasion("شهادت محمدحسین فهمیده / روز نوجوان و بسیج دانش‌آموزی", isHoliday = false)),
        "8-10" to listOf(PersianOccasion("آبان روز، جشن آبانگان", isHoliday = false, isNational = true)),
        "8-13" to listOf(PersianOccasion("روز دانش‌آموز / تسخیر لانه جاسوسی / روز ملی مبارزه با استکبار جهانی", isHoliday = false)),
        "8-14" to listOf(PersianOccasion("روز فرهنگ عمومی", isHoliday = false)),
        "8-24" to listOf(PersianOccasion("روز کتاب و کتابخوانی / بزرگداشت علامه سید محمدحسین طباطبایی", isHoliday = false)),

        // آذر
        "9-5" to listOf(PersianOccasion("روز بسیج مستضعفان", isHoliday = false)),
        "9-7" to listOf(PersianOccasion("روز نیروی دریایی", isHoliday = false)),
        "9-9" to listOf(PersianOccasion("آذر روز، جشن آذرگان / روز بزرگداشت شیخ مفید", isHoliday = false, isNational = true)),
        "9-10" to listOf(PersianOccasion("روز مجلس (شهادت آیت‌الله سید حسن مدرس)", isHoliday = false)),
        "9-12" to listOf(PersianOccasion("تصویب قانون اساسی جمهوری اسلامی ایران / روز جهانی معلولان", isHoliday = false)),
        "9-16" to listOf(PersianOccasion("روز دانشجو", isHoliday = false, isNational = true)),
        "9-25" to listOf(PersianOccasion("روز پژوهش", isHoliday = false)),
        "9-26" to listOf(PersianOccasion("روز حمل و نقل", isHoliday = false)),
        "9-27" to listOf(PersianOccasion("شهادت آیت‌الله دکتر محمد مفتح / روز وحدت حوزه و دانشگاه", isHoliday = false)),
        "9-30" to listOf(PersianOccasion("شب یلدا (طولانی‌ترین شب سال / جشن شب چله)", isHoliday = false, isNational = true)),

        // دی
        "10-1" to listOf(PersianOccasion("روز میلاد خورشید؛ جشن خرم‌روز (نخستین جشن دی‌گان)", isHoliday = false, isNational = true)),
        "10-5" to listOf(PersianOccasion("سالروز زلزله بم / روز ایمنی در برابر زلزله و بلایای طبیعی", isHoliday = false)),
        "10-8" to listOf(PersianOccasion("دی به آذر روز، دومین جشن دیگان", isHoliday = false, isNational = true)),
        "10-9" to listOf(PersianOccasion("روز بصیرت و میثاق امت با ولایت", isHoliday = false)),
        "10-13" to listOf(PersianOccasion("شهادت سردار سپهبد قاسم سلیمانی / روز جهانی مقاومت", isHoliday = false)),
        "10-15" to listOf(PersianOccasion("دی به مهر روز، سومین جشن دیگان", isHoliday = false, isNational = true)),
        "10-19" to listOf(PersianOccasion("قیام خونین مردم قم (۱۳۵۶)", isHoliday = false)),
        "10-20" to listOf(PersianOccasion("شهادت میرزا تقی خان امیرکبیر", isHoliday = false)),
        "10-23" to listOf(PersianOccasion("دی به دین روز، چهارمین جشن دیگان", isHoliday = false, isNational = true)),
        "10-29" to listOf(PersianOccasion("روز ملی هوای پاک / روز غزه", isHoliday = false)),

        // بهمن
        "11-1" to listOf(PersianOccasion("زادروز حکیم ابوالقاسم فردوسی", isHoliday = false, isNational = true)),
        "11-2" to listOf(PersianOccasion("بهمن روز، جشن بهمنگان", isHoliday = false, isNational = true)),
        "11-10" to listOf(PersianOccasion("جشن سده", isHoliday = false, isNational = true)),
        "11-12" to listOf(PersianOccasion("بازگشت امام خمینی (ره) به میهن / آغاز دهه فجر انقلاب اسلامی", isHoliday = false)),
        "11-19" to listOf(PersianOccasion("روز نیروی هوایی (بیعت همافران با امام)", isHoliday = false)),
        "11-22" to listOf(PersianOccasion("پیروزی شکوهمند انقلاب اسلامی ایران", isHoliday = true, isNational = true)),
        "11-29" to listOf(PersianOccasion("جشن سپندارمذگان (روز عشق ایرانی) / قیام مردم تبریز (۱۳۵۶)", isHoliday = false, isNational = true)),

        // اسفند
        "12-5" to listOf(PersianOccasion("روز بزرگداشت خواجه نصیرالدین طوسی / روز مهندس", isHoliday = false, isNational = true)),
        "12-14" to listOf(PersianOccasion("روز احسان و نیکوکاری (تاسیس کمیته امداد امام خمینی)", isHoliday = false)),
        "12-15" to listOf(PersianOccasion("روز درختکاری / آغاز هفته منابع طبیعی", isHoliday = false)),
        "12-22" to listOf(PersianOccasion("روز بزرگداشت شهدا (تاسیس بنیاد شهید)", isHoliday = false)),
        "12-25" to listOf(PersianOccasion("پایان سرایش شاهنامه فردوسی / بزرگداشت پروین اعتصامی / شهادت شهردار باکری", isHoliday = false, isNational = true)),
        "12-29" to listOf(PersianOccasion("روز ملی شدن صنعت نفت ایران", isHoliday = true, isNational = true))
    )

    // مناسبت‌های مذهبی (قمری) دیگر به صورت ثابت ذخیره نمی‌شوند.
    // به جای آن، HijriCalendarUtils تاریخ‌ها را بر اساس سال شمسی به صورت خودکار محاسبه می‌کند.

    /**
     * دریافت تمام مناسبت‌های (شمسی + قمری + سفارشی) برای یک تاریخ مشخص.
     * مناسبت‌های قمری به صورت خودکار بر اساس سال شمسی محاسبه می‌شوند.
     *
     * @param month ماه شمسی
     * @param day روز شمسی
     * @param year سال شمسی (برای محاسبه مناسبت‌های قمری)
     */
    private val COMMON_STOP_WORDS = setOf(
        "روز", "جشن", "بزرگداشت", "ملی", "جهانی", "سالروز", "شهادت", "ولادت", "یادروز", "تاسیس", "آغاز", "پایان", "ایران"
    )

    private fun normalizeTitle(text: String): String {
        return text.trim()
            .replace("\u200C", "") // حذف نیم‌فاصله‌ها
            .replace("‌", "")
            .replace("ي", "ی")
            .replace("ك", "ک")
            .replace("  ", " ")
    }

    /**
     * بررسی هوشمند اینکه آیا دو عنوان مناسبت به یک رویداد اشاره دارند یا خیر.
     * اگر یکی باشند true برمی‌گرداند (هیچ تکراری ایجاد نمی‌شود).
     * اگر رویداد متفاوتی باشد false برمی‌گرداند (در آپدیت‌های آینده به عنوان مناسبت جدید اضافه می‌شود).
     */
    fun isSameOccasion(t1: String, t2: String): Boolean {
        val s1 = normalizeTitle(t1)
        val s2 = normalizeTitle(t2)
        if (s1.isEmpty() || s2.isEmpty()) return false
        if (s1 == s2) return true

        // استخراج کلمات کلیدی اختصاصی (بدون کلمات عمومی تقویم مثل "روز" و "بزرگداشت")
        val words1 = s1.split(" ", "/", "،", "-").map { it.trim() }.filter { it.length > 2 && it !in COMMON_STOP_WORDS }
        val words2 = s2.split(" ", "/", "،", "-").map { it.trim() }.filter { it.length > 2 && it !in COMMON_STOP_WORDS }
        val common = words1.intersect(words2.toSet())
        if (common.isNotEmpty()) {
            return true
        }

        // بررسی زیررشته برای عناوینی با طول مشخص
        if (s1.length >= 6 && s2.length >= 6) {
            if (s1.contains(s2) || s2.contains(s1)) return true
        }

        return false
    }

    fun getOccasionsForDate(month: Int, day: Int, year: Int = 1405): List<PersianOccasion> {
        ensureYearLoaded(year)
        val key = "$month-$day"
        val custom = customOccasions.filter { it.customMonth == month && it.customDay == day }

        // بررسی اینکه آیا مناسبت رسمی ورودی، قبلاً با یکی از مناسبت‌های ثبت‌شده/سفارشی یا پنهان‌شده یکی است یا خیر
        fun isCoveredByCustomOrHidden(officialTitle: String): Boolean {
            // ۱. بررسی در عناوین صریحاً پنهان‌شده
            if (hiddenOccasions.any { isSameOccasion(it, officialTitle) }) return true

            // ۲. بررسی در مناسبت‌های ثبت‌شده کاربر برای این روز
            for (c in custom) {
                // اگر کاربر قبلاً این مناسبت را ویرایش کرده یا عنوان سفارشی با این مناسبت یکی است
                if (c.originalTitle.isNotBlank() && isSameOccasion(c.originalTitle, officialTitle)) return true
                if (isSameOccasion(c.title, officialTitle)) return true
            }
            return false
        }

        // ۱. واکشی مناسبت‌های شمسی که با موارد ثبت‌شده کاربر یکی نیستند
        val solar = (SOLAR_OCCASIONS[key] ?: emptyList()).filter { !isCoveredByCustomOrHidden(it.title) }
        val mergedOccasions = mutableListOf<PersianOccasion>()
        mergedOccasions.addAll(solar)

        // ۲. واکشی مناسبت‌های رسمی JSON که با موارد ثبت‌شده کاربر یکی نیستند
        val officialMap = officialDataCache[year]
        if (officialMap != null && officialMap.containsKey(key)) {
            val officialDay = officialMap[key]!!
            val jsonOccs = officialDay.occasions
                .filter { !isCoveredByCustomOrHidden(it) }
                .map { PersianOccasion(title = it, isHoliday = officialDay.isHoliday, isNational = true) }
            for (jOcc in jsonOccs) {
                val isDuplicate = mergedOccasions.any { sOcc -> isSameOccasion(jOcc.title, sOcc.title) }
                if (!isDuplicate) {
                    mergedOccasions.add(jOcc)
                }
            }
        } else {
            // ۳. در صورت عدم وجود فایل JSON، مناسبت‌های قمری واکشی می‌شوند
            val lunarMap = HijriCalendarUtils.getOccasionsForYear(year)
            val lunar = (lunarMap[key] ?: emptyList()).filter { !isCoveredByCustomOrHidden(it.title) }
            for (lOcc in lunar) {
                val isDuplicate = mergedOccasions.any { mOcc -> isSameOccasion(lOcc.title, mOcc.title) }
                if (!isDuplicate) {
                    mergedOccasions.add(lOcc)
                }
            }
        }

        // مناسبت‌های سفارشی اولویت دارند؛ سپس مناسبت‌های رسمی جدید و غیرهمپوشان اضافه می‌شوند
        return (custom + mergedOccasions).distinctBy { normalizeTitle(it.title) }
    }

    /**
     * آیا این روز تعطیل رسمی است؟
     * @param year سال شمسی (برای محاسبه تعطیلات قمری)
     */
    fun isHoliday(month: Int, day: Int, year: Int = 1405): Boolean {
        val occasions = getOccasionsForDate(month, day, year)
        return occasions.any { it.isHoliday }
    }

    private var customOccasions: List<PersianOccasion> = emptyList()
    private var hiddenOccasions: MutableSet<String> = mutableSetOf()

    fun loadCustomOccasions(context: android.content.Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences("CustomOccasions", android.content.Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("data", "[]") ?: "[]"
        try {
            val arr = org.json.JSONArray(jsonStr)
            val list = mutableListOf<PersianOccasion>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PersianOccasion(
                        title = obj.getString("title"),
                        isHoliday = obj.optBoolean("isHoliday", false),
                        isNational = true,
                        customMonth = obj.optInt("month", -1),
                        customDay = obj.optInt("day", -1),
                        originalTitle = obj.optString("originalTitle", "")
                    )
                )
            }
            // پاکسازی رکوردهای تکراری ذخیره‌شده
            customOccasions = list.distinctBy { "${it.title.trim()}_${it.customMonth}_${it.customDay}" }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val hiddenJson = prefs.getString("hidden_data", "[]") ?: "[]"
        try {
            val hArr = org.json.JSONArray(hiddenJson)
            val hSet = mutableSetOf<String>()
            for (i in 0 until hArr.length()) {
                hSet.add(hArr.getString(i))
            }
            hiddenOccasions = hSet
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addCustomOccasion(context: android.content.Context, title: String, month: Int, day: Int, isHoliday: Boolean) {
        val list = customOccasions.toMutableList()
        val newOccasion = PersianOccasion(title.trim(), isHoliday, isNational = true, customMonth = month, customDay = day)
        list.add(newOccasion)
        customOccasions = list.distinctBy { "${it.title.trim()}_${it.customMonth}_${it.customDay}" }

        saveCustomOccasionsToPrefs(context)
    }

    fun updateCustomOccasion(
        context: android.content.Context,
        oldOccasion: PersianOccasion,
        newTitle: String,
        newMonth: Int,
        newDay: Int,
        newIsHoliday: Boolean
    ) {
        val list = customOccasions.toMutableList()
        // پیدا کردن رکورد موجود برای جلوگیری از ایجاد تکراری
        val index = list.indexOfFirst {
            (it.title == oldOccasion.title &&
                (oldOccasion.customMonth == -1 || it.customMonth == oldOccasion.customMonth) &&
                (oldOccasion.customDay == -1 || it.customDay == oldOccasion.customDay)) ||
            (oldOccasion.originalTitle.isNotBlank() && it.originalTitle == oldOccasion.originalTitle) ||
            (it.originalTitle.isNotBlank() && it.originalTitle == oldOccasion.title && it.customMonth == newMonth && it.customDay == newDay) ||
            (it.customMonth == newMonth && it.customDay == newDay && it.title == newTitle.trim())
        }

        // پنهان کردن عنوان قبلی مناسبت رسمی و عناوین همپوشان
        hiddenOccasions.add(oldOccasion.title)
        if (oldOccasion.originalTitle.isNotBlank()) {
            hiddenOccasions.add(oldOccasion.originalTitle)
        }

        val origTitle = if (index != -1 && list[index].originalTitle.isNotBlank()) {
            list[index].originalTitle
        } else if (oldOccasion.originalTitle.isNotBlank()) {
            oldOccasion.originalTitle
        } else {
            oldOccasion.title
        }

        val updatedOccasion = PersianOccasion(
            title = newTitle.trim(),
            isHoliday = newIsHoliday,
            isNational = true,
            customMonth = newMonth,
            customDay = newDay,
            originalTitle = origTitle
        )

        if (index != -1) {
            list[index] = updatedOccasion
        } else {
            list.add(updatedOccasion)
        }

        customOccasions = list.distinctBy { "${it.title.trim()}_${it.customMonth}_${it.customDay}" }
        saveCustomOccasionsToPrefs(context)
    }

    fun removeCustomOccasion(context: android.content.Context, occasion: PersianOccasion) {
        val list = customOccasions.toMutableList()
        list.removeAll {
            (it.title == occasion.title && it.customMonth == occasion.customMonth && it.customDay == occasion.customDay) ||
            (occasion.originalTitle.isNotBlank() && it.originalTitle == occasion.originalTitle) ||
            (it.originalTitle.isNotBlank() && it.originalTitle == occasion.title)
        }
        if (occasion.originalTitle.isNotBlank()) {
            hiddenOccasions.remove(occasion.originalTitle)
        } else if (occasion.customMonth == -1) {
            hiddenOccasions.add(occasion.title)
        }
        customOccasions = list
        saveCustomOccasionsToPrefs(context)
    }
    
    fun getCustomOccasions(): List<PersianOccasion> = customOccasions

    private fun saveCustomOccasionsToPrefs(context: android.content.Context) {
        val arr = org.json.JSONArray()
        for (occ in customOccasions) {
            val obj = org.json.JSONObject()
            obj.put("title", occ.title)
            obj.put("isHoliday", occ.isHoliday)
            obj.put("month", occ.customMonth)
            obj.put("day", occ.customDay)
            if (occ.originalTitle.isNotBlank()) {
                obj.put("originalTitle", occ.originalTitle)
            }
            arr.put(obj)
        }
        val hArr = org.json.JSONArray()
        for (h in hiddenOccasions) {
            hArr.put(h)
        }
        context.getSharedPreferences("CustomOccasions", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("data", arr.toString())
            .putString("hidden_data", hArr.toString())
            .apply()
    }
}
