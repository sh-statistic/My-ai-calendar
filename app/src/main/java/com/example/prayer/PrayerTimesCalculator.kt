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
        IranianCity("یونسی", "Yunesi", 34.8, 58.44)
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
