/**
 * Persian (Solar Hijri / Jalali) Calendar Helper for Chrome Extension
 * 100% Client-side mathematical conversion without any external libraries
 */

const PersianDateUtil = {
  PERSIAN_MONTHS: [
    "فروردین", "اردیبهشت", "خرداد",
    "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر",
    "دی", "بهمن", "اسفند"
  ],

  GREGORIAN_MONTHS: [
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
  ],

  WEEKDAYS: [
    "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
  ],

  WEEKDAY_ABBR: ["ش", "ی", "د", "س", "چ", "پ", "ج"],

  toPersianDigits(str) {
    if (str === null || str === undefined) return "";
    const persianNums = ["۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹"];
    return String(str).replace(/[0-9]/g, (w) => persianNums[+w]);
  },

  gregorianToJdn(year, month, day) {
    const a = Math.floor((14 - month) / 12);
    const y = year + 4800 - a;
    const m = month + 12 * a - 3;
    return day + Math.floor((153 * m + 2) / 5) + 365 * y + Math.floor(y / 4) - Math.floor(y / 100) + Math.floor(y / 400) - 32045;
  },

  jdnToGregorian(jdn) {
    const a = jdn + 32044;
    const b = Math.floor((4 * a + 3) / 146097);
    const c = a - Math.floor((146097 * b) / 4);
    const d = Math.floor((4 * c + 3) / 1461);
    const e = c - Math.floor((1461 * d) / 4);
    const m = Math.floor((5 * e + 2) / 153);
    const day = e - Math.floor((153 * m + 2) / 5) + 1;
    const month = m + 3 - 12 * Math.floor(m / 10);
    const year = 100 * b + d - 4800 + Math.floor(m / 10);
    return { year, month, day };
  },

  jalaliToJdn(year, month, day) {
    const epBase = year - 474;
    const epYear = 474 + ((epBase % 2820) + 2820) % 2820;
    const md = month <= 7 ? (month - 1) * 31 : (month - 1) * 30 + 6;
    return day + md + Math.floor(((epYear * 682) - 110) / 2816) + (epYear - 1) * 365 + Math.floor(epBase / 2820) * 1029983 + (1948320 - 1);
  },

  jdnToJalali(jdn) {
    const dep = jdn - 2121445;
    const cycle = Math.floor(dep / 1029983);
    const cDay = ((dep % 1029983) + 1029983) % 1029983;
    let yCycle = 0;
    if (cDay === 1029982) {
      yCycle = 2820;
    } else {
      const aux1 = Math.floor(cDay / 366);
      const aux2 = Math.floor(cDay % 366);
      yCycle = Math.floor((2134 * aux1 + 2816 * aux2 + 2815) / 1028522) + aux1 + 1;
    }
    const year = yCycle + 2820 * cycle + 474;
    const dayOfYear = jdn - this.jalaliToJdn(year, 1, 1) + 1;
    let month, day;
    if (dayOfYear <= 186) {
      month = Math.ceil(dayOfYear / 31);
      const rem = dayOfYear % 31;
      day = rem === 0 ? 31 : rem;
    } else {
      const remOfYear = dayOfYear - 186;
      month = 6 + Math.ceil(remOfYear / 30);
      const rem = remOfYear % 30;
      day = rem === 0 ? 30 : rem;
    }
    return { year, month, day };
  },

  gregorianToJalali(gYear, gMonth, gDay) {
    const jdn = this.gregorianToJdn(gYear, gMonth, gDay);
    return this.jdnToJalali(jdn);
  },

  jalaliToGregorian(jYear, jMonth, jDay) {
    const jdn = this.jalaliToJdn(jYear, jMonth, jDay);
    return this.jdnToGregorian(jdn);
  },

  getDaysInJalaliMonth(year, month) {
    if (month >= 1 && month <= 6) return 31;
    if (month >= 7 && month <= 11) return 30;
    if (month === 12) {
      return this.isJalaliLeapYear(year) ? 30 : 29;
    }
    return 30;
  },

  isJalaliLeapYear(year) {
    const a = year - 474;
    const b = ((a % 2820) + 2820) % 2820 + 474;
    return ((b + 38) * 682) % 2816 < 682;
  },

  getPersianDayOfWeek(jYear, jMonth, jDay) {
    const jdn = this.jalaliToJdn(jYear, jMonth, jDay);
    const dow = (jdn + 2) % 7;
    return dow < 0 ? dow + 7 : dow;
  },

  formatJalali(jDate) {
    const y = String(jDate.year).padStart(4, "0");
    const m = String(jDate.month).padStart(2, "0");
    const d = String(jDate.day).padStart(2, "0");
    return `${y}/${m}/${d}`;
  },

  formatGregorian(gDate) {
    const y = String(gDate.year).padStart(4, "0");
    const m = String(gDate.month).padStart(2, "0");
    const d = String(gDate.day).padStart(2, "0");
    return `${y}-${m}-${d}`;
  },

  getTodayJalali() {
    const now = new Date();
    return this.gregorianToJalali(now.getFullYear(), now.getMonth() + 1, now.getDate());
  },

  getTodayGregorian() {
    const now = new Date();
    return { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate() };
  }
};

if (typeof module !== "undefined" && module.exports) {
  module.exports = PersianDateUtil;
}
