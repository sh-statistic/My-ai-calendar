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

function islamicToJdn(year, month, day) {
    return day + Math.ceil(29.5001 * (month - 1)) + (year - 1) * 354 + Math.floor((3 + 11 * year) / 30) + ISLAMIC_EPOCH - 1;
}

function jdnToIslamic_js(jdn) {
    const l = jdn - ISLAMIC_EPOCH + 1;
    const year = Math.floor((30 * l + 10646) / 10631);
    let month = Math.floor((24 * (l - 29 - islamicToJdn(year, 1, 1) + ISLAMIC_EPOCH) + 23) / 709) + 1;
    if (month < 1) month = 1;
    if (month > 12) month = 12;
    const day = jdn - islamicToJdn(year, month, 1) + 1;
    return { year, month, day };
}

function jdnToIslamic_kt(jdn) {
    const l = jdn - ISLAMIC_EPOCH + 1;
    const year = Math.floor((30 * l + 10646) / 10631);
    let month = Math.ceil((l - 29 - islamicToJdn(year, 1, 1) + ISLAMIC_EPOCH) / 29.5) + 1;
    if (month < 1) month = 1;
    if (month > 12) month = 12;
    const day = jdn - islamicToJdn(year, month, 1) + 1;
    return { year, month, day };
}

const jdn = persianToJdn(1405, 11, 4);
console.log("JDN:", jdn);
console.log("JS jdnToIslamic:", jdnToIslamic_js(jdn));
console.log("KT jdnToIslamic:", jdnToIslamic_kt(jdn));
