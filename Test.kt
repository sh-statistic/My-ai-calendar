import android.icu.util.IslamicCalendar
import android.icu.util.ULocale

fun test() {
    val cal = IslamicCalendar(ULocale("ar_SA"))
    cal.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
    // Let's just output this... wait, I can write a simple java program to run.
}
