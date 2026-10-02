import math

PERSIAN_EPOCH = 1948320
ISLAMIC_EPOCH = 1948439

def positiveModulo(a, b):
    return ((a % b) + b) % b

def floorDiv(a, b):
    return math.floor(a / b)

def persianToJdn(year, month, day):
    epbase = year - (474 if year >= 0 else 473)
    epyear = 474 + positiveModulo(epbase, 2820)
    
    return day + \
           ((month - 1) * 31 if month <= 7 else (month - 1) * 30 + 1) + \
           math.floor((epyear * 682 - 110) / 2816) + \
           (epyear - 1) * 365 + \
           floorDiv(epbase, 2820) * 1029983 + \
           PERSIAN_EPOCH

def jdnToPersian(jdn):
    depoch = jdn - persianToJdn(475, 1, 1)
    cycle = floorDiv(depoch, 1029983)
    cyear = positiveModulo(depoch, 1029983)
    
    if cyear == 1029982:
        ycycle = 2820
    else:
        aux1 = math.floor(cyear / 366)
        aux2 = cyear % 366
        ycycle = math.floor((2134 * aux1 + 2816 * aux2 + 2815) / 1028522) + aux1 + 1
        
    year = ycycle + 2820 * cycle + 474
    if year <= 0:
        year -= 1
        
    yday = jdn - persianToJdn(year, 1, 1) + 1
    month = math.ceil(yday / 31) if yday <= 186 else math.ceil((yday - 6) / 30)
    day = jdn - persianToJdn(year, month, 1) + 1
    
    return year, month, day

def islamicToJdn(year, month, day):
    return day + \
           math.ceil(29.5001 * (month - 1)) + \
           (year - 1) * 354 + \
           math.floor((3 + 11 * year) / 30) + \
           ISLAMIC_EPOCH - 1

def jdnToIslamic(jdn):
    l = jdn - ISLAMIC_EPOCH + 1
    year = math.floor((30 * l + 10646) / 10631)
    month = math.floor((24 * (l - 29 - islamicToJdn(year, 1, 1) + ISLAMIC_EPOCH) + 23) / 709) + 1
    if month < 1: month = 1
    if month > 12: month = 12
    day = jdn - islamicToJdn(year, month, 1) + 1
    return year, month, day

print("Persian 1405-08-22 to JDN:", persianToJdn(1405, 8, 22))
print("JDN back to Persian:", jdnToPersian(persianToJdn(1405, 8, 22)))

hijri_jdn = islamicToJdn(1448, 6, 3) # Shahadat Zahra 1448
print("Hijri 1448-06-03 to JDN:", hijri_jdn)
print("Which is Persian:", jdnToPersian(hijri_jdn))
