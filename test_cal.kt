import kotlin.math.ceil
import kotlin.math.floor

const val PERSIAN_EPOCH = 1948320L
const val ISLAMIC_EPOCH = 1948439L

fun positiveModulo(a: Int, b: Int): Int {
    val result = a % b
    return if (result < 0) result + b else result
}

fun floorDiv(a: Long, b: Long): Long {
    val d = a / b
    return if (a xor b < 0 && d * b != a) d - 1 else d
}

fun persianToJdn(year: Int, month: Int, day: Int): Long {
    val epbase = year - if (year >= 0) 474 else 473
    val epyear = 474 + positiveModulo(epbase, 2820)
    val daysPassed = if (month <= 7) (month - 1) * 31L else 186L + (month - 7) * 30L

    return day.toLong() +
        daysPassed +
        (epyear.toLong() * 682L - 110L) / 2816L +
        (epyear - 1L) * 365L +
        floorDiv(epbase.toLong(), 2820L) * 1029983L +
        PERSIAN_EPOCH
}

fun islamicToJdn(year: Int, month: Int, day: Int): Long {
    return day.toLong() +
        ceil(29.5001 * (month - 1)).toLong() +
        (year - 1).toLong() * 354L +
        floor((3.0 + 11.0 * year) / 30.0).toLong() +
        ISLAMIC_EPOCH - 1L
}

fun jdnToIslamic(jdn: Long): Triple<Int, Int, Int> {
    val l = jdn - ISLAMIC_EPOCH + 1
    val year = ((30L * l + 10646L) / 10631L).toInt()
    var month = (ceil((l - 29L - islamicToJdn(year, 1, 1) + ISLAMIC_EPOCH).toDouble() / 29.5) + 1).toInt()
    if (month < 1) month = 1
    if (month > 12) month = 12
    val day = (jdn - islamicToJdn(year, month, 1) + 1).toInt()
    return Triple(year, month, day)
}

fun main() {
    val jdn = persianToJdn(1405, 11, 4)
    println("4 Bahman 1405 -> " + jdnToIslamic(jdn))
}
