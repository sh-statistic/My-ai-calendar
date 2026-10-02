package com.example.calendar

data class PersianOccasion(
    val title: String,
    val isHoliday: Boolean = false,
    val isReligious: Boolean = false,
    val isNational: Boolean = false,
    val customMonth: Int = -1,
    val customDay: Int = -1
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
    fun getOccasionsForDate(month: Int, day: Int, year: Int = 1405): List<PersianOccasion> {
        ensureYearLoaded(year)
        val key = "$month-$day"
        val custom = customOccasions.filter { it.customMonth == month && it.customDay == day }
        val solar = SOLAR_OCCASIONS[key] ?: emptyList()
        val mergedOccasions = mutableListOf<PersianOccasion>()
        mergedOccasions.addAll(solar)

        val officialMap = officialDataCache[year]
        if (officialMap != null && officialMap.containsKey(key)) {
            val officialDay = officialMap[key]!!
            // Add JSON events that are not already covered by Solar (simple deduplication)
            val jsonOccs = officialDay.occasions.map { PersianOccasion(title = it, isHoliday = officialDay.isHoliday, isNational = true) }
            for (jOcc in jsonOccs) {
                // If the solar list doesn't have a very similar title, add it
                // We assume religious events from JSON will have different keywords or 'ه‍.ق'
                val isDuplicate = solar.any { sOcc -> 
                    jOcc.title.contains(sOcc.title) || sOcc.title.contains(jOcc.title) ||
                    (jOcc.title.contains("نوروز") && sOcc.title.contains("نوروز"))
                }
                if (!isDuplicate) {
                    mergedOccasions.add(jOcc)
                }
            }
            return mergedOccasions + custom
        }

        // If JSON doesn't exist for this year, fallback to mathematical lunar calculation
        val lunarMap = HijriCalendarUtils.getOccasionsForYear(year)
        val lunar = lunarMap[key] ?: emptyList()
        return mergedOccasions + lunar + custom
    }

    /**
     * آیا این روز تعطیل رسمی است؟
     * @param year سال شمسی (برای محاسبه تعطیلات قمری)
     */
    fun isHoliday(month: Int, day: Int, year: Int = 1405): Boolean {
        ensureYearLoaded(year)
        val key = "$month-$day"
        
        var isHoliday = false
        
        // 1. Check Solar Fixed Holidays
        if (SOLAR_OCCASIONS[key]?.any { it.isHoliday } == true) {
            isHoliday = true
        }

        // 2. Check JSON Official Holidays
        val officialMap = officialDataCache[year]
        if (officialMap != null && officialMap.containsKey(key)) {
            if (officialMap[key]!!.isHoliday) {
                isHoliday = true
            }
        } else {
            // 3. Fallback to Lunar Math Holidays if JSON is missing
            val lunarMap = HijriCalendarUtils.getOccasionsForYear(year)
            if (lunarMap[key]?.any { it.isHoliday } == true) {
                isHoliday = true
            }
        }

        // 4. Check Custom Holidays
        if (customOccasions.any { it.customMonth == month && it.customDay == day && it.isHoliday }) {
            isHoliday = true
        }

        return isHoliday
    }

    private var customOccasions: List<PersianOccasion> = emptyList()

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
                        customDay = obj.optInt("day", -1)
                    )
                )
            }
            customOccasions = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addCustomOccasion(context: android.content.Context, title: String, month: Int, day: Int, isHoliday: Boolean) {
        val list = customOccasions.toMutableList()
        val newOccasion = PersianOccasion(title, isHoliday, isNational = true, customMonth = month, customDay = day)
        list.add(newOccasion)
        customOccasions = list

        saveCustomOccasionsToPrefs(context)
    }

    fun removeCustomOccasion(context: android.content.Context, occasion: PersianOccasion) {
        val list = customOccasions.toMutableList()
        list.remove(occasion)
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
            arr.put(obj)
        }
        context.getSharedPreferences("CustomOccasions", android.content.Context.MODE_PRIVATE)
            .edit().putString("data", arr.toString()).apply()
    }
}
