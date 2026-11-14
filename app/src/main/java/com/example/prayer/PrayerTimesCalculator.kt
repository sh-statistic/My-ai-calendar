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
    val longitude: Double
)

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
        IranianCity("تهران", "Tehran", 35.6892, 51.3890),
        IranianCity("مشهد", "Mashhad", 36.2972, 59.6067),
        IranianCity("اصفهان", "Isfahan", 32.6546, 51.6680),
        IranianCity("شیراز", "Shiraz", 29.5918, 52.5837),
        IranianCity("تبریز", "Tabriz", 38.0800, 46.2919),
        IranianCity("کرج", "Karaj", 35.8400, 50.9391),
        IranianCity("قم", "Qom", 34.6401, 50.8764),
        IranianCity("اهواز", "Ahvaz", 31.3183, 48.6706),
        IranianCity("کرمانشاه", "Kermanshah", 34.3142, 47.0650),
        IranianCity("ارومیه", "Urmia", 37.5527, 45.0761),
        IranianCity("رشت", "Rasht", 37.2808, 49.5832),
        IranianCity("زاهدان", "Zahedan", 29.4963, 60.8629),
        IranianCity("همدان", "Hamadan", 34.7989, 48.5150),
        IranianCity("کرمان", "Kerman", 30.2839, 57.0788),
        IranianCity("یزد", "Yazd", 31.8974, 54.3569),
        IranianCity("اردبیل", "Ardabil", 38.2498, 48.2933),
        IranianCity("بندرعباس", "Bandar Abbas", 27.1832, 56.2666),
        IranianCity("اراک", "Arak", 34.0954, 49.7013),
        IranianCity("زنجان", "Zanjan", 36.6736, 48.4787),
        IranianCity("سنندج", "Sanandaj", 35.3219, 46.9862),
        IranianCity("قزوین", "Qazvin", 36.2797, 50.0049),
        IranianCity("خرم‌آباد", "Khorramabad", 33.4878, 48.3558),
        IranianCity("گرگان", "Gorgan", 36.8427, 54.4347),
        IranianCity("ساری", "Sari", 36.5659, 53.0586),
        IranianCity("بوشهر", "Bushehr", 28.9234, 50.8203),
        IranianCity("بجنورد", "Bojnord", 37.4747, 57.3290),
        IranianCity("بیرجند", "Birjand", 32.8663, 59.2211),
        IranianCity("ایلام", "Ilam", 33.6374, 46.4227),
        IranianCity("شهرکرد", "Shahrekord", 32.3256, 50.8644),
        IranianCity("سمنان", "Semnan", 35.5729, 53.3971),
        IranianCity("یاسوج", "Yasuj", 30.6684, 51.5876),

        // استان خراسان رضوی (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آبروان", "Abravan", 36.08, 59.94),
        IranianCity("اسحاق آباد", "EshaghAbad", 36.0292, 59.0078),
        IranianCity("اسدآباد خراسان", "Asad Abad", 35.63, 59.36),
        IranianCity("امام تقی", "Emam Taqi", 35.94, 59.44),
        IranianCity("امام قلی", "Emam Qoli", 37.4, 58.52),
        IranianCity("انابد", "Anabad", 35.2502, 57.801),
        IranianCity("باجگیران", "Bajgiran", 37.62, 58.42),
        IranianCity("باخرز", "Bakharz", 34.995, 60.313),
        IranianCity("بار", "Bar", 36.4919, 58.7164),
        IranianCity("باغچه", "Baghcheh", 35.98, 59.6),
        IranianCity("بایگ", "Bayg", 35.36, 59.05),
        IranianCity("بجستان", "Bajestan", 34.52, 58.18),
        IranianCity("بردسکن", "Bardaskan", 35.25, 57.97),
        IranianCity("برزنون", "Barzanun", 36.6072, 58.29),
        IranianCity("بیدخت", "Bidokht", 34.34, 58.76),
        IranianCity("بینالود", "Binalood", 35.58, 59.22),
        IranianCity("پروند", "Parvand", 35.9419, 57.0989),
        IranianCity("تایباد", "Taybad", 34.74, 60.78),
        IranianCity("تربت جام", "Torbat-e Jam", 35.24, 60.62),
        IranianCity("تربت حیدریه", "Torbat-e Heydarieh", 35.27, 59.22),
        IranianCity("تلخ بخش گلبو", "Talkh Bakhsh", 35.72, 58.86),
        IranianCity("جغتای", "Joghatay", 36.63, 57.07),
        IranianCity("جنگل", "Jangal", 34.7021, 59.2154),
        IranianCity("چکنه", "Chakaneh", 36.8155, 58.5073),
        IranianCity("چناران", "Chenaran", 36.65, 59.12),
        IranianCity("چویلی", "Choyvali", 37.41, 58.56),
        IranianCity("حارث آباد", "Hares Abad", 36.11, 57.61),
        IranianCity("حصارخراسان", "Hesar", 37.44, 59.39),
        IranianCity("حکم آباد", "Hokmabad", 36.63, 57.6),
        IranianCity("خرو", "Kharv", 36.1689, 59.0522),
        IranianCity("خلیل آباد", "Khalilabad", 35.2507, 58.2879),
        IranianCity("خواف", "Khaf", 34.57, 60.14),
        IranianCity("داورزن", "Davarzan", 36.35, 56.88),
        IranianCity("دررود", "Darrood", 36.14, 59.11),
        IranianCity("درگز", "Dargaz", 37.44, 59.11),
        IranianCity("درونه", "Doruneh", 35.17, 57.42),
        IranianCity("دوغارون", "Dogharon", 34.71, 61.0),
        IranianCity("دولت اباد", "Dolat Abad", 35.2834, 59.5198),
        IranianCity("دهانه قلعه", "Dahane Qal'eh", 35.32, 57.64),
        IranianCity("رباط سفید", "Robat Sefid", 35.79, 59.39),
        IranianCity("رباط سنگ", "Robat-e-Sang", 35.5472, 59.1963),
        IranianCity("رشتخوار", "Roshtkhar", 34.97, 59.62),
        IranianCity("روداب", "Rud Ab", 36.0272, 57.305),
        IranianCity("روشناوند", "Rushnavand", 34.46, 58.91),
        IranianCity("ریوش", "Rivash", 35.4771, 58.4566),
        IranianCity("سبزوار", "Sabzevar", 36.21, 57.67),
        IranianCity("سرخس", "Sarakhs", 36.53, 61.16),
        IranianCity("سلامی", "Salami", 34.7478, 59.975),
        IranianCity("سلطان آباد", "Soltanabad", 36.4039, 58.0394),
        IranianCity("سمیع آباد", "Sami abad", 35.15, 60.91),
        IranianCity("سنگان", "Sangan", 34.41, 60.24),
        IranianCity("سنگ بست", "Sang Bast", 35.99, 59.77),
        IranianCity("سنو", "Seno", 34.3128, 58.4489),
        IranianCity("شادمهر", "Shadmehr", 35.1719, 59.0365),
        IranianCity("شاندیز", "Shandiz", 36.4, 59.28),
        IranianCity("ششتمد", "Sheshtomad", 35.97, 57.77),
        IranianCity("شفیع آبادسبزوار", "Shafie Abad", 36.72, 56.62),
        IranianCity("شورستان", "Shorestan", 35.72, 61.02),
        IranianCity("شهر زو", "Shahrezu", 36.7492, 59.939),
        IranianCity("صالح آباد خراسان", "Saleh Abad", 35.69, 61.09),
        IranianCity("طرقبه", "Torqabeh", 36.31, 59.37),
        IranianCity("عبدالله گیو", "Abdollah Givi", 36.69, 58.43),
        IranianCity("عشق آباد", "Eshqabad", 36.0431, 58.6831),
        IranianCity("عمرانی", "Omrani", 34.56, 58.7),
        IranianCity("فریمان", "Fariman", 35.71, 59.85),
        IranianCity("فیروزه", "Firuzeh", 36.2, 58.6),
        IranianCity("فیض آباد", "Feyzabad", 35.02, 58.79),
        IranianCity("قاسم آباد", "Qasemabad", 34.36, 59.87),
        IranianCity("قدمگاه", "Qadamgah", 36.11, 59.06),
        IranianCity("قوچان", "Quchan", 37.11, 58.51),
        IranianCity("کاخک", "Kakhk", 34.15, 58.65),
        IranianCity("کاشمر", "Kashmar", 35.23, 58.46),
        IranianCity("کاهک", "Kahak", 36.35, 56.77),
        IranianCity("کدکن", "Kadkan", 35.61, 58.88),
        IranianCity("کلات نادری", "Kalat Nader", 37.0, 59.77),
        IranianCity("کندر", "Kondor", 35.12, 58.09),
        IranianCity("گرماب", "Garmab", 36.14, 58.25),
        IranianCity("گلبهار", "Golbahar", 36.5729, 59.1709),
        IranianCity("گناباد", "Gonabad", 34.35, 58.69),
        IranianCity("لطف آباد", "Lotfabad", 37.52, 59.34),
        IranianCity("ماروسک", "Marusk", 35.81, 58.58),
        IranianCity("مشهدریزه", "Mashhad Rizeh", 34.7938, 60.5089),
        IranianCity("نقاب", "Neghab", 36.71, 57.42),
        IranianCity("نیازآباد", "Niazabad", 34.23, 60.24),
        IranianCity("نیشابور", "Nishapur", 36.2, 58.8),
        IranianCity("نیل شهر", "Nilshahr", 35.1347, 60.7736),
        IranianCity("همت‌آباد", "Hemmatabad", 36.1609, 58.4522),
        IranianCity("یونسی", "Yunesi", 34.8, 58.44),

        // استان آذربایجان شرقی (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آذرشهر", "Azarshahr", 37.77, 45.97),
        IranianCity("اسکو", "Osku", 37.92, 46.12),
        IranianCity("اهر", "Ahar", 38.47, 47.07),
        IranianCity("بستان آباد", "Bostanabad", 37.84, 46.83),
        IranianCity("بناب", "Bonab", 37.34, 46.07),
        IranianCity("جلفا", "Jolfa", 38.94, 45.63),
        IranianCity("سراب", "Sarab", 37.94, 47.54),
        IranianCity("شبستر", "Shabestar", 38.18, 45.7),
        IranianCity("شهر جدید سهند", "Sahand", 37.944773, 46.121163),
        IranianCity("عجب شیر", "Ajab Shir", 37.48, 45.89),
        IranianCity("کلیبر", "Kaleybar", 38.86, 47.04),
        IranianCity("مراغه", "Maragheh", 37.37, 46.19),
        IranianCity("مرند", "Marand", 38.43, 45.77),
        IranianCity("ملکان", "Malekan", 37.11, 46.11),
        IranianCity("میانه", "Mianeh", 37.44, 47.7),
        IranianCity("هریس", "Heris", 38.25, 47.11),
        IranianCity("هشترود", "Hashtrud", 37.48, 47.05),
        IranianCity("ورزقان", "Varzaqan", 38.49, 46.67),

        // استان آذربایجان غربی (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اشنویه", "Oshnavieh", 37.04, 45.1),
        IranianCity("بوکان", "Bukan", 36.52, 46.21),
        IranianCity("پیرانشهر", "Piranshahr", 36.7, 45.14),
        IranianCity("تکاب", "Takab", 36.4, 47.11),
        IranianCity("چالدران", "Chaldoran", 39.07, 44.38),
        IranianCity("خوی", "Khoy", 38.53, 44.96),
        IranianCity("سردشت", "Sardasht", 36.18, 45.51),
        IranianCity("سلماس", "Salmas", 38.19, 44.77),
        IranianCity("شاهیندژ", "Shahin Dej", 36.68, 46.56),
        IranianCity("ماکو", "Maku", 39.3, 44.51),
        IranianCity("مهاباد", "Mahabad", 36.77, 45.72),
        IranianCity("میاندوآب", "Miandoab", 36.97, 46.1),
        IranianCity("نقده", "Naqadeh", 36.97, 45.39),

        // استان اردبیل (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("بیله سوار", "Bileh Savar", 39.37, 48.36),
        IranianCity("پارس آباد", "Parsabad", 39.65, 47.92),
        IranianCity("خلخال", "Khalkhal", 37.62, 48.53),
        IranianCity("سرعین", "Sareyn", 38.15, 48.07),
        IranianCity("گرمی", "Germi", 39.04, 48.08),
        IranianCity("مشگین شهر", "Meshginshahr", 38.4, 47.68),
        IranianCity("نمین", "Namin", 38.42, 48.49),
        IranianCity("نیر", "Nir", 38.03, 48.0),

        // استان اصفهان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آران و بیدگل", "Aran va Bidgol", 34.05, 51.48),
        IranianCity("تیران", "Tiran", 32.7, 51.15),
        IranianCity("خمینی شهر", "Khomeyni Shahr", 32.68, 51.54),
        IranianCity("خوانسار", "Khansar", 33.21, 50.32),
        IranianCity("داران", "Daran", 32.99, 50.42),
        IranianCity("دولت آباد اصفهان", "Dolatabad", 32.81, 51.69),
        IranianCity("زرین شهر", "Zarrin Shahr", 32.39, 51.38),
        IranianCity("سمیرم", "Semirom", 31.42, 51.57),
        IranianCity("شاهین شهر", "Shahin Shahr", 32.88, 51.54),
        IranianCity("شهرضا", "Shahreza", 32.01, 51.87),
        IranianCity("فلاورجان", "Falavarjan", 32.55, 51.51),
        IranianCity("فولادشهر", "Fuladshahr", 32.49, 51.41),
        IranianCity("کاشان", "Kashan", 33.99, 51.45),
        IranianCity("گلپایگان", "Golpayegan", 33.45, 50.29),
        IranianCity("مبارکه", "Mobarakeh", 32.35, 51.51),
        IranianCity("نائین", "Naein", 32.86, 53.1),
        IranianCity("نجف آباد", "Najafabad", 32.63, 51.37),

        // استان البرز (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اشتهارد", "Eshtehard", 35.72, 50.35),
        IranianCity("چهار باغ", "Charbagh", 35.8369, 50.8467),
        IranianCity("طالقان", "Taleqan", 36.17, 50.77),
        IranianCity("فردیس", "Fardis", 35.7247, 50.9883),
        IranianCity("کمال شهر", "Kamalshahr", 35.8603, 50.8739),
        IranianCity("ماهدشت", "Mahdasht", 35.73, 50.82),
        IranianCity("محمد شهر", "Mohammad Shahr", 35.76, 50.92),
        IranianCity("مشکین دشت", "Meshkindasht", 35.7458, 50.9511),
        IranianCity("نظرآباد", "Nazarabad", 35.96, 50.61),
        IranianCity("هشتگرد", "Hashtgerd", 35.96, 50.69),

        // استان ایلام (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آبدانان", "Abdanan", 32.99, 47.42),
        IranianCity("ارکواز", "Arkovaz", 33.38, 46.6),
        IranianCity("ایوان", "Eyvan", 33.83, 46.31),
        IranianCity("دره شهر", "Darreh Shahr", 33.15, 47.39),
        IranianCity("دهلران", "Dehloran", 32.7, 47.26),
        IranianCity("سرابله", "Sarableh", 33.77, 46.56),
        IranianCity("مهران", "Mehran", 33.12, 46.16),

        // استان بوشهر (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اهرم", "Ahram", 28.88, 51.28),
        IranianCity("برازجان", "Borazjan", 29.27, 51.21),
        IranianCity("بندر دیر", "Dayyer", 27.84, 51.93),
        IranianCity("بندر گناوه", "Bandar Ganaveh", 29.57, 50.52),
        IranianCity("جم", "Jam", 27.824085, 52.327742),
        IranianCity("خورموج", "Khormoj", 28.65, 51.38),
        IranianCity("دیلم", "Deylam", 30.06, 50.17),
        IranianCity("عسلویه", "Asaluyeh", 27.499593, 52.597239),
        IranianCity("کنگان", "Kangan", 27.83, 52.06),

        // استان تهران (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اسلام شهر", "Eslamshahr", 35.56, 51.24),
        IranianCity("اندیشه", "Andisheh", 35.708563, 51.015475),
        IranianCity("باقرشهر", "Baqershahr", 35.535426, 51.401148),
        IranianCity("بومهن", "Bumahen", 35.73, 51.87),
        IranianCity("پاکدشت", "Pakdasht", 35.47, 51.67),
        IranianCity("پردیس", "Pardis", 35.7469, 51.7962),
        IranianCity("پرند", "Parand", 35.4666, 50.983),
        IranianCity("پیشوا", "Pishva", 35.308326, 51.728954),
        IranianCity("دماوند", "Damavand", 35.72, 52.07),
        IranianCity("رباط کریم", "Robat Karim", 35.48, 51.08),
        IranianCity("رودهن", "Rudehen", 35.74, 51.91),
        IranianCity("شهرری", "Shahr-e-Rey", 35.59, 51.43),
        IranianCity("شهریار", "Shahriar", 35.65, 51.06),
        IranianCity("فیروزکوه", "Firuzkuh", 35.76, 52.78),
        IranianCity("قدس", "Qods", 35.72, 51.11),
        IranianCity("قرچک", "Qarchak", 35.44, 51.57),
        IranianCity("گلستان", "Golestan", 35.51, 51.16),
        IranianCity("ملارد", "Malard", 35.65, 51.0),
        IranianCity("نسیم شهر", "Nasimshahr", 35.55, 51.1667),
        IranianCity("ورامین", "Varamin", 35.33, 51.65),

        // استان چهارمحال و بختیاری (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اردل", "Ardal", 32.0, 50.66),
        IranianCity("بروجن", "Borujen", 31.97, 51.28),
        IranianCity("چلگرد", "Chelgard", 32.46, 50.14),
        IranianCity("فارسان", "Farsan", 32.26, 50.57),
        IranianCity("فرخشهر", "Farrokh Shahr", 32.27, 50.98),
        IranianCity("لردگان", "Lordegan", 31.51, 50.82),

        // استان خراسان جنوبی (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اسدیه", "Asadiyeh", 32.92, 60.02),
        IranianCity("بشرویه", "Boshruyeh", 33.87, 57.43),
        IranianCity("سرایان", "Sarayan", 33.86, 58.53),
        IranianCity("سربیشه", "Sarbisheh", 32.57, 59.8),
        IranianCity("طبس", "Tabas", 33.59, 56.92),
        IranianCity("فردوس", "Ferdows", 34.02, 58.17),
        IranianCity("قاین", "Qaen", 33.73, 59.19),
        IranianCity("نهبندان", "Nehbandan", 31.54, 60.04),

        // استان خراسان شمالی (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اسفراین", "Esfarayen", 37.08, 57.51),
        IranianCity("آشخانه", "Ashkhaneh", 37.56, 56.93),
        IranianCity("جاجرم", "Jajarm", 36.97, 56.31),
        IranianCity("راز", "Raz", 37.9335, 57.111),
        IranianCity("شیروان", "Shirvan", 37.4, 57.93),
        IranianCity("فاروج", "Faruj", 37.23, 58.22),
        IranianCity("گرمه", "Garmeh", 36.99, 56.29),

        // استان خوزستان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آبادان", "Abadan", 30.38, 48.29),
        IranianCity("امیدیه", "Omidiyeh", 30.77, 49.71),
        IranianCity("اندیمشک", "Andimeshk", 32.46, 48.35),
        IranianCity("ایذه", "Izeh", 31.84, 49.87),
        IranianCity("باغملک", "Baghmalek", 31.52, 49.89),
        IranianCity("بندر امام", "Bandar Imam Khomeini", 30.44, 49.08),
        IranianCity("بهبهان", "Behbahan", 30.6, 50.24),
        IranianCity("خرمشهر", "Khorramshahr", 30.44, 48.18),
        IranianCity("دزفول", "Dezful", 32.38, 48.4),
        IranianCity("رامهرمز", "Ramhormoz", 31.27, 49.6),
        IranianCity("سوسنگرد", "Susangerd", 31.56, 48.19),
        IranianCity("شادگان", "Shadegan", 30.65, 48.67),
        IranianCity("شوش", "Shush", 32.2, 48.25),
        IranianCity("شوشتر", "Shushtar", 32.05, 48.86),
        IranianCity("گتوند", "Gotvand", 32.17, 48.82),
        IranianCity("ماهشهر", "Mahshahr", 30.56, 49.2),
        IranianCity("مسجدسلیمان", "Masjed Soleyman", 31.93, 49.31),
        IranianCity("هندیجان", "Hendijan", 30.24, 49.66),

        // استان زنجان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آببر", "Abbar", 36.923629, 48.963344),
        IranianCity("ابهر", "Abhar", 36.16, 49.23),
        IranianCity("خرمدره", "Khorramdarreh", 36.25, 49.16),
        IranianCity("قیدار", "Qeydar", 36.12, 48.59),
        IranianCity("ماه نشان", "Mah Neshan", 36.74, 47.66),

        // استان سمنان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("دامغان", "Damghan", 36.17, 54.35),
        IranianCity("سرخه", "Sorkheh", 35.467672, 53.215476),
        IranianCity("شاهرود", "Shahrud", 36.42, 54.96),
        IranianCity("گرمسار", "Garmsar", 35.22, 52.34),
        IranianCity("مهدی شهر", "Mahdi Shahr", 35.7, 53.35),

        // استان سیستان و بلوچستان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("ایرانشهر", "Iranshahr", 27.21, 60.7),
        IranianCity("چابهار", "Chabahar", 25.29, 60.67),
        IranianCity("خاش", "Khash", 28.22, 61.21),
        IranianCity("دوست محمد", "Dust Mohammad", 31.14, 61.79),
        IranianCity("راسک", "Rask", 26.233054, 61.404188),
        IranianCity("زابل", "Zabol", 31.02, 61.49),
        IranianCity("زهک", "Zehak", 30.9, 61.68),
        IranianCity("سراوان", "Saravan", 27.37, 62.34),
        IranianCity("کنارک", "Konarak", 25.36, 60.4),
        IranianCity("میرجاوه", "Mirjaveh", 29.02, 61.45),
        IranianCity("نیک شهر", "Nikshahr", 26.23, 60.22),

        // استان فارس (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آباده", "Abadeh", 31.16, 52.65),
        IranianCity("استهبان", "Estahban", 29.14, 54.0),
        IranianCity("اقلید", "Eqlid", 30.9, 52.69),
        IranianCity("جهرم", "Jahrom", 28.5, 53.55),
        IranianCity("حاجی آباد فارس", "HajiAbad", 28.36, 54.42),
        IranianCity("داراب", "Darab", 28.75, 54.55),
        IranianCity("زرقان", "Zarqan", 29.77, 52.7),
        IranianCity("شهر جدید صدرا", "Sadra", 29.8, 52.4),
        IranianCity("فسا", "Fasa", 28.94, 53.65),
        IranianCity("فیروزآباد فارس", "Firuzabad", 28.84, 52.57),
        IranianCity("کازرون", "Kazerun", 29.62, 51.65),
        IranianCity("گراش", "Gerash", 27.67, 54.14),
        IranianCity("لار", "Lar", 27.66, 54.32),
        IranianCity("لامرد", "Lamerd", 27.34, 53.18),
        IranianCity("مرودشت", "Marvdasht", 29.87, 52.79),
        IranianCity("نورآباد ممسنی", "Nourabad", 30.11, 51.54),
        IranianCity("نی ریز", "Neyriz", 29.2, 54.33),

        // استان قزوین (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آبیک", "Abyek", 36.04, 50.53),
        IranianCity("اقبالیه", "Eqbaliyeh", 36.23, 49.92),
        IranianCity("الوند", "Alvand", 36.187764, 50.04089),
        IranianCity("بوئین زهرا", "Buin Zahra", 35.77, 50.06),
        IranianCity("تاکستان", "Takestan", 36.07, 49.7),
        IranianCity("شهرک محمدیه", "Mohamadie", 36.23, 50.2),

        // استان قم (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("جعفریه", "Jafariyeh", 34.78, 50.51),
        IranianCity("دستجرد", "Dastjerd", 34.530317, 50.234642),
        IranianCity("سلفچگان", "Salafchegan", 34.48, 50.47),
        IranianCity("کهک قم", "Kahak", 34.4, 50.87),

        // استان کردستان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("بانه", "Baneh", 35.99, 45.89),
        IranianCity("بیجار", "Bijar", 35.87, 47.61),
        IranianCity("دهگلان", "Dehgolan", 35.28, 47.42),
        IranianCity("دیواندره", "Divandarreh", 35.91, 47.02),
        IranianCity("سقز", "Saqqez", 36.24, 46.28),
        IranianCity("قروه", "Qorveh", 35.17, 47.8),
        IranianCity("کامیاران", "Kamyaran", 34.8, 46.94),
        IranianCity("مریوان", "Marivan", 35.53, 46.18),

        // استان کرمان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("بافت", "Baft", 29.24, 56.6),
        IranianCity("بردسیر", "Bardsir", 29.93, 56.57),
        IranianCity("بم", "Bam", 29.11, 58.36),
        IranianCity("جیرفت", "Jiroft", 28.67, 57.74),
        IranianCity("رفسنجان", "Rafsanjan", 30.41, 55.99),
        IranianCity("رودبار کرمان", "Rudbar", 28.029429, 57.992726),
        IranianCity("زرند", "Zarand", 30.81, 56.57),
        IranianCity("سیرجان", "Sirjan", 29.45, 55.67),
        IranianCity("شهربابک", "Shahrbabak", 30.12, 55.12),
        IranianCity("عنبرآباد", "Anbarabad", 28.47, 57.85),
        IranianCity("قلعه گنج", "Qaleh Ganj", 27.52, 57.88),
        IranianCity("کهنوج", "Kahnooj", 27.94, 57.7),
        IranianCity("منوجان", "Manujan", 27.41, 57.5),

        // استان کرمانشاه (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اسلام آباد غرب", "Eslamabad-e-Gharb", 34.11, 46.53),
        IranianCity("پاوه", "Paveh", 35.04, 46.37),
        IranianCity("جوانرود", "Javanrud", 34.8, 46.5),
        IranianCity("روانسر", "Ravansar", 34.75, 46.683333),
        IranianCity("سرپل ذهاب", "Sarpol-e Zahab", 34.46, 45.86),
        IranianCity("سنقر", "Sonqor", 34.78, 47.6),
        IranianCity("صحنه", "Sahneh", 34.48, 47.68),
        IranianCity("قصرشیرین", "Qasr-e Shirin", 34.51, 45.57),
        IranianCity("کنگاور", "Kangavar", 34.5, 47.96),
        IranianCity("گیلانغرب", "Gilan-e Gharb", 34.14, 45.92),
        IranianCity("هرسین", "Harsin", 34.27, 47.58),

        // استان کهگیلویه و بویراحمد (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("باشت", "Basht", 30.35, 51.17),
        IranianCity("چرام", "Charam", 30.75, 50.74),
        IranianCity("دهدشت", "Dehdasht", 30.79, 50.57),
        IranianCity("دوگنبدان", "Dogonbadan", 30.36, 50.79),
        IranianCity("سی سخت", "Sisakht", 30.85, 51.46),
        IranianCity("لیکک", "Likak", 30.9, 50.09),

        // استان گلستان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آزادشهر", "Azadshahr", 37.08, 55.18),
        IranianCity("آق قلا", "Aqqala", 37.01, 54.46),
        IranianCity("بندر ترکمن", "Bandar Torkaman", 36.89, 54.07),
        IranianCity("رامیان", "Ramian", 37.02, 55.14),
        IranianCity("علی آباد کتول", "Aliabad-e-Katul", 36.906459, 54.858942),
        IranianCity("کرد کوی", "Kordkuy", 36.8, 54.11),
        IranianCity("کلاله", "Kalaleh", 37.38, 55.49),
        IranianCity("گالیکش", "Galikesh", 37.27, 55.43),
        IranianCity("گنبد کاووس", "Gonbad-e Kavus", 37.25, 55.17),
        IranianCity("مینودشت", "Minudasht", 37.23, 55.36),

        // استان گیلان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آستارا", "Astara", 38.44, 48.87),
        IranianCity("آستانه اشرفیه", "Astaneh-ye Ashrafiyeh", 37.26, 49.94),
        IranianCity("بندر انزلی", "Bandar Anzali", 37.47, 49.46),
        IranianCity("تالش", "Talesh", 37.8, 48.9),
        IranianCity("رضوانشهر", "Rezvanshahr", 37.55, 49.14),
        IranianCity("رودبار گیلان", "Rudbar", 36.81, 49.41),
        IranianCity("رودسر", "Rudsar", 37.13, 50.29),
        IranianCity("سیاهکل", "Siahkal", 37.15, 49.87),
        IranianCity("صومعه سرا", "Someh Sara", 37.3, 49.31),
        IranianCity("فومن", "Fuman", 37.22, 49.31),
        IranianCity("لاهیجان", "Lahijan", 37.21, 50.0),
        IranianCity("لنگرود", "Langarud", 37.19, 50.15),
        IranianCity("ماسال", "Masal", 37.36, 49.13),

        // استان لرستان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("ازنا", "Azna", 33.46, 49.46),
        IranianCity("الشتر", "Aleshtar", 33.86, 48.26),
        IranianCity("الیگودرز", "Aligudarz", 33.4, 49.69),
        IranianCity("بروجرد", "Borujerd", 33.89, 48.76),
        IranianCity("پلدختر", "Pol-e Dokhtar", 33.15, 47.71),
        IranianCity("دورود", "Dorud", 33.48, 49.06),
        IranianCity("کوهدشت", "Kuhdasht", 33.53, 47.61),
        IranianCity("نورآباد لرستان", "Nurabad", 34.08, 47.97),

        // استان مازندران (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آمل", "Amol", 36.47, 52.35),
        IranianCity("بابل", "Babol", 36.54, 52.68),
        IranianCity("بابلسر", "Babolsar", 36.7, 52.65),
        IranianCity("بهشهر", "Behshahr", 36.69, 53.54),
        IranianCity("تنکابن", "Tonekabon", 36.82, 50.88),
        IranianCity("جویبار", "Juybar", 36.65, 52.91),
        IranianCity("چالوس", "Chalus", 36.65, 51.42),
        IranianCity("رامسر", "Ramsar", 36.92, 50.64),
        IranianCity("فریدونکنار", "Fereydunkenar", 36.68, 52.51),
        IranianCity("قائمشهر", "Qaemshahr", 36.46, 52.86),
        IranianCity("محمودآباد مازندران", "Mahmudabad", 36.63, 52.26),
        IranianCity("نکا", "Neka", 36.65, 53.3),
        IranianCity("نور", "Nur", 36.58, 52.01),
        IranianCity("نوشهر", "Nowshahr", 36.65, 51.4833),

        // استان مرکزی (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("آشتیان", "Ashtian", 34.53, 50.0),
        IranianCity("تفرش", "Tafresh", 34.69, 50.02),
        IranianCity("خمین", "Khomeyn", 33.64, 50.08),
        IranianCity("دلیجان", "Delijan", 33.99, 50.68),
        IranianCity("ساوه", "Saveh", 35.03, 50.37),
        IranianCity("شازند", "Shazand", 33.93, 49.41),
        IranianCity("کمیجان", "Komijan", 34.72, 49.32),
        IranianCity("مامونیه", "Mamuniyeh", 35.38, 50.58),
        IranianCity("محلات", "Mahallat", 33.91, 50.45),

        // استان هرمزگان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("بستک", "Bastak", 27.2, 54.36),
        IranianCity("بندر خمیر", "Bandar Khamir", 26.95, 55.59),
        IranianCity("بندر لنگه", "Bandar Lengeh", 26.55, 54.88),
        IranianCity("پارسیان", "Parsian", 27.21, 53.03),
        IranianCity("جاسک", "Jask", 25.65, 57.78),
        IranianCity("حاجی آباد هرمزگان", "Haji Abad", 28.31, 55.89),
        IranianCity("درگهان", "Dargahan", 26.961888, 56.084148),
        IranianCity("رودان", "Rudan", 27.61, 57.19),
        IranianCity("قشم", "Qeshm", 26.8676, 56.0337),
        IranianCity("کیش", "Kish", 26.53, 53.98),
        IranianCity("میناب", "Minab", 27.15, 57.08),

        // استان همدان (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("اسدآباد", "Asadabad", 34.78, 48.12),
        IranianCity("بهار", "Bahar", 34.9, 48.45),
        IranianCity("تویسرکان", "Tuyserkan", 34.55, 48.45),
        IranianCity("رزن", "Razan", 35.38, 49.03),
        IranianCity("فامنین", "Famenin", 35.116667, 48.966667),
        IranianCity("کبودرآهنگ", "Kabudrahang", 35.21, 48.72),
        IranianCity("ملایر", "Malayer", 34.29, 48.82),
        IranianCity("نهاوند", "Nahavand", 34.19, 48.37),

        // استان یزد (افزوده‌شده از فهرست شهرهای بادصبا، ۲۰۲۶-۱۱-۱۴)
        IranianCity("ابرکوه", "Abarkuh", 31.13, 53.29),
        IranianCity("اردکان", "Ardakan", 32.31, 54.02),
        IranianCity("اشکذر", "Ashkezar", 32.01, 54.23),
        IranianCity("بافق", "Bafq", 31.6, 55.41),
        IranianCity("تفت", "Taft", 31.74, 54.2),
        IranianCity("مهریز", "Mehriz", 31.58, 54.48),
        IranianCity("میبد", "Meybod", 32.22, 54.01),
        IranianCity("هرات یزد", "Harat", 30.08, 54.36),
    )

    fun getCityByName(name: String): IranianCity {
        return CITIES.find { it.nameFa == name || it.nameEn.equals(name, ignoreCase = true) }
            ?: CITIES.first() // Default to Tehran
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
