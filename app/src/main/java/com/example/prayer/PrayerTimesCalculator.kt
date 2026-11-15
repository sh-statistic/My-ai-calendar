package com.example.prayer

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

data class IranianCity(
    val nameFa: String,
    val nameEn: String,
    val latitude: Double,
    val longitude: Double,
    val province: String
)

// ترتیب الفبای فارسی برای مرتب‌سازی نام استان‌ها و شهرها
private const val PERSIAN_ALPHA = "ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی"

internal fun persianNameRank(name: String): List<Int> = name.mapNotNull { raw ->
    val ch = when (raw) {
        'آ' -> 'ا'
        'ي' -> 'ی'
        'ك' -> 'ک'
        '‌', ' ', '-' -> null
        else -> raw
    }
    ch?.let { PERSIAN_ALPHA.indexOf(it).takeIf { i -> i >= 0 } ?: 99 }
}

internal val persianNameComparator = Comparator<String> { a, b ->
    val ra = persianNameRank(a)
    val rb = persianNameRank(b)
    val firstDiff = ra.zip(rb).firstOrNull { it.first != it.second }
    if (firstDiff != null) firstDiff.first - firstDiff.second else ra.size - rb.size
}

/** Normalizes a city search query/name so Arabic ي/ك and ZWNJ spellings match Persian ones. */
internal fun normalizeCitySearchText(text: String): String =
    text.trim()
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace('‌', ' ')
        .replace(Regex("\\s+"), " ")

data class PrayerTimes(
    val fajr: String,        // اذان صبح
    val sunrise: String,     // طلوع آفتاب
    val dhuhr: String,       // اذان ظهر
    val sunset: String,      // غروب آفتاب
    val maghrib: String,     // اذان مغرب
    val midnight: String,    // نیمه‌شب شرعی
    val fajrHour: Double,
    val sunriseHour: Double,
    val dhuhrHour: Double,
    val sunsetHour: Double,
    val maghribHour: Double,
    val midnightHour: Double
)

data class NextPrayerInfo(
    val name: String,
    val time: String,
    val remainingMinutes: Int
)

object PrayerTimesCalculator {

    // Institute of Geophysics, University of Tehran parameters
    private const val FAJR_ANGLE = 17.7
    private const val MAGHRIB_ANGLE = 4.5
    private const val SUNRISE_SUNSET_ANGLE = 0.8333

    val CITIES = listOf(
        IranianCity("تهران", "Tehran", 35.6892, 51.3890, "تهران"),
        IranianCity("مشهد", "Mashhad", 36.2972, 59.6067, "خراسان رضوی"),
        IranianCity("اصفهان", "Isfahan", 32.6546, 51.6680, "اصفهان"),
        IranianCity("شیراز", "Shiraz", 29.5918, 52.5837, "فارس"),
        IranianCity("تبریز", "Tabriz", 38.0800, 46.2919, "آذربایجان شرقی"),
        IranianCity("کرج", "Karaj", 35.8400, 50.9391, "البرز"),
        IranianCity("قم", "Qom", 34.6401, 50.8764, "قم"),
        IranianCity("اهواز", "Ahvaz", 31.3183, 48.6706, "خوزستان"),
        IranianCity("کرمانشاه", "Kermanshah", 34.3142, 47.0650, "کرمانشاه"),
        IranianCity("ارومیه", "Urmia", 37.5527, 45.0761, "آذربایجان غربی"),
        IranianCity("رشت", "Rasht", 37.2808, 49.5832, "گیلان"),
        IranianCity("زاهدان", "Zahedan", 29.4963, 60.8629, "سیستان و بلوچستان"),
        IranianCity("همدان", "Hamadan", 34.7989, 48.5150, "همدان"),
        IranianCity("کرمان", "Kerman", 30.2839, 57.0788, "کرمان"),
        IranianCity("یزد", "Yazd", 31.8974, 54.3569, "یزد"),
        IranianCity("اردبیل", "Ardabil", 38.2498, 48.2933, "اردبیل"),
        IranianCity("بندرعباس", "Bandar Abbas", 27.1832, 56.2666, "هرمزگان"),
        IranianCity("اراک", "Arak", 34.0954, 49.7013, "مرکزی"),
        IranianCity("زنجان", "Zanjan", 36.6736, 48.4787, "زنجان"),
        IranianCity("سنندج", "Sanandaj", 35.3219, 46.9862, "کردستان"),
        IranianCity("قزوین", "Qazvin", 36.2797, 50.0049, "قزوین"),
        IranianCity("خرم‌آباد", "Khorramabad", 33.4878, 48.3558, "لرستان"),
        IranianCity("گرگان", "Gorgan", 36.8427, 54.4347, "گلستان"),
        IranianCity("ساری", "Sari", 36.5659, 53.0586, "مازندران"),
        IranianCity("بوشهر", "Bushehr", 28.9234, 50.8203, "بوشهر"),
        IranianCity("بجنورد", "Bojnord", 37.4747, 57.3290, "خراسان شمالی"),
        IranianCity("بیرجند", "Birjand", 32.8663, 59.2211, "خراسان جنوبی"),
        IranianCity("ایلام", "Ilam", 33.6374, 46.4227, "ایلام"),
        IranianCity("شهرکرد", "Shahrekord", 32.3256, 50.8644, "چهارمحال و بختیاری"),
        IranianCity("سمنان", "Semnan", 35.5729, 53.3971, "سمنان"),
        IranianCity("یاسوج", "Yasuj", 30.6684, 51.5876, "کهگیلویه و بویراحمد"),

        // استان خراسان رضوی (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آبروان", "Abravan", 36.08, 59.94, "خراسان رضوی"),
        IranianCity("اسحاق آباد", "EshaghAbad", 36.0292, 59.0078, "خراسان رضوی"),
        IranianCity("اسدآباد خراسان", "Asad Abad", 35.63, 59.36, "خراسان رضوی"),
        IranianCity("امام تقی", "Emam Taqi", 35.94, 59.44, "خراسان رضوی"),
        IranianCity("امام قلی", "Emam Qoli", 37.4, 58.52, "خراسان رضوی"),
        IranianCity("انابد", "Anabad", 35.2502, 57.801, "خراسان رضوی"),
        IranianCity("باجگیران", "Bajgiran", 37.62, 58.42, "خراسان رضوی"),
        IranianCity("باخرز", "Bakharz", 34.995, 60.313, "خراسان رضوی"),
        IranianCity("بار", "Bar", 36.4919, 58.7164, "خراسان رضوی"),
        IranianCity("باغچه", "Baghcheh", 35.98, 59.6, "خراسان رضوی"),
        IranianCity("بایگ", "Bayg", 35.36, 59.05, "خراسان رضوی"),
        IranianCity("بجستان", "Bajestan", 34.52, 58.18, "خراسان رضوی"),
        IranianCity("بردسکن", "Bardaskan", 35.25, 57.97, "خراسان رضوی"),
        IranianCity("برزنون", "Barzanun", 36.6072, 58.29, "خراسان رضوی"),
        IranianCity("بیدخت", "Bidokht", 34.34, 58.76, "خراسان رضوی"),
        IranianCity("بینالود", "Binalood", 35.58, 59.22, "خراسان رضوی"),
        IranianCity("پروند", "Parvand", 35.9419, 57.0989, "خراسان رضوی"),
        IranianCity("تایباد", "Taybad", 34.74, 60.78, "خراسان رضوی"),
        IranianCity("تربت جام", "Torbat-e Jam", 35.24, 60.62, "خراسان رضوی"),
        IranianCity("تربت حیدریه", "Torbat-e Heydarieh", 35.27, 59.22, "خراسان رضوی"),
        IranianCity("تلخ بخش گلبو", "Talkh Bakhsh", 35.72, 58.86, "خراسان رضوی"),
        IranianCity("جغتای", "Joghatay", 36.63, 57.07, "خراسان رضوی"),
        IranianCity("جنگل", "Jangal", 34.7021, 59.2154, "خراسان رضوی"),
        IranianCity("چکنه", "Chakaneh", 36.8155, 58.5073, "خراسان رضوی"),
        IranianCity("چناران", "Chenaran", 36.65, 59.12, "خراسان رضوی"),
        IranianCity("چویلی", "Choyvali", 37.41, 58.56, "خراسان رضوی"),
        IranianCity("حارث آباد", "Hares Abad", 36.11, 57.61, "خراسان رضوی"),
        IranianCity("حصارخراسان", "Hesar", 37.44, 59.39, "خراسان رضوی"),
        IranianCity("حکم آباد", "Hokmabad", 36.63, 57.6, "خراسان رضوی"),
        IranianCity("خرو", "Kharv", 36.1689, 59.0522, "خراسان رضوی"),
        IranianCity("خلیل آباد", "Khalilabad", 35.2507, 58.2879, "خراسان رضوی"),
        IranianCity("خواف", "Khaf", 34.57, 60.14, "خراسان رضوی"),
        IranianCity("داورزن", "Davarzan", 36.35, 56.88, "خراسان رضوی"),
        IranianCity("دررود", "Darrood", 36.14, 59.11, "خراسان رضوی"),
        IranianCity("درگز", "Dargaz", 37.44, 59.11, "خراسان رضوی"),
        IranianCity("درونه", "Doruneh", 35.17, 57.42, "خراسان رضوی"),
        IranianCity("دوغارون", "Dogharon", 34.71, 61.0, "خراسان رضوی"),
        IranianCity("دولت اباد", "Dolat Abad", 35.2834, 59.5198, "خراسان رضوی"),
        IranianCity("دهانه قلعه", "Dahane Qal'eh", 35.32, 57.64, "خراسان رضوی"),
        IranianCity("رباط سفید", "Robat Sefid", 35.79, 59.39, "خراسان رضوی"),
        IranianCity("رباط سنگ", "Robat-e-Sang", 35.5472, 59.1963, "خراسان رضوی"),
        IranianCity("رشتخوار", "Roshtkhar", 34.97, 59.62, "خراسان رضوی"),
        IranianCity("روداب", "Rud Ab", 36.0272, 57.305, "خراسان رضوی"),
        IranianCity("روشناوند", "Rushnavand", 34.46, 58.91, "خراسان رضوی"),
        IranianCity("ریوش", "Rivash", 35.4771, 58.4566, "خراسان رضوی"),
        IranianCity("سبزوار", "Sabzevar", 36.21, 57.67, "خراسان رضوی"),
        IranianCity("سرخس", "Sarakhs", 36.53, 61.16, "خراسان رضوی"),
        IranianCity("سلامی", "Salami", 34.7478, 59.975, "خراسان رضوی"),
        IranianCity("سلطان آباد", "Soltanabad", 36.4039, 58.0394, "خراسان رضوی"),
        IranianCity("سمیع آباد", "Sami abad", 35.15, 60.91, "خراسان رضوی"),
        IranianCity("سنگان", "Sangan", 34.41, 60.24, "خراسان رضوی"),
        IranianCity("سنگ بست", "Sang Bast", 35.99, 59.77, "خراسان رضوی"),
        IranianCity("سنو", "Seno", 34.3128, 58.4489, "خراسان رضوی"),
        IranianCity("شادمهر", "Shadmehr", 35.1719, 59.0365, "خراسان رضوی"),
        IranianCity("شاندیز", "Shandiz", 36.4, 59.28, "خراسان رضوی"),
        IranianCity("ششتمد", "Sheshtomad", 35.97, 57.77, "خراسان رضوی"),
        IranianCity("شفیع آبادسبزوار", "Shafie Abad", 36.72, 56.62, "خراسان رضوی"),
        IranianCity("شورستان", "Shorestan", 35.72, 61.02, "خراسان رضوی"),
        IranianCity("شهر زو", "Shahrezu", 36.7492, 59.939, "خراسان رضوی"),
        IranianCity("صالح آباد خراسان", "Saleh Abad", 35.69, 61.09, "خراسان رضوی"),
        IranianCity("طرقبه", "Torqabeh", 36.31, 59.37, "خراسان رضوی"),
        IranianCity("عبدالله گیو", "Abdollah Givi", 36.69, 58.43, "خراسان رضوی"),
        IranianCity("عشق آباد", "Eshqabad", 36.0431, 58.6831, "خراسان رضوی"),
        IranianCity("عمرانی", "Omrani", 34.56, 58.7, "خراسان رضوی"),
        IranianCity("فریمان", "Fariman", 35.71, 59.85, "خراسان رضوی"),
        IranianCity("فیروزه", "Firuzeh", 36.2, 58.6, "خراسان رضوی"),
        IranianCity("فیض آباد", "Feyzabad", 35.02, 58.79, "خراسان رضوی"),
        IranianCity("قاسم آباد", "Qasemabad", 34.36, 59.87, "خراسان رضوی"),
        IranianCity("قدمگاه", "Qadamgah", 36.11, 59.06, "خراسان رضوی"),
        IranianCity("قوچان", "Quchan", 37.11, 58.51, "خراسان رضوی"),
        IranianCity("کاخک", "Kakhk", 34.15, 58.65, "خراسان رضوی"),
        IranianCity("کاشمر", "Kashmar", 35.23, 58.46, "خراسان رضوی"),
        IranianCity("کاهک", "Kahak", 36.35, 56.77, "خراسان رضوی"),
        IranianCity("کدکن", "Kadkan", 35.61, 58.88, "خراسان رضوی"),
        IranianCity("کلات نادری", "Kalat Nader", 37.0, 59.77, "خراسان رضوی"),
        IranianCity("کندر", "Kondor", 35.12, 58.09, "خراسان رضوی"),
        IranianCity("گرماب", "Garmab", 36.14, 58.25, "خراسان رضوی"),
        IranianCity("گلبهار", "Golbahar", 36.5729, 59.1709, "خراسان رضوی"),
        IranianCity("گناباد", "Gonabad", 34.35, 58.69, "خراسان رضوی"),
        IranianCity("لطف آباد", "Lotfabad", 37.52, 59.34, "خراسان رضوی"),
        IranianCity("ماروسک", "Marusk", 35.81, 58.58, "خراسان رضوی"),
        IranianCity("مشهدریزه", "Mashhad Rizeh", 34.7938, 60.5089, "خراسان رضوی"),
        IranianCity("نقاب", "Neghab", 36.71, 57.42, "خراسان رضوی"),
        IranianCity("نیازآباد", "Niazabad", 34.23, 60.24, "خراسان رضوی"),
        IranianCity("نیشابور", "Nishapur", 36.2, 58.8, "خراسان رضوی"),
        IranianCity("نیل شهر", "Nilshahr", 35.1347, 60.7736, "خراسان رضوی"),
        IranianCity("همت‌آباد", "Hemmatabad", 36.1609, 58.4522, "خراسان رضوی"),
        IranianCity("یونسی", "Yunesi", 34.8, 58.44, "خراسان رضوی"),

        // استان آذربایجان شرقی (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آذرشهر", "Azarshahr", 37.77, 45.97, "آذربایجان شرقی"),
        IranianCity("اسکو", "Osku", 37.92, 46.12, "آذربایجان شرقی"),
        IranianCity("اهر", "Ahar", 38.47, 47.07, "آذربایجان شرقی"),
        IranianCity("بستان آباد", "Bostanabad", 37.84, 46.83, "آذربایجان شرقی"),
        IranianCity("بناب", "Bonab", 37.34, 46.07, "آذربایجان شرقی"),
        IranianCity("جلفا", "Jolfa", 38.94, 45.63, "آذربایجان شرقی"),
        IranianCity("سراب", "Sarab", 37.94, 47.54, "آذربایجان شرقی"),
        IranianCity("شبستر", "Shabestar", 38.18, 45.7, "آذربایجان شرقی"),
        IranianCity("شهر جدید سهند", "Sahand", 37.944773, 46.121163, "آذربایجان شرقی"),
        IranianCity("عجب شیر", "Ajab Shir", 37.48, 45.89, "آذربایجان شرقی"),
        IranianCity("کلیبر", "Kaleybar", 38.86, 47.04, "آذربایجان شرقی"),
        IranianCity("مراغه", "Maragheh", 37.37, 46.19, "آذربایجان شرقی"),
        IranianCity("مرند", "Marand", 38.43, 45.77, "آذربایجان شرقی"),
        IranianCity("ملکان", "Malekan", 37.11, 46.11, "آذربایجان شرقی"),
        IranianCity("میانه", "Mianeh", 37.44, 47.7, "آذربایجان شرقی"),
        IranianCity("هریس", "Heris", 38.25, 47.11, "آذربایجان شرقی"),
        IranianCity("هشترود", "Hashtrud", 37.48, 47.05, "آذربایجان شرقی"),
        IranianCity("ورزقان", "Varzaqan", 38.49, 46.67, "آذربایجان شرقی"),

        // استان آذربایجان غربی (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اشنویه", "Oshnavieh", 37.04, 45.1, "آذربایجان غربی"),
        IranianCity("بوکان", "Bukan", 36.52, 46.21, "آذربایجان غربی"),
        IranianCity("پیرانشهر", "Piranshahr", 36.7, 45.14, "آذربایجان غربی"),
        IranianCity("تکاب", "Takab", 36.4, 47.11, "آذربایجان غربی"),
        IranianCity("چالدران", "Chaldoran", 39.07, 44.38, "آذربایجان غربی"),
        IranianCity("خوی", "Khoy", 38.53, 44.96, "آذربایجان غربی"),
        IranianCity("سردشت", "Sardasht", 36.18, 45.51, "آذربایجان غربی"),
        IranianCity("سلماس", "Salmas", 38.19, 44.77, "آذربایجان غربی"),
        IranianCity("شاهیندژ", "Shahin Dej", 36.68, 46.56, "آذربایجان غربی"),
        IranianCity("ماکو", "Maku", 39.3, 44.51, "آذربایجان غربی"),
        IranianCity("مهاباد", "Mahabad", 36.77, 45.72, "آذربایجان غربی"),
        IranianCity("میاندوآب", "Miandoab", 36.97, 46.1, "آذربایجان غربی"),
        IranianCity("نقده", "Naqadeh", 36.97, 45.39, "آذربایجان غربی"),

        // استان اردبیل (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("بیله سوار", "Bileh Savar", 39.37, 48.36, "اردبیل"),
        IranianCity("پارس آباد", "Parsabad", 39.65, 47.92, "اردبیل"),
        IranianCity("خلخال", "Khalkhal", 37.62, 48.53, "اردبیل"),
        IranianCity("سرعین", "Sareyn", 38.15, 48.07, "اردبیل"),
        IranianCity("گرمی", "Germi", 39.04, 48.08, "اردبیل"),
        IranianCity("مشگین شهر", "Meshginshahr", 38.4, 47.68, "اردبیل"),
        IranianCity("نمین", "Namin", 38.42, 48.49, "اردبیل"),
        IranianCity("نیر", "Nir", 38.03, 48.0, "اردبیل"),

        // استان اصفهان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آران و بیدگل", "Aran va Bidgol", 34.05, 51.48, "اصفهان"),
        IranianCity("تیران", "Tiran", 32.7, 51.15, "اصفهان"),
        IranianCity("خمینی شهر", "Khomeyni Shahr", 32.68, 51.54, "اصفهان"),
        IranianCity("خوانسار", "Khansar", 33.21, 50.32, "اصفهان"),
        IranianCity("داران", "Daran", 32.99, 50.42, "اصفهان"),
        IranianCity("دولت آباد اصفهان", "Dolatabad", 32.81, 51.69, "اصفهان"),
        IranianCity("زرین شهر", "Zarrin Shahr", 32.39, 51.38, "اصفهان"),
        IranianCity("سمیرم", "Semirom", 31.42, 51.57, "اصفهان"),
        IranianCity("شاهین شهر", "Shahin Shahr", 32.88, 51.54, "اصفهان"),
        IranianCity("شهرضا", "Shahreza", 32.01, 51.87, "اصفهان"),
        IranianCity("فلاورجان", "Falavarjan", 32.55, 51.51, "اصفهان"),
        IranianCity("فولادشهر", "Fuladshahr", 32.49, 51.41, "اصفهان"),
        IranianCity("کاشان", "Kashan", 33.99, 51.45, "اصفهان"),
        IranianCity("گلپایگان", "Golpayegan", 33.45, 50.29, "اصفهان"),
        IranianCity("مبارکه", "Mobarakeh", 32.35, 51.51, "اصفهان"),
        IranianCity("نائین", "Naein", 32.86, 53.1, "اصفهان"),
        IranianCity("نجف آباد", "Najafabad", 32.63, 51.37, "اصفهان"),

        // استان البرز (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اشتهارد", "Eshtehard", 35.72, 50.35, "البرز"),
        IranianCity("چهار باغ", "Charbagh", 35.8369, 50.8467, "البرز"),
        IranianCity("طالقان", "Taleqan", 36.17, 50.77, "البرز"),
        IranianCity("فردیس", "Fardis", 35.7247, 50.9883, "البرز"),
        IranianCity("کمال شهر", "Kamalshahr", 35.8603, 50.8739, "البرز"),
        IranianCity("ماهدشت", "Mahdasht", 35.73, 50.82, "البرز"),
        IranianCity("محمد شهر", "Mohammad Shahr", 35.76, 50.92, "البرز"),
        IranianCity("مشکین دشت", "Meshkindasht", 35.7458, 50.9511, "البرز"),
        IranianCity("نظرآباد", "Nazarabad", 35.96, 50.61, "البرز"),
        IranianCity("هشتگرد", "Hashtgerd", 35.96, 50.69, "البرز"),

        // استان ایلام (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آبدانان", "Abdanan", 32.99, 47.42, "ایلام"),
        IranianCity("ارکواز", "Arkovaz", 33.38, 46.6, "ایلام"),
        IranianCity("ایوان", "Eyvan", 33.83, 46.31, "ایلام"),
        IranianCity("دره شهر", "Darreh Shahr", 33.15, 47.39, "ایلام"),
        IranianCity("دهلران", "Dehloran", 32.7, 47.26, "ایلام"),
        IranianCity("سرابله", "Sarableh", 33.77, 46.56, "ایلام"),
        IranianCity("مهران", "Mehran", 33.12, 46.16, "ایلام"),

        // استان بوشهر (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اهرم", "Ahram", 28.88, 51.28, "بوشهر"),
        IranianCity("برازجان", "Borazjan", 29.27, 51.21, "بوشهر"),
        IranianCity("بندر دیر", "Dayyer", 27.84, 51.93, "بوشهر"),
        IranianCity("بندر گناوه", "Bandar Ganaveh", 29.57, 50.52, "بوشهر"),
        IranianCity("جم", "Jam", 27.824085, 52.327742, "بوشهر"),
        IranianCity("خورموج", "Khormoj", 28.65, 51.38, "بوشهر"),
        IranianCity("دیلم", "Deylam", 30.06, 50.17, "بوشهر"),
        IranianCity("عسلویه", "Asaluyeh", 27.499593, 52.597239, "بوشهر"),
        IranianCity("کنگان", "Kangan", 27.83, 52.06, "بوشهر"),

        // استان تهران (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اسلام شهر", "Eslamshahr", 35.56, 51.24, "تهران"),
        IranianCity("اندیشه", "Andisheh", 35.708563, 51.015475, "تهران"),
        IranianCity("باقرشهر", "Baqershahr", 35.535426, 51.401148, "تهران"),
        IranianCity("بومهن", "Bumahen", 35.73, 51.87, "تهران"),
        IranianCity("پاکدشت", "Pakdasht", 35.47, 51.67, "تهران"),
        IranianCity("پردیس", "Pardis", 35.7469, 51.7962, "تهران"),
        IranianCity("پرند", "Parand", 35.4666, 50.983, "تهران"),
        IranianCity("پیشوا", "Pishva", 35.308326, 51.728954, "تهران"),
        IranianCity("دماوند", "Damavand", 35.72, 52.07, "تهران"),
        IranianCity("رباط کریم", "Robat Karim", 35.48, 51.08, "تهران"),
        IranianCity("رودهن", "Rudehen", 35.74, 51.91, "تهران"),
        IranianCity("شهرری", "Shahr-e-Rey", 35.59, 51.43, "تهران"),
        IranianCity("شهریار", "Shahriar", 35.65, 51.06, "تهران"),
        IranianCity("فیروزکوه", "Firuzkuh", 35.76, 52.78, "تهران"),
        IranianCity("قدس", "Qods", 35.72, 51.11, "تهران"),
        IranianCity("قرچک", "Qarchak", 35.44, 51.57, "تهران"),
        IranianCity("گلستان", "Golestan", 35.51, 51.16, "تهران"),
        IranianCity("ملارد", "Malard", 35.65, 51.0, "تهران"),
        IranianCity("نسیم شهر", "Nasimshahr", 35.55, 51.1667, "تهران"),
        IranianCity("ورامین", "Varamin", 35.33, 51.65, "تهران"),

        // استان چهارمحال و بختیاری (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اردل", "Ardal", 32.0, 50.66, "چهارمحال و بختیاری"),
        IranianCity("بروجن", "Borujen", 31.97, 51.28, "چهارمحال و بختیاری"),
        IranianCity("چلگرد", "Chelgard", 32.46, 50.14, "چهارمحال و بختیاری"),
        IranianCity("فارسان", "Farsan", 32.26, 50.57, "چهارمحال و بختیاری"),
        IranianCity("فرخشهر", "Farrokh Shahr", 32.27, 50.98, "چهارمحال و بختیاری"),
        IranianCity("لردگان", "Lordegan", 31.51, 50.82, "چهارمحال و بختیاری"),

        // استان خراسان جنوبی (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اسدیه", "Asadiyeh", 32.92, 60.02, "خراسان جنوبی"),
        IranianCity("بشرویه", "Boshruyeh", 33.87, 57.43, "خراسان جنوبی"),
        IranianCity("سرایان", "Sarayan", 33.86, 58.53, "خراسان جنوبی"),
        IranianCity("سربیشه", "Sarbisheh", 32.57, 59.8, "خراسان جنوبی"),
        IranianCity("طبس", "Tabas", 33.59, 56.92, "خراسان جنوبی"),
        IranianCity("فردوس", "Ferdows", 34.02, 58.17, "خراسان جنوبی"),
        IranianCity("قاین", "Qaen", 33.73, 59.19, "خراسان جنوبی"),
        IranianCity("نهبندان", "Nehbandan", 31.54, 60.04, "خراسان جنوبی"),

        // استان خراسان شمالی (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اسفراین", "Esfarayen", 37.08, 57.51, "خراسان شمالی"),
        IranianCity("آشخانه", "Ashkhaneh", 37.56, 56.93, "خراسان شمالی"),
        IranianCity("جاجرم", "Jajarm", 36.97, 56.31, "خراسان شمالی"),
        IranianCity("راز", "Raz", 37.9335, 57.111, "خراسان شمالی"),
        IranianCity("شیروان", "Shirvan", 37.4, 57.93, "خراسان شمالی"),
        IranianCity("فاروج", "Faruj", 37.23, 58.22, "خراسان شمالی"),
        IranianCity("گرمه", "Garmeh", 36.99, 56.29, "خراسان شمالی"),

        // استان خوزستان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آبادان", "Abadan", 30.38, 48.29, "خوزستان"),
        IranianCity("امیدیه", "Omidiyeh", 30.77, 49.71, "خوزستان"),
        IranianCity("اندیمشک", "Andimeshk", 32.46, 48.35, "خوزستان"),
        IranianCity("ایذه", "Izeh", 31.84, 49.87, "خوزستان"),
        IranianCity("باغملک", "Baghmalek", 31.52, 49.89, "خوزستان"),
        IranianCity("بندر امام", "Bandar Imam Khomeini", 30.44, 49.08, "خوزستان"),
        IranianCity("بهبهان", "Behbahan", 30.6, 50.24, "خوزستان"),
        IranianCity("خرمشهر", "Khorramshahr", 30.44, 48.18, "خوزستان"),
        IranianCity("دزفول", "Dezful", 32.38, 48.4, "خوزستان"),
        IranianCity("رامهرمز", "Ramhormoz", 31.27, 49.6, "خوزستان"),
        IranianCity("سوسنگرد", "Susangerd", 31.56, 48.19, "خوزستان"),
        IranianCity("شادگان", "Shadegan", 30.65, 48.67, "خوزستان"),
        IranianCity("شوش", "Shush", 32.2, 48.25, "خوزستان"),
        IranianCity("شوشتر", "Shushtar", 32.05, 48.86, "خوزستان"),
        IranianCity("گتوند", "Gotvand", 32.17, 48.82, "خوزستان"),
        IranianCity("ماهشهر", "Mahshahr", 30.56, 49.2, "خوزستان"),
        IranianCity("مسجدسلیمان", "Masjed Soleyman", 31.93, 49.31, "خوزستان"),
        IranianCity("هندیجان", "Hendijan", 30.24, 49.66, "خوزستان"),

        // استان زنجان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آببر", "Abbar", 36.923629, 48.963344, "زنجان"),
        IranianCity("ابهر", "Abhar", 36.16, 49.23, "زنجان"),
        IranianCity("خرمدره", "Khorramdarreh", 36.25, 49.16, "زنجان"),
        IranianCity("قیدار", "Qeydar", 36.12, 48.59, "زنجان"),
        IranianCity("ماه نشان", "Mah Neshan", 36.74, 47.66, "زنجان"),

        // استان سمنان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("دامغان", "Damghan", 36.17, 54.35, "سمنان"),
        IranianCity("سرخه", "Sorkheh", 35.467672, 53.215476, "سمنان"),
        IranianCity("شاهرود", "Shahrud", 36.42, 54.96, "سمنان"),
        IranianCity("گرمسار", "Garmsar", 35.22, 52.34, "سمنان"),
        IranianCity("مهدی شهر", "Mahdi Shahr", 35.7, 53.35, "سمنان"),

        // استان سیستان و بلوچستان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("ایرانشهر", "Iranshahr", 27.21, 60.7, "سیستان و بلوچستان"),
        IranianCity("چابهار", "Chabahar", 25.29, 60.67, "سیستان و بلوچستان"),
        IranianCity("خاش", "Khash", 28.22, 61.21, "سیستان و بلوچستان"),
        IranianCity("دوست محمد", "Dust Mohammad", 31.14, 61.79, "سیستان و بلوچستان"),
        IranianCity("راسک", "Rask", 26.233054, 61.404188, "سیستان و بلوچستان"),
        IranianCity("زابل", "Zabol", 31.02, 61.49, "سیستان و بلوچستان"),
        IranianCity("زهک", "Zehak", 30.9, 61.68, "سیستان و بلوچستان"),
        IranianCity("سراوان", "Saravan", 27.37, 62.34, "سیستان و بلوچستان"),
        IranianCity("کنارک", "Konarak", 25.36, 60.4, "سیستان و بلوچستان"),
        IranianCity("میرجاوه", "Mirjaveh", 29.02, 61.45, "سیستان و بلوچستان"),
        IranianCity("نیک شهر", "Nikshahr", 26.23, 60.22, "سیستان و بلوچستان"),

        // استان فارس (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آباده", "Abadeh", 31.16, 52.65, "فارس"),
        IranianCity("استهبان", "Estahban", 29.14, 54.0, "فارس"),
        IranianCity("اقلید", "Eqlid", 30.9, 52.69, "فارس"),
        IranianCity("جهرم", "Jahrom", 28.5, 53.55, "فارس"),
        IranianCity("حاجی آباد فارس", "HajiAbad", 28.36, 54.42, "فارس"),
        IranianCity("داراب", "Darab", 28.75, 54.55, "فارس"),
        IranianCity("زرقان", "Zarqan", 29.77, 52.7, "فارس"),
        IranianCity("شهر جدید صدرا", "Sadra", 29.8, 52.4, "فارس"),
        IranianCity("فسا", "Fasa", 28.94, 53.65, "فارس"),
        IranianCity("فیروزآباد فارس", "Firuzabad", 28.84, 52.57, "فارس"),
        IranianCity("کازرون", "Kazerun", 29.62, 51.65, "فارس"),
        IranianCity("گراش", "Gerash", 27.67, 54.14, "فارس"),
        IranianCity("لار", "Lar", 27.66, 54.32, "فارس"),
        IranianCity("لامرد", "Lamerd", 27.34, 53.18, "فارس"),
        IranianCity("مرودشت", "Marvdasht", 29.87, 52.79, "فارس"),
        IranianCity("نورآباد ممسنی", "Nourabad", 30.11, 51.54, "فارس"),
        IranianCity("نی ریز", "Neyriz", 29.2, 54.33, "فارس"),

        // استان قزوین (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آبیک", "Abyek", 36.04, 50.53, "قزوین"),
        IranianCity("اقبالیه", "Eqbaliyeh", 36.23, 49.92, "قزوین"),
        IranianCity("الوند", "Alvand", 36.187764, 50.04089, "قزوین"),
        IranianCity("بوئین زهرا", "Buin Zahra", 35.77, 50.06, "قزوین"),
        IranianCity("تاکستان", "Takestan", 36.07, 49.7, "قزوین"),
        IranianCity("شهرک محمدیه", "Mohamadie", 36.23, 50.2, "قزوین"),

        // استان قم (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("جعفریه", "Jafariyeh", 34.78, 50.51, "قم"),
        IranianCity("دستجرد", "Dastjerd", 34.530317, 50.234642, "قم"),
        IranianCity("سلفچگان", "Salafchegan", 34.48, 50.47, "قم"),
        IranianCity("کهک قم", "Kahak", 34.4, 50.87, "قم"),

        // استان کردستان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("بانه", "Baneh", 35.99, 45.89, "کردستان"),
        IranianCity("بیجار", "Bijar", 35.87, 47.61, "کردستان"),
        IranianCity("دهگلان", "Dehgolan", 35.28, 47.42, "کردستان"),
        IranianCity("دیواندره", "Divandarreh", 35.91, 47.02, "کردستان"),
        IranianCity("سقز", "Saqqez", 36.24, 46.28, "کردستان"),
        IranianCity("قروه", "Qorveh", 35.17, 47.8, "کردستان"),
        IranianCity("کامیاران", "Kamyaran", 34.8, 46.94, "کردستان"),
        IranianCity("مریوان", "Marivan", 35.53, 46.18, "کردستان"),

        // استان کرمان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("بافت", "Baft", 29.24, 56.6, "کرمان"),
        IranianCity("بردسیر", "Bardsir", 29.93, 56.57, "کرمان"),
        IranianCity("بم", "Bam", 29.11, 58.36, "کرمان"),
        IranianCity("جیرفت", "Jiroft", 28.67, 57.74, "کرمان"),
        IranianCity("رفسنجان", "Rafsanjan", 30.41, 55.99, "کرمان"),
        IranianCity("رودبار کرمان", "Rudbar", 28.029429, 57.992726, "کرمان"),
        IranianCity("زرند", "Zarand", 30.81, 56.57, "کرمان"),
        IranianCity("سیرجان", "Sirjan", 29.45, 55.67, "کرمان"),
        IranianCity("شهربابک", "Shahrbabak", 30.12, 55.12, "کرمان"),
        IranianCity("عنبرآباد", "Anbarabad", 28.47, 57.85, "کرمان"),
        IranianCity("قلعه گنج", "Qaleh Ganj", 27.52, 57.88, "کرمان"),
        IranianCity("کهنوج", "Kahnooj", 27.94, 57.7, "کرمان"),
        IranianCity("منوجان", "Manujan", 27.41, 57.5, "کرمان"),

        // استان کرمانشاه (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اسلام آباد غرب", "Eslamabad-e-Gharb", 34.11, 46.53, "کرمانشاه"),
        IranianCity("پاوه", "Paveh", 35.04, 46.37, "کرمانشاه"),
        IranianCity("جوانرود", "Javanrud", 34.8, 46.5, "کرمانشاه"),
        IranianCity("روانسر", "Ravansar", 34.75, 46.683333, "کرمانشاه"),
        IranianCity("سرپل ذهاب", "Sarpol-e Zahab", 34.46, 45.86, "کرمانشاه"),
        IranianCity("سنقر", "Sonqor", 34.78, 47.6, "کرمانشاه"),
        IranianCity("صحنه", "Sahneh", 34.48, 47.68, "کرمانشاه"),
        IranianCity("قصرشیرین", "Qasr-e Shirin", 34.51, 45.57, "کرمانشاه"),
        IranianCity("کنگاور", "Kangavar", 34.5, 47.96, "کرمانشاه"),
        IranianCity("گیلانغرب", "Gilan-e Gharb", 34.14, 45.92, "کرمانشاه"),
        IranianCity("هرسین", "Harsin", 34.27, 47.58, "کرمانشاه"),

        // استان کهگیلویه و بویراحمد (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("باشت", "Basht", 30.35, 51.17, "کهگیلویه و بویراحمد"),
        IranianCity("چرام", "Charam", 30.75, 50.74, "کهگیلویه و بویراحمد"),
        IranianCity("دهدشت", "Dehdasht", 30.79, 50.57, "کهگیلویه و بویراحمد"),
        IranianCity("دوگنبدان", "Dogonbadan", 30.36, 50.79, "کهگیلویه و بویراحمد"),
        IranianCity("سی سخت", "Sisakht", 30.85, 51.46, "کهگیلویه و بویراحمد"),
        IranianCity("لیکک", "Likak", 30.9, 50.09, "کهگیلویه و بویراحمد"),

        // استان گلستان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آزادشهر", "Azadshahr", 37.08, 55.18, "گلستان"),
        IranianCity("آق قلا", "Aqqala", 37.01, 54.46, "گلستان"),
        IranianCity("بندر ترکمن", "Bandar Torkaman", 36.89, 54.07, "گلستان"),
        IranianCity("رامیان", "Ramian", 37.02, 55.14, "گلستان"),
        IranianCity("علی آباد کتول", "Aliabad-e-Katul", 36.906459, 54.858942, "گلستان"),
        IranianCity("کرد کوی", "Kordkuy", 36.8, 54.11, "گلستان"),
        IranianCity("کلاله", "Kalaleh", 37.38, 55.49, "گلستان"),
        IranianCity("گالیکش", "Galikesh", 37.27, 55.43, "گلستان"),
        IranianCity("گنبد کاووس", "Gonbad-e Kavus", 37.25, 55.17, "گلستان"),
        IranianCity("مینودشت", "Minudasht", 37.23, 55.36, "گلستان"),

        // استان گیلان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آستارا", "Astara", 38.44, 48.87, "گیلان"),
        IranianCity("آستانه اشرفیه", "Astaneh-ye Ashrafiyeh", 37.26, 49.94, "گیلان"),
        IranianCity("بندر انزلی", "Bandar Anzali", 37.47, 49.46, "گیلان"),
        IranianCity("تالش", "Talesh", 37.8, 48.9, "گیلان"),
        IranianCity("رضوانشهر", "Rezvanshahr", 37.55, 49.14, "گیلان"),
        IranianCity("رودبار گیلان", "Rudbar", 36.81, 49.41, "گیلان"),
        IranianCity("رودسر", "Rudsar", 37.13, 50.29, "گیلان"),
        IranianCity("سیاهکل", "Siahkal", 37.15, 49.87, "گیلان"),
        IranianCity("صومعه سرا", "Someh Sara", 37.3, 49.31, "گیلان"),
        IranianCity("فومن", "Fuman", 37.22, 49.31, "گیلان"),
        IranianCity("لاهیجان", "Lahijan", 37.21, 50.0, "گیلان"),
        IranianCity("لنگرود", "Langarud", 37.19, 50.15, "گیلان"),
        IranianCity("ماسال", "Masal", 37.36, 49.13, "گیلان"),

        // استان لرستان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("ازنا", "Azna", 33.46, 49.46, "لرستان"),
        IranianCity("الشتر", "Aleshtar", 33.86, 48.26, "لرستان"),
        IranianCity("الیگودرز", "Aligudarz", 33.4, 49.69, "لرستان"),
        IranianCity("بروجرد", "Borujerd", 33.89, 48.76, "لرستان"),
        IranianCity("پلدختر", "Pol-e Dokhtar", 33.15, 47.71, "لرستان"),
        IranianCity("دورود", "Dorud", 33.48, 49.06, "لرستان"),
        IranianCity("کوهدشت", "Kuhdasht", 33.53, 47.61, "لرستان"),
        IranianCity("نورآباد لرستان", "Nurabad", 34.08, 47.97, "لرستان"),

        // استان مازندران (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آمل", "Amol", 36.47, 52.35, "مازندران"),
        IranianCity("بابل", "Babol", 36.54, 52.68, "مازندران"),
        IranianCity("بابلسر", "Babolsar", 36.7, 52.65, "مازندران"),
        IranianCity("بهشهر", "Behshahr", 36.69, 53.54, "مازندران"),
        IranianCity("تنکابن", "Tonekabon", 36.82, 50.88, "مازندران"),
        IranianCity("جویبار", "Juybar", 36.65, 52.91, "مازندران"),
        IranianCity("چالوس", "Chalus", 36.65, 51.42, "مازندران"),
        IranianCity("رامسر", "Ramsar", 36.92, 50.64, "مازندران"),
        IranianCity("فریدونکنار", "Fereydunkenar", 36.68, 52.51, "مازندران"),
        IranianCity("قائمشهر", "Qaemshahr", 36.46, 52.86, "مازندران"),
        IranianCity("محمودآباد مازندران", "Mahmudabad", 36.63, 52.26, "مازندران"),
        IranianCity("نکا", "Neka", 36.65, 53.3, "مازندران"),
        IranianCity("نور", "Nur", 36.58, 52.01, "مازندران"),
        IranianCity("نوشهر", "Nowshahr", 36.65, 51.4833, "مازندران"),

        // استان مرکزی (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آشتیان", "Ashtian", 34.53, 50.0, "مرکزی"),
        IranianCity("تفرش", "Tafresh", 34.69, 50.02, "مرکزی"),
        IranianCity("خمین", "Khomeyn", 33.64, 50.08, "مرکزی"),
        IranianCity("دلیجان", "Delijan", 33.99, 50.68, "مرکزی"),
        IranianCity("ساوه", "Saveh", 35.03, 50.37, "مرکزی"),
        IranianCity("شازند", "Shazand", 33.93, 49.41, "مرکزی"),
        IranianCity("کمیجان", "Komijan", 34.72, 49.32, "مرکزی"),
        IranianCity("مامونیه", "Mamuniyeh", 35.38, 50.58, "مرکزی"),
        IranianCity("محلات", "Mahallat", 33.91, 50.45, "مرکزی"),

        // استان هرمزگان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("بستک", "Bastak", 27.2, 54.36, "هرمزگان"),
        IranianCity("بندر خمیر", "Bandar Khamir", 26.95, 55.59, "هرمزگان"),
        IranianCity("بندر لنگه", "Bandar Lengeh", 26.55, 54.88, "هرمزگان"),
        IranianCity("پارسیان", "Parsian", 27.21, 53.03, "هرمزگان"),
        IranianCity("جاسک", "Jask", 25.65, 57.78, "هرمزگان"),
        IranianCity("حاجی آباد هرمزگان", "Haji Abad", 28.31, 55.89, "هرمزگان"),
        IranianCity("درگهان", "Dargahan", 26.961888, 56.084148, "هرمزگان"),
        IranianCity("رودان", "Rudan", 27.61, 57.19, "هرمزگان"),
        IranianCity("قشم", "Qeshm", 26.8676, 56.0337, "هرمزگان"),
        IranianCity("کیش", "Kish", 26.53, 53.98, "هرمزگان"),
        IranianCity("میناب", "Minab", 27.15, 57.08, "هرمزگان"),

        // استان همدان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اسدآباد", "Asadabad", 34.78, 48.12, "همدان"),
        IranianCity("بهار", "Bahar", 34.9, 48.45, "همدان"),
        IranianCity("تویسرکان", "Tuyserkan", 34.55, 48.45, "همدان"),
        IranianCity("رزن", "Razan", 35.38, 49.03, "همدان"),
        IranianCity("فامنین", "Famenin", 35.116667, 48.966667, "همدان"),
        IranianCity("کبودرآهنگ", "Kabudrahang", 35.21, 48.72, "همدان"),
        IranianCity("ملایر", "Malayer", 34.29, 48.82, "همدان"),
        IranianCity("نهاوند", "Nahavand", 34.19, 48.37, "همدان"),

        // استان یزد (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("ابرکوه", "Abarkuh", 31.13, 53.29, "یزد"),
        IranianCity("اردکان", "Ardakan", 32.31, 54.02, "یزد"),
        IranianCity("اشکذر", "Ashkezar", 32.01, 54.23, "یزد"),
        IranianCity("بافق", "Bafq", 31.6, 55.41, "یزد"),
        IranianCity("تفت", "Taft", 31.74, 54.2, "یزد"),
        IranianCity("مهریز", "Mehriz", 31.58, 54.48, "یزد"),
        IranianCity("میبد", "Meybod", 32.22, 54.01, "یزد"),
        IranianCity("هرات یزد", "Harat", 30.08, 54.36, "یزد"),
    )

    fun getCityByName(name: String): IranianCity {
        return CITIES.find { it.nameFa == name || it.nameEn.equals(name, ignoreCase = true) }
            ?: CITIES.first() // Default to Tehran
    }

    /** Province names in Persian dictionary order. */
    fun provinces(): List<String> =
        CITIES.map { it.province }.distinct().sortedWith(persianNameComparator)

    /** Cities of one province, in list order. */
    fun citiesInProvince(province: String): List<IranianCity> =
        CITIES.filter { it.province == province }

    /** Every city grouped under its province; provinces in Persian dictionary order. */
    fun citiesByProvince(): Map<String, List<IranianCity>> =
        provinces().associateWith { citiesInProvince(it) }

    /** Search across all cities by Persian or English name; Arabic ي/ك spellings also match. */
    fun searchCities(query: String): List<IranianCity> {
        val q = normalizeCitySearchText(query)
        if (q.isEmpty()) return CITIES
        val qEn = query.trim()
        return CITIES.filter {
            normalizeCitySearchText(it.nameFa).contains(q) || it.nameEn.contains(qEn, ignoreCase = true)
        }
    }

    private fun d2r(d: Double): Double = d * Math.PI / 180.0
    private fun r2d(r: Double): Double = r * 180.0 / Math.PI

    private fun fixHour(h: Double): Double {
        var res = h - 24.0 * floor(h / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(d2r(g)) + 0.020 * sin(d2r(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val ra = r2d(atan2(cos(d2r(e)) * sin(d2r(l)), cos(d2r(l)))) / 15.0
        val delta = r2d(asin(sin(d2r(e)) * sin(d2r(l))))
        return Pair(fixHour(ra), delta)
    }

    private fun equationOfTime(jd: Double): Double {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(d2r(g)) + 0.020 * sin(d2r(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val ra = r2d(atan2(cos(d2r(e)) * sin(d2r(l)), cos(d2r(l))))
        val eqt = (q - fixAngle(ra)) / 15.0
        return eqt
    }

    private fun sunHourAngle(angle: Double, lat: Double, dec: Double, isMorning: Boolean): Double {
        val latRad = d2r(lat)
        val decRad = d2r(dec)
        val top = -sin(d2r(angle)) - sin(latRad) * sin(decRad)
        val bottom = cos(latRad) * cos(decRad)
        val cosH = top / bottom
        if (cosH > 1.0 || cosH < -1.0) return 0.0 // Sun never reaches angle
        val h = r2d(acos(cosH)) / 15.0
        return if (isMorning) -h else h
    }

    /**
     * Calculates prayer times according to Tehran University standard (Tehran Institute of Geophysics).
     * Timezone is +3.5 for standard Iran time (IRT/IRST).
     */
    fun calculatePrayerTimes(
        lat: Double,
        lng: Double,
        year: Int,
        month: Int,
        day: Int,
        timezoneOffsetHours: Double = 3.5
    ): PrayerTimes {
        val jd = julianDate(year, month, day)
        val eqt = equationOfTime(jd)
        val (_, dec) = sunPosition(jd)

        // Midday (Dhuhr)
        val dhuhr = fixHour(12.0 + timezoneOffsetHours - (lng / 15.0) - eqt)

        // Fajr (Tehran method: 17.7 degrees below horizon)
        val fajrAngleDiff = sunHourAngle(FAJR_ANGLE, lat, dec, isMorning = true)
        val fajr = fixHour(dhuhr + fajrAngleDiff)

        // Sunrise (0.8333 degrees below horizon)
        val sunriseAngleDiff = sunHourAngle(SUNRISE_SUNSET_ANGLE, lat, dec, isMorning = true)
        val sunrise = fixHour(dhuhr + sunriseAngleDiff)

        // Sunset (0.8333 degrees below horizon)
        val sunsetAngleDiff = sunHourAngle(SUNRISE_SUNSET_ANGLE, lat, dec, isMorning = false)
        val sunset = fixHour(dhuhr + sunsetAngleDiff)

        // Maghrib (Tehran method: 4.5 degrees below horizon)
        val maghribAngleDiff = sunHourAngle(MAGHRIB_ANGLE, lat, dec, isMorning = false)
        val maghrib = fixHour(dhuhr + maghribAngleDiff)

        // Midnight (Tehran method: halfway between sunset/maghrib and tomorrow's fajr)
        // Midnight = (Sunset + Fajr + 24) / 2
        val midnight = fixHour((sunset + fajr + (if (fajr < sunset) 24.0 else 0.0)) / 2.0)

        return PrayerTimes(
            fajr = formatTime(fajr),
            sunrise = formatTime(sunrise),
            dhuhr = formatTime(dhuhr),
            sunset = formatTime(sunset),
            maghrib = formatTime(maghrib),
            midnight = formatTime(midnight),
            fajrHour = fajr,
            sunriseHour = sunrise,
            dhuhrHour = dhuhr,
            sunsetHour = sunset,
            maghribHour = maghrib,
            midnightHour = midnight
        )
    }

    private fun formatTime(hourFraction: Double): String {
        val fixed = fixHour(hourFraction)
        val totalMinutes = (fixed * 60.0).roundToInt()
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        return String.format("%02d:%02d", hours, minutes)
    }

    fun getNextPrayer(times: PrayerTimes, currentHour: Int, currentMinute: Int): NextPrayerInfo {
        val currentMinutes = currentHour * 60 + currentMinute

        val prayerList = listOf(
            Triple("اذان صبح", times.fajr, (times.fajrHour * 60).roundToInt()),
            Triple("طلوع آفتاب", times.sunrise, (times.sunriseHour * 60).roundToInt()),
            Triple("اذان ظهر", times.dhuhr, (times.dhuhrHour * 60).roundToInt()),
            Triple("غروب آفتاب", times.sunset, (times.sunsetHour * 60).roundToInt()),
            Triple("اذان مغرب", times.maghrib, (times.maghribHour * 60).roundToInt()),
            Triple("نیمه‌شب شرعی", times.midnight, (times.midnightHour * 60).roundToInt())
        )

        for (prayer in prayerList) {
            val diff = prayer.third - currentMinutes
            if (diff > 0) {
                return NextPrayerInfo(prayer.first, prayer.second, diff)
            }
        }

        // If all today's prayers have passed, next is tomorrow's Fajr
        val tomorrowFajrMinutes = (times.fajrHour * 60).roundToInt() + (24 * 60)
        val diff = tomorrowFajrMinutes - currentMinutes
        return NextPrayerInfo("اذان صبح (فردا)", times.fajr, diff)
    }
}
