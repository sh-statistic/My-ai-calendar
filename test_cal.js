const PERSIAN_EPOCH = 1948320;
const ISLAMIC_EPOCH = 1948439;

function positiveModulo(a, b) { return ((a % b) + b) % b; }
function floorDiv(a, b) { return Math.floor(a / b); }

function persianToJdn(year, month, day) {
    const epbase = year - (year >= 0 ? 474 : 473);
    const epyear = 474 + positiveModulo(epbase, 2820);
    const daysPassed = month <= 7 ? (month - 1) * 31 : 186 + (month - 7) * 30;
    return day + daysPassed + Math.floor((epyear * 682 - 110) / 2816) + (epyear - 1) * 365 + floorDiv(epbase, 2820) * 1029983 + PERSIAN_EPOCH;
}

function isIslamicLeap(year) {
    return ((11 * year + 14) % 30) < 11;
}

function islamicToJdn(year, month, day) {
    return day + Math.ceil(29.5 * (month - 1)) + (year - 1) * 354 + Math.floor((3 + 11 * year) / 30) + ISLAMIC_EPOCH - 1;
}

function jdnToIslamicExact(jdn) {
    let year = Math.floor((30 * (jdn - ISLAMIC_EPOCH) + 10646) / 10631);
    let dayOfYear = jdn - islamicToJdn(year, 1, 1) + 1;
    while (dayOfYear <= 0) {
        year--;
        dayOfYear = jdn - islamicToJdn(year, 1, 1) + 1;
    }
    let month = 1;
    while (month <= 12) {
        const daysInM = (month % 2 === 1) ? 30 : ((month === 12 && isIslamicLeap(year)) ? 30 : 29);
        if (dayOfYear <= daysInM) break;
        dayOfYear -= daysInM;
        month++;
    }
    return { year, month, day: dayOfYear };
}

const jdn = persianToJdn(1405, 11, 4);
console.log("JDN 1405/11/4:", jdn);
console.log("Exact Islamic Date:", jdnToIslamicExact(jdn));
