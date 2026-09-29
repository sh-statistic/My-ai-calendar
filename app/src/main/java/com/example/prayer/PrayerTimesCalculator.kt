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
        IranianCity("یاسوج", "Yasuj", 30.6684, 51.5876)
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
