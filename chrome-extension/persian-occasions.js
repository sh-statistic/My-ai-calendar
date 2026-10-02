/**
 * Persian Calendar Occasions and Official Iranian Holidays Engine
 * Ported directly from PersianOccasionsHelper.kt
 */

const PersianOccasions = {
  officialDataCache: {},

  async loadOfficialData(year) {
    if (this.officialDataCache[year]) return;
    try {
      const url = chrome.runtime?.getURL ? chrome.runtime.getURL(`occasions_${year}.json`) : `occasions_${year}.json`;
      const res = await fetch(url);
      if (res.ok) {
        this.officialDataCache[year] = await res.json();
      }
    } catch (e) {
      // JSON not found or error parsing
    }
  },

  SOLAR_OCCASIONS: {
    // فروردین
    "1-1": [{ title: "جشن نوروز / آغاز سال نو هجری شمسی", isHoliday: true, isNational: true }],
    "1-2": [{ title: "عید نوروز", isHoliday: true, isNational: true }],
    "1-3": [{ title: "عید نوروز", isHoliday: true, isNational: true }],
    "1-4": [{ title: "عید نوروز", isHoliday: true, isNational: true }],
    "1-6": [{ title: "روز امید / زادروز زرتشت پیامبر", isHoliday: false, isNational: true }],
    "1-12": [{ title: "روز جمهوری اسلامی ایران", isHoliday: true, isNational: true }],
    "1-13": [{ title: "روز طبیعت (سیزده‌به‌در)", isHoliday: true, isNational: true }],
    "1-18": [{ title: "روز سلامت و روز جهانی بهداشت", isHoliday: false }],
    "1-25": [{ title: "روز بزرگداشت عطار نیشابوری", isHoliday: false, isNational: true }],
    "1-29": [{ title: "روز ارتش جمهوری اسلامی ایران", isHoliday: false }],

    // اردیبهشت
    "2-1": [{ title: "روز بزرگداشت سعدی شیرازی", isHoliday: false, isNational: true }],
    "2-2": [{ title: "جشن گیاه‌آوری / روز زمین پاک", isHoliday: false }],
    "2-3": [{ title: "روز بزرگداشت شیخ بهایی / روز معماری", isHoliday: false }],
    "2-9": [{ title: "روز شوراها / روز روان‌شناس و مشاور", isHoliday: false }],
    "2-10": [{ title: "روز ملی خلیج فارس", isHoliday: false, isNational: true }],
    "2-11": [{ title: "روز جهانی کار و کارگر", isHoliday: false }],
    "2-12": [{ title: "روز معلم / شهادت استاد مرتضی مطهری", isHoliday: false }],
    "2-15": [{ title: "جشن بهاربد / روز شیراز / روز جهانی ماما", isHoliday: false }],
    "2-24": [{ title: "لغو امتیاز تنباکو به فتوای آیت‌الله میرزای شیرازی", isHoliday: false }],
    "2-25": [{ title: "روز بزرگداشت فردوسی و پاسداشت زبان فارسی", isHoliday: false, isNational: true }],
    "2-28": [{ title: "روز بزرگداشت حکیم عمر خیام نیشابوری", isHoliday: false, isNational: true }],

    // خرداد
    "3-1": [{ title: "روز بزرگداشت ملاصدرا / روز بهره‌وری", isHoliday: false }],
    "3-3": [{ title: "روز آزادسازی خرمشهر / روز مقاومت و ایثار", isHoliday: false, isNational: true }],
    "3-14": [{ title: "رحلت حضرت امام خمینی (ره)", isHoliday: true, isNational: true }],
    "3-15": [{ title: "قیام خونین ۱۵ خرداد", isHoliday: true, isNational: true }],
    "3-20": [{ title: "روز ملی فرش دستباف / روز جهانی صنایع دستی", isHoliday: false }],
    "3-27": [{ title: "روز جهاد کشاورزی / روز جهانی بیابان‌زدایی", isHoliday: false }],

    // تیر
    "4-1": [{ title: "جشن آب‌پاشونک (آغاز تابستان) / روز اصناف", isHoliday: false, isNational: true }],
    "4-7": [{ title: "شهادت دکتر بهشتی و ۷۲ تن از یاران / روز قوه قضائیه", isHoliday: false }],
    "4-8": [{ title: "روز مبارزه با سلاح‌های شیمیایی و میکروبی", isHoliday: false }],
    "4-10": [{ title: "روز صنعت و معدن", isHoliday: false }],
    "4-13": [{ title: "جشن تیرگان / روز ملی دماوند", isHoliday: false, isNational: true }],
    "4-14": [{ title: "روز قلم", isHoliday: false }],
    "4-25": [{ title: "روز بهزیستی و تامین اجتماعی", isHoliday: false }],

    // مرداد
    "5-6": [{ title: "روز ترویج آموزش‌های فنی و حرفه‌ای", isHoliday: false }],
    "5-8": [{ title: "روز بزرگداشت شیخ شهاب‌الدین سهروردی (شیخ اشراق)", isHoliday: false }],
    "5-14": [{ title: "صدور فرمان مشروطیت", isHoliday: false, isNational: true }],
    "5-17": [{ title: "روز خبرنگار", isHoliday: false }],
    "5-26": [{ title: "آغاز بازگشت آزادگان سرافراز به میهن", isHoliday: false, isNational: true }],
    "5-28": [{ title: "کودتای ۲۸ مرداد / سالروز آتش‌سوزی سینما رکس آبادان", isHoliday: false }],

    // شهریور
    "6-1": [{ title: "روز بزرگداشت ابوعلی سینا / روز پزشک", isHoliday: false, isNational: true }],
    "6-2": [{ title: "آغاز هفته دولت", isHoliday: false }],
    "6-4": [{ title: "جشن شهریورگان / روز کارمند", isHoliday: false }],
    "6-5": [{ title: "روز بزرگداشت زکریای رازی / روز داروساز", isHoliday: false, isNational: true }],
    "6-8": [{ title: "روز مبارزه با تروریسم", isHoliday: false }],
    "6-13": [{ title: "روز بزرگداشت ابوریحان بیرونی / روز علوم پایه", isHoliday: false }],
    "6-21": [{ title: "روز سینما", isHoliday: false }],
    "6-27": [{ title: "روز شعر و ادب فارسی / روز بزرگداشت استاد شهریار", isHoliday: false, isNational: true }],
    "6-31": [{ title: "آغاز هفته دفاع مقدس", isHoliday: false, isNational: true }],

    // مهر
    "7-1": [{ title: "آغاز سال تحصیلی جدید / بازگشایی مدارس", isHoliday: false }],
    "7-7": [{ title: "روز آتش‌نشانی و ایمنی / بزرگداشت شمس تبریزی", isHoliday: false }],
    "7-8": [{ title: "روز بزرگداشت مولوی (جلال‌الدین محمد بلخی)", isHoliday: false, isNational: true }],
    "7-10": [{ title: "جشن مهرگان", isHoliday: false, isNational: true }],
    "7-13": [{ title: "روز نیروی انتظامی / روز جهانی معلم", isHoliday: false }],
    "7-14": [{ title: "روز دامپزشکی", isHoliday: false }],
    "7-20": [{ title: "روز بزرگداشت حافظ شیرازی", isHoliday: false, isNational: true }],
    "7-26": [{ title: "روز تربیت‌بدنی و ورزش", isHoliday: false }],

    // آبان
    "8-1": [{ title: "روز بزرگداشت ابوالفضل بیهقی / روز آمار و برنامه‌ریزی", isHoliday: false }],
    "8-7": [{ title: "روز بزرگداشت کوروش بزرگ", isHoliday: false, isNational: true }],
    "8-8": [{ title: "شهادت حسین فهمیده / روز نوجوان", isHoliday: false }],
    "8-10": [{ title: "جشن آبانگان", isHoliday: false, isNational: true }],
    "8-13": [{ title: "روز دانش‌آموز / تسخیر لانه جاسوسی", isHoliday: false }],
    "8-24": [{ title: "روز کتاب و کتابخوانی / بزرگداشت علامه طباطبایی", isHoliday: false }],

    // آذر
    "9-5": [{ title: "روز بسیج مستضعفان", isHoliday: false }],
    "9-9": [{ title: "جشن آذرگان", isHoliday: false, isNational: true }],
    "9-10": [{ title: "روز مجلس (شهادت آیت‌الله مدرس)", isHoliday: false }],
    "9-16": [{ title: "روز دانشجو", isHoliday: false, isNational: true }],
    "9-25": [{ title: "روز پژوهش", isHoliday: false }],
    "9-30": [{ title: "شب یلدا (طولانی‌ترین شب سال / جشن شب چله)", isHoliday: false, isNational: true }],

    // دی
    "10-1": [{ title: "جشن خرم‌روز (نخستین جشن دی‌گان)", isHoliday: false, isNational: true }],
    "10-5": [{ title: "سالروز زلزله بم / روز ایمنی در برابر زلزله", isHoliday: false }],
    "10-13": [{ title: "شهادت سردار سپهبد قاسم سلیمانی", isHoliday: false }],
    "10-19": [{ title: "قیام خونین مردم قم در سال ۱۳۵۶", isHoliday: false }],
    "10-29": [{ title: "روز ملی هوای پاک / روز غزه", isHoliday: false }],

    // بهمن
    "11-1": [{ title: "زادروز حکیم ابوالقاسم فردوسی", isHoliday: false, isNational: true }],
    "11-12": [{ title: "بازگشت امام خمینی (ره) به میهن / آغاز دهه فجر", isHoliday: false }],
    "11-19": [{ title: "روز نیروی هوایی", isHoliday: false }],
    "11-22": [{ title: "پیروزی شکوهمند انقلاب اسلامی ایران", isHoliday: true, isNational: true }],
    "11-29": [{ title: "جشن سپندارمذگان (روز عشق ایرانی و پاسداشت بانوان)", isHoliday: false, isNational: true }],

    // اسفند
    "12-5": [{ title: "روز بزرگداشت خواجه نصیرالدین طوسی / روز مهندس", isHoliday: false, isNational: true }],
    "12-14": [{ title: "روز احسان و نیکوکاری", isHoliday: false }],
    "12-15": [{ title: "روز درختکاری / آغاز هفته منابع طبیعی", isHoliday: false }],
    "12-25": [{ title: "پایان سرایش شاهنامه فردوسی / بزرگداشت پروین اعتصامی", isHoliday: false, isNational: true }],
    "12-29": [{ title: "روز ملی شدن صنعت نفت ایران", isHoliday: true, isNational: true }]
  },

  // ========== محاسبه خودکار مناسبت‌های قمری (Hijri ↔ Jalali) ==========
  // مناسبت‌های مذهبی بر اساس تاریخ ثابت قمری ذخیره شده‌اند
  // و برای هر سال شمسی به صورت خودکار محاسبه می‌شوند

  HIJRI_OCCASIONS: [
    // محرم (1)
    { hijriMonth: 1, hijriDay: 9, title: "تاسوعای حسینی", isHoliday: true, isReligious: true },
    { hijriMonth: 1, hijriDay: 10, title: "عاشورای حسینی", isHoliday: true, isReligious: true },
    { hijriMonth: 1, hijriDay: 12, title: "شهادت حضرت امام زین‌العابدین (ع)", isHoliday: true, isReligious: true },
    // صفر (2)
    { hijriMonth: 2, hijriDay: 20, title: "اربعین حسینی", isHoliday: true, isReligious: true },
    { hijriMonth: 2, hijriDay: 28, title: "رحلت حضرت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)", isHoliday: true, isReligious: true },
    { hijriMonth: 2, hijriDay: 29, title: "شهادت حضرت امام رضا (ع)", isHoliday: true, isReligious: true },
    // ربیع‌الاول (3)
    { hijriMonth: 3, hijriDay: 8, title: "شهادت امام حسن عسکری (ع) و آغاز امامت حضرت مهدی (عج)", isHoliday: true, isReligious: true },
    { hijriMonth: 3, hijriDay: 17, title: "میلاد پیامبر اکرم (ص) و امام جعفر صادق (ع) / هفته وحدت", isHoliday: true, isReligious: true },
    // جمادی‌الثانی (6)
    { hijriMonth: 6, hijriDay: 3, title: "شهادت حضرت فاطمه زهرا (س)", isHoliday: true, isReligious: true },
    // رجب (7)
    { hijriMonth: 7, hijriDay: 13, title: "ولادت حضرت امام علی (ع) و روز پدر", isHoliday: true, isReligious: true },
    { hijriMonth: 7, hijriDay: 27, title: "مبعث حضرت رسول اکرم (ص)", isHoliday: true, isReligious: true },
    // شعبان (8)
    { hijriMonth: 8, hijriDay: 15, title: "ولادت با سعادت حضرت مهدی (عج) (نیمه شعبان)", isHoliday: true, isReligious: true },
    // رمضان (9)
    { hijriMonth: 9, hijriDay: 21, title: "شهادت حضرت علی (ع)", isHoliday: true, isReligious: true },
    // شوال (10)
    { hijriMonth: 10, hijriDay: 1, title: "عید سعید فطر", isHoliday: true, isReligious: true },
    { hijriMonth: 10, hijriDay: 2, title: "تعطیلی به مناسبت عید سعید فطر", isHoliday: true, isReligious: true },
    { hijriMonth: 10, hijriDay: 25, title: "شهادت حضرت امام جعفر صادق (ع)", isHoliday: true, isReligious: true },
    // ذی‌الحجه (12)
    { hijriMonth: 12, hijriDay: 10, title: "عید سعید قربان", isHoliday: true, isReligious: true },
    { hijriMonth: 12, hijriDay: 18, title: "عید سعید غدیر خم", isHoliday: true, isReligious: true }
  ],

  // ===== الگوریتم‌های تبدیل تقویم =====

  _PERSIAN_EPOCH: 1948320,
  _ISLAMIC_EPOCH: 1948439,
  _lunarCache: {},

  _positiveModulo(a, b) {
    return ((a % b) + b) % b;
  },

  _floorDiv(a, b) {
    return Math.floor(a / b);
  },

  /** تبدیل تاریخ شمسی به JDN */
  _persianToJdn(year, month, day) {
    const epbase = year - (year >= 0 ? 474 : 473);
    const epyear = 474 + this._positiveModulo(epbase, 2820);

    const daysPassed = month <= 7 ? (month - 1) * 31 : 186 + (month - 7) * 30;

    return day +
      daysPassed +
      Math.floor((epyear * 682 - 110) / 2816) +
      (epyear - 1) * 365 +
      this._floorDiv(epbase, 2820) * 1029983 +
      this._PERSIAN_EPOCH;
  },

  /** تبدیل JDN به تاریخ شمسی */
  _jdnToPersian(jdn) {
    const depoch = jdn - this._persianToJdn(475, 1, 1);
    const cycle = this._floorDiv(depoch, 1029983);
    const cyear = this._positiveModulo(depoch, 1029983);

    let ycycle;
    if (cyear === 1029982) {
      ycycle = 2820;
    } else {
      const aux1 = Math.floor(cyear / 366);
      const aux2 = cyear % 366;
      ycycle = Math.floor((2134 * aux1 + 2816 * aux2 + 2815) / 1028522) + aux1 + 1;
    }

    let year = ycycle + 2820 * cycle + 474;
    if (year <= 0) year--;

    const yday = jdn - this._persianToJdn(year, 1, 1) + 1;
    const month = yday <= 186 ? Math.ceil(yday / 31) : Math.ceil((yday - 186) / 30) + 6;
    const day = jdn - this._persianToJdn(year, month, 1) + 1;

    return { year, month, day };
  },

  /** تبدیل تاریخ قمری به JDN */
  _islamicToJdn(year, month, day) {
    return day +
      Math.ceil(29.5001 * (month - 1)) +
      (year - 1) * 354 +
      Math.floor((3 + 11 * year) / 30) +
      this._ISLAMIC_EPOCH - 1;
  },

  /** تبدیل JDN به تاریخ قمری */
  _jdnToIslamic(jdn) {
    const l = jdn - this._ISLAMIC_EPOCH + 1;
    const year = Math.floor((30 * l + 10646) / 10631);
    let month = Math.ceil((l - 29 - this._islamicToJdn(year, 1, 1) + this._ISLAMIC_EPOCH) / 29.5) + 1;
    if (month < 1) month = 1;
    if (month > 12) month = 12;
    const day = jdn - this._islamicToJdn(year, month, 1) + 1;
    return { year, month, day };
  },

  /** تبدیل تاریخ شمسی به هجری قمری */
  persianToIslamic(year, month, day) {
    const key = `${month}-${day}`;
    if (this.officialDataCache[year] && this.officialDataCache[year][key]) {
      const off = this.officialDataCache[year][key];
      return { year: off.hYear, month: off.hMonth, day: off.hDay };
    }
    const jdn = this._persianToJdn(year, month, day);
    return this._jdnToIslamic(jdn);
  },

  /** آیا سال شمسی کبیسه است؟ */
  _isPersianLeap(jy) {
    const base = jy > 0 ? jy - 474 : jy - 473;
    return this._positiveModulo((base + 38) * 682, 2816) < 682;
  },

  /**
   * محاسبه خودکار مناسبت‌های قمری برای یک سال شمسی
   * @param {number} jalaliYear سال شمسی
   * @returns {Object} نگاشت "ماه-روز" → لیست مناسبت‌ها
   */
  _getLunarOccasionsForYear(jalaliYear) {
    if (this._lunarCache[jalaliYear]) return this._lunarCache[jalaliYear];

    const result = {};

    // محدوده JDN سال شمسی
    const yearStart = this._persianToJdn(jalaliYear, 1, 1);
    const lastDay = this._isPersianLeap(jalaliYear) ? 30 : 29;
    const yearEnd = this._persianToJdn(jalaliYear, 12, lastDay);

    // سال‌های قمری همپوشان
    const hijriStart = this._jdnToIslamic(yearStart);
    const hijriEnd = this._jdnToIslamic(yearEnd);

    for (let hijriYear = hijriStart.year; hijriYear <= hijriEnd.year; hijriYear++) {
      for (const occ of this.HIJRI_OCCASIONS) {
        const jdn = this._islamicToJdn(hijriYear, occ.hijriMonth, occ.hijriDay);

        if (jdn >= yearStart && jdn <= yearEnd) {
          const jalali = this._jdnToPersian(jdn);
          const key = `${jalali.month}-${jalali.day}`;

          if (!result[key]) result[key] = [];
          result[key].push({
            title: occ.title,
            isHoliday: occ.isHoliday,
            isReligious: occ.isReligious
          });
        }
      }
    }

    this._lunarCache[jalaliYear] = result;
    return result;
  },

  /**
   * دریافت مناسبت‌های یک روز (شمسی + قمری)
   * @param {number} month ماه شمسی
   * @param {number} day روز شمسی
   * @param {number} [year=1405] سال شمسی
   */
  getOccasions(month, day, year = 1405) {
    const key = `${month}-${day}`;
    if (this.officialDataCache[year] && this.officialDataCache[year][key]) {
      const off = this.officialDataCache[year][key];
      return off.occasions.map(title => ({ title, isHoliday: off.isHoliday, isNational: true }));
    }
    const solar = this.SOLAR_OCCASIONS[key] || [];
    const lunarMap = this._getLunarOccasionsForYear(year);
    const lunar = lunarMap[key] || [];
    return [...solar, ...lunar];
  },

  /**
   * آیا این روز تعطیل رسمی است؟
   * @param {number} month ماه شمسی
   * @param {number} day روز شمسی
   * @param {number} [year=1405] سال شمسی
   */
  isHoliday(month, day, year = 1405) {
    const key = `${month}-${day}`;
    if (this.officialDataCache[year] && this.officialDataCache[year][key]) {
      if (this.officialDataCache[year][key].isHoliday) return true;
    }
    const list = this.getOccasions(month, day, year);
    return list.some(o => o.isHoliday);
  }
};

if (typeof module !== "undefined" && module.exports) {
  module.exports = PersianOccasions;
}
