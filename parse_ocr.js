const fs = require('fs');

const text = fs.readFileSync('ocr_text.txt', 'utf8');
const lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);

const persianMonths = [
  "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
  "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
];

// We need a mapping from OCR text to clean month index (1-12)
function getPersianMonthIndex(str) {
  if (str.includes("فروردین")) return 1;
  if (str.includes("اردیبهشت")) return 2;
  if (str.includes("خرداد")) return 3;
  if (str.includes("تير") || str.includes("تیر")) return 4;
  if (str.includes("مرداد")) return 5;
  if (str.includes("شهریور")) return 6;
  if (str.includes("مهر")) return 7;
  if (str.includes("آبان")) return 8;
  if (str.includes("آذر")) return 9;
  if (str.includes("دي") || str.includes("دی")) return 10;
  if (str.includes("بهمن")) return 11;
  if (str.includes("اسفند")) return 12;
  return -1;
}

function getHijriMonthIndex(str) {
  if (str.includes("محرم")) return 1;
  if (str.includes("صفر")) return 2;
  if (str.includes("ربي االول") || str.includes("ربيع االول") || str.includes("ربيع‌االول")) return 3;
  if (str.includes("ربي الثانی") || str.includes("ربيع الثانی") || str.includes("ربيع‌الثانی") || str.includes("ربيع‌الاخر")) return 4;
  if (str.includes("جمادياالولی") || str.includes("جمادی االولی") || str.includes("جمادی‌االولی")) return 5;
  if (str.includes("جماديالثانيه") || str.includes("جمادي الثانی") || str.includes("جمادی‌الثانی") || str.includes("جمادی الثانیه")) return 6;
  if (str.includes("رجب")) return 7;
  if (str.includes("شعبان")) return 8;
  if (str.includes("رمضان")) return 9;
  if (str.includes("شوال")) return 10;
  if (str.includes("ذيالقعده") || str.includes("ذی القعده") || str.includes("ذی‌القعده")) return 11;
  if (str.includes("ذيالحجه") || str.includes("ذی الحجه") || str.includes("ذی‌الحجه")) return 12;
  return -1;
}

let currentPYear = 1405;
let currentPMonth = 1;

let currentHYear = 1447;
let currentHMonth = 10;

const result = {};

for (const line of lines) {
  // Try to match a calendar row
  // Example: شنبه 1 فروردین 1 شوال 1447 21 مارس 2026 عيد سعيد فطر)تعطيل( و آغاز نوروز )تعطيل(
  // Example: یكشنبه 2 " 2 " " 22 " " تعطيل به مناسبت...
  
  const parts = line.split(/\s+/);
  if (parts.length < 5) continue;
  
  const dayOfWeekNames = ["شنبه", "یكشنبه", "دوشنبه", "سهشنبه", "چهارشنبه", "پنجشنبه", "جمعه", "یکشنبه"];
  if (!dayOfWeekNames.includes(parts[0])) continue;
  
  const pDayStr = parts[1];
  const pDay = parseInt(pDayStr);
  if (isNaN(pDay)) continue;

  let index = 2;
  
  if (parts[index] !== '"') {
    const pm = getPersianMonthIndex(parts[index]);
    if (pm !== -1) {
      currentPMonth = pm;
    }
    index++; // skip month name
  } else {
    index++;
  }
  
  const hDayStr = parts[index];
  const hDay = parseInt(hDayStr);
  index++;
  
  if (parts[index] !== '"') {
    const hm = getHijriMonthIndex(parts[index]);
    if (hm !== -1) {
      currentHMonth = hm;
    }
    index++;
  } else {
    index++;
  }
  
  const hYearStr = parts[index];
  if (hYearStr !== '"') {
    const hy = parseInt(hYearStr);
    if (!isNaN(hy)) {
        currentHYear = hy;
    }
  }
  index++;
  
  // Skip gregorian day and month and year
  // e.g. 21 مارس 2026 or " " "
  let gDay = parts[index]; index++;
  let gMonth = parts[index]; index++;
  if (parts[index] && parts[index].match(/^\d{4}$/)) index++; // year
  else if (parts[index] === '"') index++;
  
  const occasionsText = parts.slice(index).join(" ");
  
  // Check if holiday
  const isHoliday = occasionsText.includes("(تعطيل)") || occasionsText.includes("تعطيل") || occasionsText.includes("عيد سعيد فطر)تعطيل(");
  // We should also check Fridays.
  const isFriday = parts[0] === "جمعه";
  
  if (!result[currentPMonth]) result[currentPMonth] = {};
  
  result[currentPMonth][pDay] = {
    hYear: currentHYear,
    hMonth: currentHMonth,
    hDay: hDay,
    isHoliday: isHoliday || isFriday,
    occasions: occasionsText.trim() ? [occasionsText.trim().replace(/و /g, " - ")] : []
  };
}

fs.writeFileSync('c:\\Users\\Ai\\Desktop\\My-ai-calendar-main\\app\\src\\main\\assets\\occasions_1405.json', JSON.stringify(result, null, 2), 'utf8');
fs.writeFileSync('c:\\Users\\Ai\\Desktop\\My-ai-calendar-main\\chrome-extension\\occasions_1405.json', JSON.stringify(result, null, 2), 'utf8');

console.log("JSON generated successfully!");
