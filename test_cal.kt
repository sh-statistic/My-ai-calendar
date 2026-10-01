import java.util.Calendar  
fun main() {  
val cal = Calendar.getInstance()  
cal.set(2026, 10 - 1, 1)  
println(cal.get(Calendar.DAY_OF_WEEK))  
val jYear = 1403  
val jMonth = 7  
val jDay = 12  
val epBase = jYear - if (jYear >= 0) 474 else 473  
val epYear = 474 + (epBase %% 2820)  
val md = if (jMonth <= 7) (jMonth - 1) * 31 else (jMonth - 1) * 30 + 6  
val jdn = jDay.toLong() + md + ((epYear * 682) - 110) / 2816 + (epYear - 1) * 365L + (epBase / 2820) * 1029983L + 1948320L  
println(((jdn + 2) %% 7).toInt())  
}  
