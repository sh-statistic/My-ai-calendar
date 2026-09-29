/**
 * Hamgam - Ultra-Lightweight 100% Offline Prayer Times Engine
 * Calculation Method: Institute of Geophysics, University of Tehran (Fajr 17.7°, Maghrib 4.5°)
 * Pure Vanilla JavaScript - Zero Dependencies
 */

const IRANIAN_CITIES = [
  { nameFa: "تهران", nameEn: "Tehran", lat: 35.6892, lng: 51.3890 },
  { nameFa: "مشهد", nameEn: "Mashhad", lat: 36.2972, lng: 59.6067 },
  { nameFa: "اصفهان", nameEn: "Isfahan", lat: 32.6546, lng: 51.6680 },
  { nameFa: "شیراز", nameEn: "Shiraz", lat: 29.5918, lng: 52.5837 },
  { nameFa: "تبریز", nameEn: "Tabriz", lat: 38.0800, lng: 46.2919 },
  { nameFa: "کرج", nameEn: "Karaj", lat: 35.8400, lng: 50.9391 },
  { nameFa: "قم", nameEn: "Qom", lat: 34.6401, lng: 50.8764 },
  { nameFa: "اهواز", nameEn: "Ahvaz", lat: 31.3183, lng: 48.6706 },
  { nameFa: "کرمانشاه", nameEn: "Kermanshah", lat: 34.3142, lng: 47.0650 },
  { nameFa: "ارومیه", nameEn: "Urmia", lat: 37.5527, lng: 45.0761 },
  { nameFa: "رشت", nameEn: "Rasht", lat: 37.2808, lng: 49.5832 },
  { nameFa: "زاهدان", nameEn: "Zahedan", lat: 29.4963, lng: 60.8629 },
  { nameFa: "همدان", nameEn: "Hamadan", lat: 34.7989, lng: 48.5150 },
  { nameFa: "کرمان", nameEn: "Kerman", lat: 30.2839, lng: 57.0788 },
  { nameFa: "یزد", nameEn: "Yazd", lat: 31.8974, lng: 54.3569 },
  { nameFa: "اردبیل", nameEn: "Ardabil", lat: 38.2498, lng: 48.2933 },
  { nameFa: "بندرعباس", nameEn: "Bandar Abbas", lat: 27.1832, lng: 56.2666 },
  { nameFa: "اراک", nameEn: "Arak", lat: 34.0954, lng: 49.7013 },
  { nameFa: "زنجان", nameEn: "Zanjan", lat: 36.6736, lng: 48.4787 },
  { nameFa: "سنندج", nameEn: "Sanandaj", lat: 35.3219, lng: 46.9862 },
  { nameFa: "قزوین", nameEn: "Qazvin", lat: 36.2797, lng: 50.0049 },
  { nameFa: "خرم‌آباد", nameEn: "Khorramabad", lat: 33.4878, lng: 48.3558 },
  { nameFa: "گرگان", nameEn: "Gorgan", lat: 36.8427, lng: 54.4347 },
  { nameFa: "ساری", nameEn: "Sari", lat: 36.5659, lng: 53.0586 },
  { nameFa: "بوشهر", nameEn: "Bushehr", lat: 28.9234, lng: 50.8203 },
  { nameFa: "بجنورد", nameEn: "Bojnord", lat: 37.4747, lng: 57.3290 },
  { nameFa: "بیرجند", nameEn: "Birjand", lat: 32.8663, lng: 59.2211 },
  { nameFa: "ایلام", nameEn: "Ilam", lat: 33.6374, lng: 46.4227 },
  { nameFa: "شهرکرد", nameEn: "Shahrekord", lat: 32.3256, lng: 50.8644 },
  { nameFa: "سمنان", nameEn: "Semnan", lat: 35.5729, lng: 53.3971 },
  { nameFa: "یاسوج", nameEn: "Yasuj", lat: 30.6684, lng: 51.5876 }
];

const PrayerTimesEngine = {
  FAJR_ANGLE: 17.7,
  MAGHRIB_ANGLE: 4.5,
  SUNRISE_SUNSET_ANGLE: 0.8333,

  d2r(d) { return (d * Math.PI) / 180.0; },
  r2d(r) { return (r * 180.0) / Math.PI; },

  fixHour(h) {
    let res = h - 24.0 * Math.floor(h / 24.0);
    return res < 0 ? res + 24.0 : res;
  },

  fixAngle(a) {
    let res = a - 360.0 * Math.floor(a / 360.0);
    return res < 0 ? res + 360.0 : res;
  },

  julianDate(year, month, day) {
    let y = year;
    let m = month;
    if (m <= 2) {
      y -= 1;
      m += 12;
    }
    const a = Math.floor(y / 100.0);
    const b = 2.0 - a + Math.floor(a / 4.0);
    return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + b - 1524.5;
  },

  sunPosition(jd) {
    const d = jd - 2451545.0;
    const g = this.fixAngle(357.529 + 0.98560028 * d);
    const q = this.fixAngle(280.459 + 0.98564736 * d);
    const l = this.fixAngle(q + 1.915 * Math.sin(this.d2r(g)) + 0.020 * Math.sin(this.d2r(2 * g)));
    const e = 23.439 - 0.00000036 * d;
    const ra = this.r2d(Math.atan2(Math.cos(this.d2r(e)) * Math.sin(this.d2r(l)), Math.cos(this.d2r(l)))) / 15.0;
    const delta = this.r2d(Math.asin(Math.sin(this.d2r(e)) * Math.sin(this.d2r(l))));
    return { ra: this.fixHour(ra), delta: delta };
  },

  equationOfTime(jd) {
    const d = jd - 2451545.0;
    const g = this.fixAngle(357.529 + 0.98560028 * d);
    const q = this.fixAngle(280.459 + 0.98564736 * d);
    const l = this.fixAngle(q + 1.915 * Math.sin(this.d2r(g)) + 0.020 * Math.sin(this.d2r(2 * g)));
    const e = 23.439 - 0.00000036 * d;
    const ra = this.r2d(Math.atan2(Math.cos(this.d2r(e)) * Math.sin(this.d2r(l)), Math.cos(this.d2r(l))));
    return (q - this.fixAngle(ra)) / 15.0;
  },

  sunHourAngle(angle, lat, dec, isMorning) {
    const latRad = this.d2r(lat);
    const decRad = this.d2r(dec);
    const top = -Math.sin(this.d2r(angle)) - Math.sin(latRad) * Math.sin(decRad);
    const bottom = Math.cos(latRad) * Math.cos(decRad);
    const cosH = top / bottom;
    if (cosH > 1.0 || cosH < -1.0) return 0.0;
    const h = this.r2d(Math.acos(cosH)) / 15.0;
    return isMorning ? -h : h;
  },

  formatTime(hourFraction) {
    const fixed = this.fixHour(hourFraction);
    const totalMinutes = Math.round(fixed * 60.0);
    const hours = Math.floor(totalMinutes / 60) % 24;
    const minutes = totalMinutes % 60;
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
  },

  calculate(lat, lng, date = new Date(), tzOffsetHours = 3.5) {
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const day = date.getDate();

    const jd = this.julianDate(year, month, day);
    const eqt = this.equationOfTime(jd);
    const { delta } = this.sunPosition(jd);

    const dhuhr = this.fixHour(12.0 + tzOffsetHours - (lng / 15.0) - eqt);
    const fajr = this.fixHour(dhuhr + this.sunHourAngle(this.FAJR_ANGLE, lat, delta, true));
    const sunrise = this.fixHour(dhuhr + this.sunHourAngle(this.SUNRISE_SUNSET_ANGLE, lat, delta, true));
    const sunset = this.fixHour(dhuhr + this.sunHourAngle(this.SUNRISE_SUNSET_ANGLE, lat, delta, false));
    const maghrib = this.fixHour(dhuhr + this.sunHourAngle(this.MAGHRIB_ANGLE, lat, delta, false));
    const midnight = this.fixHour((sunset + fajr + (fajr < sunset ? 24.0 : 0.0)) / 2.0);

    return {
      fajr: this.formatTime(fajr),
      sunrise: this.formatTime(sunrise),
      dhuhr: this.formatTime(dhuhr),
      sunset: this.formatTime(sunset),
      maghrib: this.formatTime(maghrib),
      midnight: this.formatTime(midnight),
      raw: { fajr, sunrise, dhuhr, sunset, maghrib, midnight }
    };
  },

  getNextPrayer(times, now = new Date()) {
    const curMinutes = now.getHours() * 60 + now.getMinutes();
    const prayers = [
      { name: "اذان صبح", time: times.fajr, minutes: Math.round(times.raw.fajr * 60) },
      { name: "طلوع آفتاب", time: times.sunrise, minutes: Math.round(times.raw.sunrise * 60) },
      { name: "اذان ظهر", time: times.dhuhr, minutes: Math.round(times.raw.dhuhr * 60) },
      { name: "غروب آفتاب", time: times.sunset, minutes: Math.round(times.raw.sunset * 60) },
      { name: "اذان مغرب", time: times.maghrib, minutes: Math.round(times.raw.maghrib * 60) },
      { name: "نیمه‌شب شرعی", time: times.midnight, minutes: Math.round(times.raw.midnight * 60) }
    ];

    for (const p of prayers) {
      const diff = p.minutes - curMinutes;
      if (diff > 0) {
        return { name: p.name, time: p.time, remainingMinutes: diff };
      }
    }

    const tomorrowFajr = Math.round(times.raw.fajr * 60) + 1440;
    return { name: "اذان صبح (فردا)", time: times.fajr, remainingMinutes: tomorrowFajr - curMinutes };
  }
};

if (typeof module !== 'undefined' && module.exports) {
  module.exports = { PrayerTimesEngine, IRANIAN_CITIES };
}
