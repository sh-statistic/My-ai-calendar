/**
 * Persian Calendar Occasions and Official Iranian Holidays Engine
 * Ported directly from PersianOccasionsHelper.kt
 */

const PersianOccasions = {
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

  LUNAR_OCCASIONS: {
    "1-1": [{ title: "عید سعید فطر", isHoliday: true, isReligious: true }],
    "1-2": [{ title: "تعطیلی به مناسبت عید سعید فطر", isHoliday: true, isReligious: true }],
    "1-25": [{ title: "شهادت حضرت امام جعفر صادق (ع)", isHoliday: true, isReligious: true }],
    "3-6": [{ title: "عید سعید قربان", isHoliday: true, isReligious: true }],
    "3-14": [{ title: "عید سعید غدیر خم", isHoliday: true, isReligious: true }],
    "4-4": [{ title: "تاسوعای حسینی (۹ محرم)", isHoliday: true, isReligious: true }],
    "4-5": [{ title: "عاشورای حسینی (۱۰ محرم)", isHoliday: true, isReligious: true }],
    "5-14": [{ title: "اربعین حسینی (۲۰ صفر)", isHoliday: true, isReligious: true }],
    "5-22": [{ title: "رحلت حضرت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)", isHoliday: true, isReligious: true }],
    "5-24": [{ title: "شهادت حضرت امام رضا (ع)", isHoliday: true, isReligious: true }],
    "6-1": [{ title: "شهادت امام حسن عسکری (ع) و آغاز امامت حضرت مهدی (عج)", isHoliday: true, isReligious: true }],
    "6-10": [{ title: "میلاد پیامبر اکرم (ص) و امام جعفر صادق (ع) / هفته وحدت", isHoliday: true, isReligious: true }],
    "8-24": [{ title: "شهادت حضرت فاطمه زهرا (س)", isHoliday: true, isReligious: true }],
    "10-3": [{ title: "ولادت حضرت امام علی (ع) و روز پدر", isHoliday: true, isReligious: true }],
    "10-17": [{ title: "مبعث حضرت رسول اکرم (ص)", isHoliday: true, isReligious: true }],
    "11-5": [{ title: "ولادت با سعادت حضرت مهدی (عج) (نیمه شعبان)", isHoliday: true, isReligious: true }],
    "12-19": [{ title: "شهادت حضرت علی (ع) و شب قدر", isHoliday: true, isReligious: true }]
  },

  getOccasions(month, day) {
    const key = `${month}-${day}`;
    const solar = this.SOLAR_OCCASIONS[key] || [];
    const lunar = this.LUNAR_OCCASIONS[key] || [];
    return [...solar, ...lunar];
  },

  isHoliday(month, day) {
    const list = this.getOccasions(month, day);
    return list.some(o => o.isHoliday);
  }
};

if (typeof module !== "undefined" && module.exports) {
  module.exports = PersianOccasions;
}
