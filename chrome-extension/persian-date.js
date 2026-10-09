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

  gregorianToJalali(gYear, gMonth, gDay) {
    const gDaysInMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
    const jDaysInMonth = [31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29];
    const gy = gYear - 1600;
    const gm = gMonth - 1;
    const gd = gDay - 1;

    let gDayNo = 365 * gy + Math.floor((gy + 3) / 4) - Math.floor((gy + 99) / 100) + Math.floor((gy + 399) / 400);
    for (let i = 0; i < gm; i++) gDayNo += gDaysInMonth[i];
    if (gm > 1 && ((gy % 4 === 0 && gy % 100 !== 0) || (gy % 400 === 0))) gDayNo++;
    gDayNo += gd;

    let jDayNo = gDayNo - 79;
    const jNp = Math.floor(jDayNo / 12053);
    jDayNo %= 12053;

    let jy = 979 + 33 * jNp + 4 * Math.floor(jDayNo / 1461);
    jDayNo %= 1461;

    if (jDayNo >= 366) {
      jy += Math.floor((jDayNo - 1) / 365);
      jDayNo = (jDayNo - 1) % 365;
    }

    let jm = 0;
    while (jm < 11 && jDayNo >= jDaysInMonth[jm]) {
      jDayNo -= jDaysInMonth[jm];
      jm++;
    }
    return { year: jy, month: jm + 1, day: jDayNo + 1 };
  },

  jalaliToGregorian(jYear, jMonth, jDay) {
    const gDaysInMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
    const jDaysInMonth = [31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29];
    const jy = jYear - 979;
    const jm = jMonth - 1;
    const jd = jDay - 1;

    let jDayNo = 365 * jy + Math.floor(jy / 33) * 8 + Math.floor(((jy % 33) + 3) / 4);
    for (let i = 0; i < jm; i++) jDayNo += jDaysInMonth[i];
    jDayNo += jd;

    let gDayNo = jDayNo + 79;
    let gy = 1600 + 400 * Math.floor(gDayNo / 146097);
    gDayNo %= 146097;

    let leap = true;
    if (gDayNo >= 36525) {
      gDayNo--;
      gy += 100 * Math.floor(gDayNo / 36524);
      gDayNo %= 36524;
      if (gDayNo >= 365) gDayNo++;
      else leap = false;
    }

    gy += 4 * Math.floor(gDayNo / 1461);
    gDayNo %= 1461;

    if (gDayNo >= 366) {
      leap = false;
      gDayNo--;
      gy += Math.floor(gDayNo / 365);
      gDayNo %= 365;
    }

    let gm = 0;
    while (gm < 11) {
      let days = gDaysInMonth[gm];
      if (gm === 1 && leap) days++;
      if (gDayNo >= days) {
        gDayNo -= days;
        gm++;
      } else break;
    }
    return { year: gy, month: gm + 1, day: gDayNo + 1 };
  },

  getDaysInJalaliMonth(year, month) {
    if (month >= 1 && month <= 6) return 31;
    if (month >= 7 && month <= 11) return 30;
    if (month === 12) {
      return this.isJalaliLeapYear(year) ? 30 : 29;
    }
    return 30;
  },

  OFFICIAL_LEAP_YEARS: new Set([
    1210, 1214, 1218, 1222, 1226, 1230, 1234, 1238, 1243, 1247, 1251, 1255, 1259, 1263, 1267, 1271, 1276, 1280, 1284, 1288, 1292, 1296, 1300, 1304, 1309, 1313, 1317, 1321, 1325, 1329, 1333, 1337, 1342, 1346, 1350, 1354, 1358, 1362, 1366, 1370, 1375, 1379, 1383, 1387, 1391, 1395, 1399, 1403, 1408, 1412, 1416, 1420, 1424, 1428, 1432, 1436, 1441, 1445, 1449, 1453, 1457, 1461, 1465, 1469, 1474, 1478, 1482, 1486, 1490, 1494, 1498
  ]),

  isJalaliLeapYear(year) {
    if (year >= 1206 && year <= 1498) {
      return this.OFFICIAL_LEAP_YEARS.has(year);
    }
    const a = year - 474;
    const b = ((a % 2820) + 2820) % 2820 + 474;
    return ((b + 38) * 682) % 2816 < 682;
  },

  normalizePersianDate(dateStr) {
    if (!dateStr || typeof dateStr !== "string") return "";
    const parts = dateStr.trim().split("/");
    if (parts.length === 3) {
      const y = parts[0].padStart(4, "0");
      const m = parts[1].padStart(2, "0");
      const d = parts[2].padStart(2, "0");
      return `${y}/${m}/${d}`;
    }
    return dateStr.trim();
  },

  getPersianDayOfWeek(jYear, jMonth, jDay) {
    const g = this.jalaliToGregorian(jYear, jMonth, jDay);
    const d = new Date(g.year, g.month - 1, g.day);
    return (d.getDay() + 1) % 7;
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
