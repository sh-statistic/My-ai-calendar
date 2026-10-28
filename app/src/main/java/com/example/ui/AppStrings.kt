package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.example.calendar.PersianCalendarHelper
import com.example.prayer.IranianCity

val LocalAppLanguage = compositionLocalOf { "fa" }

@Composable
fun rememberAppStrings(): AppStrings {
    val lang = LocalAppLanguage.current
    return if (lang == "en") EnStrings else FaStrings
}

interface AppStrings {
    val isFa: Boolean
    val appName: String

    // Navigation & Tabs
    val navCalendar: String
    val navTasks: String
    val navNotes: String
    val navLifeGrid: String
    val navSettings: String
    val navAiChat: String
    val today: String
    val nextMonth: String
    val prevMonth: String

    // Calendar
    val weekdayAbbr: List<String>
    val weekdayNames: List<String>
    val jalaliMonths: List<String>
    val eventsTitle: String
    val occasionsTitle: String
    val occasionsOfDayTitle: String
    val addOccasion: String
    val dayNotesTitle: String
    val addEvent: String
    val editEvent: String
    val deleteEvent: String
    val newEvent: String
    val noEvents: String
    val noOccasions: String
    val noPlansForDay: String
    val tasksOfDayTitle: String
    val officialHoliday: String
    val gregorianEquivalent: String
    val eventTitleLabel: String
    val eventDescLabel: String
    val eventCategoryLabel: String
    val eventTimeLabel: String
    val save: String
    val saveChanges: String
    val cancel: String
    val delete: String
    val edit: String
    fun plansCount(count: Int): String

    // Prayer Times
    val prayerTimesCity: String
    val fajr: String
    val sunrise: String
    val dhuhr: String
    val sunset: String
    val maghrib: String
    val midnight: String
    val prayerTimesAccuracyNote: String
    fun nextPrayerRemaining(name: String, time: String, hours: Int, mins: Int): String

    // Tasks & Checklist
    val tasksTitle: String
    val allTasks: String
    val pendingTasks: String
    val completedTasks: String
    val allTimeTasks: String
    val weekTasks: String
    val overdue: String
    val prevDay: String
    val nextDay: String
    val prevWeek: String
    val nextWeek: String
    val searchTasks: String
    val addTask: String
    val newTask: String
    val editTask: String
    val deleteTask: String
    val noTasks: String
    val taskTitleLabel: String
    val taskDueDateLabel: String
    val taskCategoryLabel: String
    fun taskTag(tag: String): String
    fun taskPriority(priority: String): String

    // Notes
    val notesTitle: String
    val searchNotes: String
    val addNote: String
    val editNote: String
    val deleteNote: String
    val noNotes: String
    val noteTitleLabel: String
    val noteContentLabel: String
    val deleteNoteConfirmTitle: String
    val deleteNoteConfirmMsg: String

    // Life Grid (Memento Mori)
    val lifeGridTitle: String
    val lifeGridSubtitle: String
    val livedMonths: String
    val remainingMonths: String
    val totalMonths: String
    val expectedLifespan: String
    val birthDateNotSet: String
    val birthDateDesc: String
    val editBirthDate: String
    val birthYearLabel: String
    val birthMonthLabel: String
    val currentAndExpectedLife: String
    val currentAge: String
    val remainingLife: String
    val percentOfLife: String
    val elapsedLegend: String
    val currentMonthLegend: String
    val futureLegend: String
    val wholeLifeHeader: String
    val tapYearHint: String
    val anotherMonthPassedTitle: String
    val anotherMonthPassedMsg: String
    val writeNoteConfirm: String
    val later: String
    val close: String
    val howWasThisMonthTitle: String
    val howWasThisMonthMsg: String
    val notePlaceholder: String
    val saveNote: String
    val lifeSettingsTitle: String
    val enterDetailsDesc: String
    val birthYearExample: String
    val saveAndUpdate: String
    val deleteBirthDate: String
    val viewOrAddNote: String
    val tapToAddNote: String
    fun bornInfo(year: String, lifespan: String): String
    fun ageRange(startAge: String, endAge: String): String
    fun ageSpan(years: String, months: String): String
    fun monthsCount(months: String): String
    fun yearLabel(year: String): String

    // Settings
    val settingsTitle: String
    val prayerSettings: String
    val selectCity: String
    val fajrAlarm: String
    val fajrAlarmDesc: String
    val alarmOffset: String
    val minutesBeforeFajr: String
    val languageSettings: String
    val backupRestore: String
    val createBackup: String
    val restoreBackup: String
    val googleSync: String
    val onboardingWizard: String
    val recheckOnboarding: String
    val localServer: String
    val syncTitle: String
    val localWifiServer: String
    val activeStatus: String
    val inactiveStatus: String
    val copyAddress: String
    val guide: String
    val understood: String
    val addressCopied: String
    val googleCalendarCloud: String
    val accessGranted: String
    val noCalendarSelected: String
    val requiresCalendarPerm: String
    val grantPermission: String
    val selectCalendar: String
    val noCalendarFound: String
    val uploadToGoogle: String
    val uploading: String
    val downloadFromGoogle: String
    val downloading: String
    val googleSyncNote: String
    val calendarSettingsTitle: String
    val hijriDayAdjustment: String
    val hijriAdjustmentDesc: String
    val noChange: String
    val offlineBackupTitle: String
    val offlineJsonFile: String
    val saveFileOnPhone: String
    val importBackup: String
    val exportBackup: String
    val smartFajrAlarm: String
    val calendarPrayerHorizonCity: String
    val changeCity: String
    val alarmTimingBeforeFajr: String
    val simultaneous: String
    fun formatFajrAlarmOffset(offsetMinutesBefore: Int): String
    val userCustomOccasions: String
    val noCustomOccasionsYet: String
    val holidayBadge: String
    val appLogs: String
    val clearLogs: String
    val noSyncLogsYet: String
    val languageAndWizard: String
    val appVersion: String
    val checkingForUpdate: String
    val checkNewVersion: String
    val shareAppWithOthers: String
    val selectCityForPrayerTimes: String
    fun daysOffsetLabel(offset: Int): String

    // In-App Update Dialog Strings
    val updateDialogTitleDownloading: String
    val updateDialogTitleReady: String
    val updateDialogTitlePermission: String
    val updateDialogTitleError: String
    val updateDialogTitleAvailable: String
    val downloadProgressLabel: String
    val downloadProgressCalculating: String
    fun downloadingVersionNote(version: String): String
    val currentVersionBadge: String
    val newVersionBadge: String
    val whatsNewInUpdate: String
    val safetyBackupNotice: String
    val playProtectNotice: String
    val apkReadyTitle: String
    val apkReadyMessage: String
    val permissionRequiredMessage: String
    val permissionRequiredHint: String
    fun updateErrorMessage(error: String): String
    val updateErrorHint: String
    val installNow: String
    val enablePermission: String
    val reinstall: String
    val tryAgain: String
    val autoUpdate: String
    val downloadFromGithub: String
    val updateLater: String
    val cancelAction: String
    fun fileSizeMegaBytes(size: String): String

    // Helpers
    fun formatDigits(text: String): String
    fun cityName(city: IranianCity): String
}

object FaStrings : AppStrings {
    override val isFa: Boolean = true
    override val appName: String = "همگام"

    override val navCalendar: String = "تقویم"
    override val navTasks: String = "وظایف"
    override val navNotes: String = "یادداشت"
    override val navLifeGrid: String = "عمر من"
    override val navSettings: String = "تنظیمات"
    override val navAiChat: String = "دستیار هوشمند"
    override val today: String = "امروز"
    override val nextMonth: String = "ماه بعد"
    override val prevMonth: String = "ماه قبل"

    override val weekdayAbbr: List<String> = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    override val weekdayNames: List<String> = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه")
    override val jalaliMonths: List<String> = PersianCalendarHelper.PERSIAN_MONTH_NAMES
    override val eventsTitle: String = "رویدادها"
    override val occasionsTitle: String = "مناسبت‌ها"
    override val occasionsOfDayTitle: String = "مناسبت‌های این روز:"
    override val addOccasion: String = "ثبت مناسبت"
    override val dayNotesTitle: String = "یادداشت روز"
    override val addEvent: String = "ثبت رویداد جدید"
    override val editEvent: String = "ویرایش رویداد"
    override val deleteEvent: String = "حذف رویداد"
    override val newEvent: String = "رویداد جدید"
    override val noEvents: String = "رویدادی برای این روز ثبت نشده است."
    override val noOccasions: String = "مناسبتی برای این روز ثبت نشده است."
    override val noPlansForDay: String = "هیچ برنامه یا وظیفه‌ای برای این روز ثبت نشده است."
    override val tasksOfDayTitle: String = "وظایف و کارهای این روز:"
    override val officialHoliday: String = "تعطیل رسمی"
    override val gregorianEquivalent: String = "معادل میلادی:"
    override val eventTitleLabel: String = "عنوان رویداد"
    override val eventDescLabel: String = "توضیحات (اختیاری)"
    override val eventCategoryLabel: String = "دسته‌بندی:"
    override val eventTimeLabel: String = "زمان رویداد"
    override val save: String = "ذخیره"
    override val saveChanges: String = "ذخیره تغییرات"
    override val cancel: String = "انصراف"
    override val delete: String = "حذف"
    override val edit: String = "ویرایش"
    override fun plansCount(count: Int): String = "${PersianCalendarHelper.toPersianDigits(count.toString())} برنامه"

    override val prayerTimesCity: String = "شهر اوقات شرعی:"
    override val fajr: String = "اذان صبح"
    override val sunrise: String = "طلوع"
    override val dhuhr: String = "اذان ظهر"
    override val sunset: String = "غروب"
    override val maghrib: String = "اذان مغرب"
    override val midnight: String = "نیمه‌شب"
    override val prayerTimesAccuracyNote: String = "⚠️ احتیاط: اوقات شرعی با فرمول ژئوفیزیک محاسبه شده و ممکن است با افق دقیق محلی چند دقیقه تفاوت داشته باشد؛ لطفاً برای ادای فرایض دقایقی احتیاط فرمایید."
    override fun nextPrayerRemaining(name: String, time: String, hours: Int, mins: Int): String {
        val remaining = if (hours > 0) "${hours} ساعت و ${mins} دقیقه" else "${mins} دقیقه"
        return "${name}: ${PersianCalendarHelper.toPersianDigits(time)} (${PersianCalendarHelper.toPersianDigits(remaining)})"
    }

    override val tasksTitle: String = "وظایف و چک‌لیست"
    override val allTasks: String = "همه"
    override val pendingTasks: String = "در انتظار"
    override val completedTasks: String = "انجام شده"
    override val allTimeTasks: String = "کل وظایف"
    override val weekTasks: String = "وظایف هفته"
    override val overdue: String = "معوقه"
    override val prevDay: String = "روز قبل"
    override val nextDay: String = "روز بعد"
    override val prevWeek: String = "هفته قبل"
    override val nextWeek: String = "هفته بعد"
    override val searchTasks: String = "جستجو در وظایف..."
    override val addTask: String = "افزودن وظیفه"
    override val newTask: String = "وظیفه جدید"
    override val editTask: String = "ویرایش وظیفه"
    override val deleteTask: String = "حذف وظیفه"
    override val noTasks: String = "هیچ وظیفه‌ای در این دسته‌بندی وجود ندارد."
    override val taskTitleLabel: String = "عنوان وظیفه"
    override val taskDueDateLabel: String = "مهلت انجام"
    override val taskCategoryLabel: String = "دسته‌بندی:"
    override fun taskTag(tag: String): String = tag
    override fun taskPriority(priority: String): String = priority

    override val notesTitle: String = "یادداشت‌ها"
    override val searchNotes: String = "جستجو در یادداشت‌ها..."
    override val addNote: String = "یادداشت جدید"
    override val editNote: String = "ویرایش یادداشت"
    override val deleteNote: String = "حذف یادداشت"
    override val noNotes: String = "هنوز یادداشتی ثبت نکرده‌اید."
    override val noteTitleLabel: String = "عنوان یادداشت"
    override val noteContentLabel: String = "متن یادداشت..."
    override val deleteNoteConfirmTitle: String = "حذف یادداشت"
    override val deleteNoteConfirmMsg: String = "آیا از حذف این یادداشت اطمینان دارید؟"

    override val lifeGridTitle: String = "عمر من"
    override val lifeGridSubtitle: String = "نمایش بصری ماه‌های سپری‌شده از زندگی به شیوه Memento Mori"
    override val livedMonths: String = "ماه‌های سپری‌شده"
    override val remainingMonths: String = "ماه‌های باقی‌مانده"
    override val totalMonths: String = "کل ماه‌ها"
    override val expectedLifespan: String = "امید به زندگی (سال):"
    override val birthDateNotSet: String = "تاریخ تولد شما تنظیم نشده است"
    override val birthDateDesc: String = "برای مشاهده میزان ماه‌های سپری‌شده و باقی‌مانده از عمر در این جدول، تاریخ تولد خود را تعیین نمایید."
    override val editBirthDate: String = "تنظیم تاریخ تولد"
    override val birthYearLabel: String = "سال تولد شمسی:"
    override val birthMonthLabel: String = "ماه تولد:"
    override val currentAndExpectedLife: String = "عمر فعلی و پیش‌بینی‌شده"
    override val currentAge: String = "سن فعلی"
    override val remainingLife: String = "باقیمانده"
    override val percentOfLife: String = "از عمر"
    override val elapsedLegend: String = "سپری‌شده"
    override val currentMonthLegend: String = "ماه فعلی"
    override val futureLegend: String = "آینده"
    override val wholeLifeHeader: String = "کل عمر؛ هر بلوک یک سال"
    override val tapYearHint: String = "سال را لمس کنید"
    override val anotherMonthPassedTitle: String = "یک ماه دیگر گذشت..."
    override val anotherMonthPassedMsg: String = "ماه جدیدی از عمر شما آغاز شده است و ماه قبلی تکمیل شد! آیا می‌خواهید برای ماه گذشته یادداشتی ثبت کنید؟"
    override val writeNoteConfirm: String = "بله، یادداشت می‌نویسم"
    override val later: String = "بعداً"
    override val close: String = "بستن"
    override val howWasThisMonthTitle: String = "این ماه چطور گذشت؟"
    override val howWasThisMonthMsg: String = "یک ماه دیگر از عمر شما سپری و ثبت شد. مهم‌ترین خاطره یا دستاورد این ماه را بنویسید:"
    override val notePlaceholder: String = "مثال: پروژه جدید را شروع کردم..."
    override val saveNote: String = "ذخیره یادداشت"
    override val lifeSettingsTitle: String = "تنظیمات عمر من"
    override val enterDetailsDesc: String = "مشخصات خود را برای محاسبه دقیق وارد کنید:"
    override val birthYearExample: String = "مثال: ۱۳۷۵"
    override val saveAndUpdate: String = "ذخیره و بروزرسانی"
    override val deleteBirthDate: String = "حذف تاریخ تولد"
    override val viewOrAddNote: String = "مشاهده/ثبت"
    override val tapToAddNote: String = "لمس برای ثبت"
    override fun bornInfo(year: String, lifespan: String): String = "متولد ${formatDigits(year)} · ${formatDigits(lifespan)} سال"
    override fun ageRange(startAge: String, endAge: String): String = "${formatDigits(startAge)} تا ${formatDigits(endAge)} سالگی"
    override fun ageSpan(years: String, months: String): String = "${formatDigits(years)} سال و ${formatDigits(months)} ماه"
    override fun monthsCount(months: String): String = "(${formatDigits(months)} ماه)"
    override fun yearLabel(year: String): String = "سال ${formatDigits(year)}"

    override val settingsTitle: String = "تنظیمات و همگام‌سازی"
    override val prayerSettings: String = "اوقات شرعی و بیدارباش اذان"
    override val selectCity: String = "انتخاب شهر اوقات شرعی:"
    override val fajrAlarm: String = "بیدارباش اذان صبح"
    override val fajrAlarmDesc: String = "پخش مداوم زنگ بیدارباش تا زمان لمس دکمه قطع"
    override val alarmOffset: String = "زمان پخش بیدارباش:"
    override val minutesBeforeFajr: String = "دقیقه قبل از اذان"
    override val languageSettings: String = "زبان برنامه"
    override val backupRestore: String = "پشتیبان‌گیری و بازیابی داده‌ها"
    override val createBackup: String = "تهیه نسخه پشتیبان (JSON)"
    override val restoreBackup: String = "بازیابی نسخه پشتیبان"
    override val googleSync: String = "همگام‌سازی تقویم گوگل"
    override val onboardingWizard: String = "راهنمای راه‌اندازی اولیه"
    override val recheckOnboarding: String = "مشاهده مجدد راهنمای ورود به برنامه"
    override val localServer: String = "سرور محلی همگام‌سازی وای‌فای"
    override val syncTitle: String = "همگام‌سازی"
    override val localWifiServer: String = "سرور محلی Wi-Fi"
    override val activeStatus: String = "فعال"
    override val inactiveStatus: String = "غیرفعال"
    override val copyAddress: String = "کپی آدرس"
    override val guide: String = "راهنما"
    override val understood: String = "متوجه شدم"
    override val addressCopied: String = "آدرس کپی شد"
    override val googleCalendarCloud: String = "تقویم گوگل (Cloud)"
    override val accessGranted: String = "دسترسی تأیید شد"
    override val noCalendarSelected: String = "تقویم انتخاب نشده"
    override val requiresCalendarPerm: String = "نیازمند دسترسی به تقویم دستگاه"
    override val grantPermission: String = "اعطای دسترسی"
    override val selectCalendar: String = "انتخاب تقویم:"
    override val noCalendarFound: String = "هیچ حساب تقویمی روی دستگاه یافت نشد. لطفاً حساب گوگل را در تنظیمات گوشی اضافه کنید."
    override val uploadToGoogle: String = "ارسال به گوگل"
    override val uploading: String = "در حال ارسال..."
    override val downloadFromGoogle: String = "دریافت از گوگل"
    override val downloading: String = "در حال دریافت..."
    override val googleSyncNote: String = "رویدادها، وظایف و یادداشت‌ها به تقویم گوگل ارسال می‌شوند و توسط سرورهای گوگل همگام خواهند شد."
    override val calendarSettingsTitle: String = "تنظیمات تقویم"
    override val hijriDayAdjustment: String = "تنظیم روز هجری قمری"
    override val hijriAdjustmentDesc: String = "برای تطبیق با تقویم رسمی کشور"
    override val noChange: String = "بدون تغییر"
    override val offlineBackupTitle: String = "پشتیبان‌گیری (آفلاین)"
    override val offlineJsonFile: String = "فایل آفلاین (JSON)"
    override val saveFileOnPhone: String = "ذخیره فایل در گوشی"
    override val importBackup: String = "ورود"
    override val exportBackup: String = "خروجی"
    override val smartFajrAlarm: String = "بیدارباش هوشمند اذان صبح"
    override val calendarPrayerHorizonCity: String = "شهر افق شرعی تقویم:"
    override val changeCity: String = "تغییر شهر"
    override val alarmTimingBeforeFajr: String = "زمان زنگ هشدار نسبت به اذان صبح:"
    override val simultaneous: String = "همزمان با اذان"
    override fun formatFajrAlarmOffset(offsetMinutesBefore: Int): String {
        return when {
            offsetMinutesBefore > 0 -> "${com.example.calendar.PersianCalendarHelper.toPersianDigits(offsetMinutesBefore.toString())} دقیقه قبل از اذان صبح"
            offsetMinutesBefore < 0 -> {
                val after = -offsetMinutesBefore
                if (after == 60) "۱ ساعت بعد از اذان صبح"
                else "${com.example.calendar.PersianCalendarHelper.toPersianDigits(after.toString())} دقیقه بعد از اذان صبح"
            }
            else -> "همزمان با اذان صبح"
        }
    }
    override val userCustomOccasions: String = "مناسبت‌های ثبت‌شده توسط کاربر"
    override val noCustomOccasionsYet: String = "هنوز مناسبتی ثبت نشده. برای ثبت مناسبت، در تقویم یک روز را انتخاب کنید و دکمه «ثبت مناسبت» را بزنید."
    override val holidayBadge: String = "(تعطیل)"
    override val appLogs: String = "گزارش‌های برنامه"
    override val clearLogs: String = "پاک کردن لاگ‌ها"
    override val noSyncLogsYet: String = "هنوز رویداد همگام‌سازی ثبت نشده است."
    override val languageAndWizard: String = "زبان برنامه و راهنمای راه‌اندازی"
    override val appVersion: String = "نسخه برنامه:"
    override val checkingForUpdate: String = "در حال بررسی..."
    override val checkNewVersion: String = "بررسی نسخه جدید"
    override val shareAppWithOthers: String = "ارسال نرم‌افزار به دیگران"
    override val selectCityForPrayerTimes: String = "انتخاب شهر برای اوقات شرعی"
    override fun daysOffsetLabel(offset: Int): String = when {
        offset > 0 -> "+${formatDigits(offset.toString())} روز"
        offset < 0 -> "${formatDigits(offset.toString())} روز"
        else -> noChange
    }

    // In-App Update Dialog Strings
    override val updateDialogTitleDownloading: String = "در حال دریافت نسخه جدید"
    override val updateDialogTitleReady: String = "نسخه جدید آماده نصب است"
    override val updateDialogTitlePermission: String = "مجوز نصب برنامه‌ها مورد نیاز است"
    override val updateDialogTitleError: String = "خطا در فرآیند به‌روزرسانی"
    override val updateDialogTitleAvailable: String = "نسخه جدید برنامه موجود است"
    override val downloadProgressLabel: String = "پیشرفت دانلود:"
    override val downloadProgressCalculating: String = "در حال محاسبه..."
    override fun downloadingVersionNote(version: String): String = "در حال دانلود فایل نسخه ${formatDigits(version)}... لطفاً برنامه را نبندید."
    override val currentVersionBadge: String = "نسخه فعلی:"
    override val newVersionBadge: String = "نسخه جدید:"
    override val whatsNewInUpdate: String = "تغییرات نسخه جدید:"
    override val safetyBackupNotice: String = "محض احتیاط، پیش از نصب یک نسخه پشتیبان خودکار از کلیه اطلاعات و رویدادهای شما تهیه می‌شود."
    override val playProtectNotice: String = "توجه: چون این برنامه مستقیماً از گیت‌هاب دریافت شده و هنوز در فروشگاه‌هایی مانند گوگل‌پلی یا بازار منتشر نشده، ممکن است سپر ایمنی گوگل (Play Protect) برای چند ثانیه صفحه اسکن فایل را نشان دهد. این بررسی کاملاً طبیعی است؛ لطفاً تا پایان اسکن صبور باشید تا دکمه نصب فعال شود."
    override val apkReadyTitle: String = "فایل با موفقیت دریافت شد"
    override val apkReadyMessage: String = "نصاب اندروید باز شد. در صورت نمایش صفحه اسکن سپر ایمنی گوگل (Play Protect)، به دلیل نصب مستقیم خارج از استورها کاملاً طبیعی است؛ لطفاً تا اتمام چند ثانیه‌ای اسکن شکیبا باشید تا دکمه نصب ظاهر شود."
    override val permissionRequiredMessage: String = "جهت نصب مستقیم نسخه جدید، لطفاً اجازه «نصب برنامه‌های ناشناخته» را در تنظیمات فعال کنید."
    override val permissionRequiredHint: String = "پس از روشن کردن کلید دسترسی، دکمه بازگشت را بزنید و سپس روی «نصب نسخه جدید» کلیک کنید."
    override fun updateErrorMessage(error: String): String = "پیام خطا: $error"
    override val updateErrorHint: String = "می‌توانید مجدداً تلاش کنید یا فایل را مستقیماً از طریق مرورگر از گیت‌هاب دانلود نمایید."
    override val installNow: String = "نصب نسخه جدید"
    override val enablePermission: String = "فعال‌سازی مجوز نصب"
    override val reinstall: String = "نصب مجدد"
    override val tryAgain: String = "تلاش مجدد"
    override val autoUpdate: String = "به‌روزرسانی به نسخه جدید"
    override val downloadFromGithub: String = "دانلود از گیت‌هاب"
    override val updateLater: String = "بعداً"
    override val cancelAction: String = "انصراف"
    override fun fileSizeMegaBytes(size: String): String = "${formatDigits(size)} مگابایت"

    override fun formatDigits(text: String): String = PersianCalendarHelper.toPersianDigits(text)
    override fun cityName(city: IranianCity): String = city.nameFa
}

object EnStrings : AppStrings {
    override val isFa: Boolean = false
    override val appName: String = "Hamgam"

    override val navCalendar: String = "Calendar"
    override val navTasks: String = "Tasks"
    override val navNotes: String = "Notes"
    override val navLifeGrid: String = "Life Grid"
    override val navSettings: String = "Settings"
    override val navAiChat: String = "AI Chat"
    override val today: String = "Today"
    override val nextMonth: String = "Next"
    override val prevMonth: String = "Prev"

    override val weekdayAbbr: List<String> = listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr")
    override val weekdayNames: List<String> = listOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    override val jalaliMonths: List<String> = listOf(
        "Farvardin", "Ordibehesht", "Khordad",
        "Tir", "Mordad", "Shahrivar",
        "Mehr", "Aban", "Azar",
        "Dey", "Bahman", "Esfand"
    )
    override val eventsTitle: String = "Events"
    override val occasionsTitle: String = "Occasions"
    override val occasionsOfDayTitle: String = "Occasions of this day:"
    override val addOccasion: String = "+ Add Occasion"
    override val dayNotesTitle: String = "Daily Note"
    override val addEvent: String = "+ Add Event"
    override val editEvent: String = "Edit Event"
    override val deleteEvent: String = "Delete Event"
    override val newEvent: String = "New Event"
    override val noEvents: String = "No events scheduled for this day."
    override val noOccasions: String = "No special occasions today."
    override val noPlansForDay: String = "No events or tasks scheduled for this day."
    override val tasksOfDayTitle: String = "Tasks for this day:"
    override val officialHoliday: String = "Official Holiday"
    override val gregorianEquivalent: String = "Gregorian:"
    override val eventTitleLabel: String = "Event Title"
    override val eventDescLabel: String = "Description (Optional)"
    override val eventCategoryLabel: String = "Category:"
    override val eventTimeLabel: String = "Event Time"
    override val save: String = "Save"
    override val saveChanges: String = "Save Changes"
    override val cancel: String = "Cancel"
    override val delete: String = "Delete"
    override val edit: String = "Edit"
    override fun plansCount(count: Int): String = if (count == 1) "1 plan" else "$count plans"

    override val prayerTimesCity: String = "Prayer City:"
    override val fajr: String = "Fajr"
    override val sunrise: String = "Sunrise"
    override val dhuhr: String = "Dhuhr"
    override val sunset: String = "Sunset"
    override val maghrib: String = "Maghrib"
    override val midnight: String = "Midnight"
    override val prayerTimesAccuracyNote: String = "⚠️ Notice: Prayer times are calculated astronomically and may slightly differ from local horizon; please allow a few minutes caution."
    override fun nextPrayerRemaining(name: String, time: String, hours: Int, mins: Int): String {
        val remaining = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        val translatedName = when {
            name.contains("صبح") -> "Fajr"
            name.contains("طلوع") -> "Sunrise"
            name.contains("ظهر") -> "Dhuhr"
            name.contains("غروب") -> "Sunset"
            name.contains("مغرب") -> "Maghrib"
            name.contains("نیمه‌شب") -> "Midnight"
            else -> name
        }
        return "${translatedName}: ${time} (in ${remaining})"
    }

    override val tasksTitle: String = "Tasks & Checklist"
    override val allTasks: String = "All"
    override val pendingTasks: String = "Pending"
    override val completedTasks: String = "Completed"
    override val allTimeTasks: String = "All Time"
    override val weekTasks: String = "Week Tasks"
    override val overdue: String = "Overdue"
    override val prevDay: String = "Prev Day"
    override val nextDay: String = "Next Day"
    override val prevWeek: String = "Prev Week"
    override val nextWeek: String = "Next Week"
    override val searchTasks: String = "Search tasks..."
    override val addTask: String = "+ New Task"
    override val newTask: String = "New Task"
    override val editTask: String = "Edit Task"
    override val deleteTask: String = "Delete Task"
    override val noTasks: String = "No tasks found in this section."
    override val taskTitleLabel: String = "Task Title"
    override val taskDueDateLabel: String = "Due Date"
    override val taskCategoryLabel: String = "Category:"
    override fun taskTag(tag: String): String = when (tag.trim()) {
        "عمومی" -> "General"
        "کسب و کار" -> "Business"
        "عبادت" -> "Worship"
        "تفریح" -> "Leisure"
        "معاشرت" -> "Social"
        "خرید" -> "Shopping"
        "شخصی" -> "Personal"
        "کاری" -> "Work"
        "یادآوری" -> "Reminder"
        else -> tag
    }
    override fun taskPriority(priority: String): String = when (priority.trim()) {
        "فوری" -> "Urgent"
        "بالا" -> "High"
        "متوسط" -> "Medium"
        "معمولی" -> "Normal"
        "پایین" -> "Low"
        else -> priority
    }

    override val notesTitle: String = "Notes"
    override val searchNotes: String = "Search notes..."
    override val addNote: String = "+ New Note"
    override val editNote: String = "Edit Note"
    override val deleteNote: String = "Delete Note"
    override val noNotes: String = "You haven't created any notes yet."
    override val noteTitleLabel: String = "Note Title"
    override val noteContentLabel: String = "Note content..."
    override val deleteNoteConfirmTitle: String = "Delete Note"
    override val deleteNoteConfirmMsg: String = "Are you sure you want to delete this note?"

    override val lifeGridTitle: String = "My Life Grid"
    override val lifeGridSubtitle: String = "Visualizing lived months inspired by Memento Mori"
    override val livedMonths: String = "Lived Months"
    override val remainingMonths: String = "Remaining Months"
    override val totalMonths: String = "Total Months"
    override val expectedLifespan: String = "Expected Lifespan (Years):"
    override val birthDateNotSet: String = "Birth date not set"
    override val birthDateDesc: String = "Please set your birth date to visualize lived and remaining life blocks."
    override val editBirthDate: String = "Set Birth Date"
    override val birthYearLabel: String = "Solar Birth Year:"
    override val birthMonthLabel: String = "Birth Month:"
    override val currentAndExpectedLife: String = "Current & Expected Lifespan"
    override val currentAge: String = "Current Age"
    override val remainingLife: String = "Remaining"
    override val percentOfLife: String = "of life"
    override val elapsedLegend: String = "Elapsed"
    override val currentMonthLegend: String = "Current Month"
    override val futureLegend: String = "Future"
    override val wholeLifeHeader: String = "Whole Life; each block is 1 year"
    override val tapYearHint: String = "Tap a year"
    override val anotherMonthPassedTitle: String = "Another month passed..."
    override val anotherMonthPassedMsg: String = "A new month of your life has begun! Would you like to write a reflection note for the past month?"
    override val writeNoteConfirm: String = "Yes, write note"
    override val later: String = "Later"
    override val close: String = "Close"
    override val howWasThisMonthTitle: String = "How was this month?"
    override val howWasThisMonthMsg: String = "Another month of your life has passed. Write your key achievement or memory:"
    override val notePlaceholder: String = "e.g., Started a new project..."
    override val saveNote: String = "Save Note"
    override val lifeSettingsTitle: String = "Life Grid Settings"
    override val enterDetailsDesc: String = "Enter your details for accurate calculation:"
    override val birthYearExample: String = "e.g., 1375"
    override val saveAndUpdate: String = "Save & Update"
    override val deleteBirthDate: String = "Delete Birth Date"
    override val viewOrAddNote: String = "View/Add"
    override val tapToAddNote: String = "Tap to add"
    override fun bornInfo(year: String, lifespan: String): String = "Born $year · $lifespan years"
    override fun ageRange(startAge: String, endAge: String): String = "Age $startAge to $endAge"
    override fun ageSpan(years: String, months: String): String = "$years years & $months months"
    override fun monthsCount(months: String): String = "($months months)"
    override fun yearLabel(year: String): String = "Year $year"

    override val settingsTitle: String = "Settings & Sync"
    override val prayerSettings: String = "Prayer Times & Fajr Alarm"
    override val selectCity: String = "Select Prayer City:"
    override val fajrAlarm: String = "Fajr Wakeup Alarm"
    override val fajrAlarmDesc: String = "Plays looping alarm audio continuously until manually stopped"
    override val alarmOffset: String = "Alarm Timing:"
    override val minutesBeforeFajr: String = "minutes before Fajr"
    override val languageSettings: String = "App Language"
    override val backupRestore: String = "Backup & Restore"
    override val createBackup: String = "Create Backup (JSON)"
    override val restoreBackup: String = "Restore Backup"
    override val googleSync: String = "Google Calendar Sync"
    override val onboardingWizard: String = "Onboarding Wizard"
    override val recheckOnboarding: String = "Show Welcome Tour Again"
    override val localServer: String = "Local Wi-Fi Sync Server"
    override val syncTitle: String = "Synchronization"
    override val localWifiServer: String = "Local Wi-Fi Server"
    override val activeStatus: String = "Active"
    override val inactiveStatus: String = "Inactive"
    override val copyAddress: String = "Copy Address"
    override val guide: String = "Guide"
    override val understood: String = "Understood"
    override val addressCopied: String = "Address copied"
    override val googleCalendarCloud: String = "Google Calendar (Cloud)"
    override val accessGranted: String = "Access Granted"
    override val noCalendarSelected: String = "No calendar selected"
    override val requiresCalendarPerm: String = "Requires calendar permission"
    override val grantPermission: String = "Grant Permission"
    override val selectCalendar: String = "Select Calendar:"
    override val noCalendarFound: String = "No calendar accounts found on device. Please add a Google account in phone settings."
    override val uploadToGoogle: String = "Upload to Google"
    override val uploading: String = "Uploading..."
    override val downloadFromGoogle: String = "Download from Google"
    override val downloading: String = "Downloading..."
    override val googleSyncNote: String = "Events, tasks and notes will be synced with Google Calendar."
    override val calendarSettingsTitle: String = "Calendar Settings"
    override val hijriDayAdjustment: String = "Lunar Hijri Day Adjustment"
    override val hijriAdjustmentDesc: String = "To match official country calendar"
    override val noChange: String = "No change"
    override val offlineBackupTitle: String = "Offline Backup"
    override val offlineJsonFile: String = "Offline File (JSON)"
    override val saveFileOnPhone: String = "Save file on device"
    override val importBackup: String = "Import"
    override val exportBackup: String = "Export"
    override val smartFajrAlarm: String = "Smart Fajr Alarm"
    override val calendarPrayerHorizonCity: String = "Calendar Prayer City:"
    override val changeCity: String = "Change City"
    override val alarmTimingBeforeFajr: String = "Alarm timing relative to Fajr prayer:"
    override val simultaneous: String = "At Fajr"
    override fun formatFajrAlarmOffset(offsetMinutesBefore: Int): String {
        return when {
            offsetMinutesBefore > 0 -> "$offsetMinutesBefore min before Fajr"
            offsetMinutesBefore < 0 -> {
                val after = -offsetMinutesBefore
                if (after == 60) "1 hour after Fajr"
                else "$after min after Fajr"
            }
            else -> "At Fajr prayer"
        }
    }
    override val userCustomOccasions: String = "Custom Occasions"
    override val noCustomOccasionsYet: String = "No custom occasions yet. Select a date on calendar and tap 'Add Occasion'."
    override val holidayBadge: String = "(Holiday)"
    override val appLogs: String = "App Logs"
    override val clearLogs: String = "Clear Logs"
    override val noSyncLogsYet: String = "No sync events recorded yet."
    override val languageAndWizard: String = "Language & Onboarding"
    override val appVersion: String = "App Version:"
    override val checkingForUpdate: String = "Checking..."
    override val checkNewVersion: String = "Check for Updates"
    override val shareAppWithOthers: String = "Share App with Others"
    override val selectCityForPrayerTimes: String = "Select City for Prayer Times"
    override fun daysOffsetLabel(offset: Int): String = when {
        offset > 0 -> "+$offset days"
        offset < 0 -> "$offset days"
        else -> noChange
    }

    // In-App Update Dialog Strings
    override val updateDialogTitleDownloading: String = "Downloading New Version"
    override val updateDialogTitleReady: String = "Update Ready to Install"
    override val updateDialogTitlePermission: String = "Install Permission Required"
    override val updateDialogTitleError: String = "Update Error"
    override val updateDialogTitleAvailable: String = "New Version Available"
    override val downloadProgressLabel: String = "Download Progress:"
    override val downloadProgressCalculating: String = "Calculating..."
    override fun downloadingVersionNote(version: String): String = "Downloading version $version... Please do not close the app."
    override val currentVersionBadge: String = "Current:"
    override val newVersionBadge: String = "New:"
    override val whatsNewInUpdate: String = "What's New in This Version:"
    override val safetyBackupNotice: String = "As a safety precaution, an automatic backup of all your data is created before upgrading."
    override val playProtectNotice: String = "Note: Because this app is downloaded directly from GitHub and not yet on Google Play, Google Play Protect may scan the file for a few seconds. This is completely normal; please wait until the scan finishes and the install button appears."
    override val apkReadyTitle: String = "Download Complete"
    override val apkReadyMessage: String = "Android installer opened. If Google Play Protect scanning screen appears, it is normal for direct downloads; please wait a few seconds for the scan to finish."
    override val permissionRequiredMessage: String = "To install the update, please enable 'Install unknown apps' permission in Settings."
    override val permissionRequiredHint: String = "After enabling the switch in Settings, return to the app and the update will install automatically."
    override fun updateErrorMessage(error: String): String = "Error: $error"
    override val updateErrorHint: String = "You can retry or download the APK directly from GitHub in your browser."
    override val installNow: String = "Install Update Now"
    override val enablePermission: String = "Grant Install Permission"
    override val reinstall: String = "Install Again"
    override val tryAgain: String = "Try Again"
    override val autoUpdate: String = "Update to New Version"
    override val downloadFromGithub: String = "GitHub Download"
    override val updateLater: String = "Later"
    override val cancelAction: String = "Cancel"
    override fun fileSizeMegaBytes(size: String): String = "$size MB"

    override fun formatDigits(text: String): String = text
    override fun cityName(city: IranianCity): String = city.nameEn
}
