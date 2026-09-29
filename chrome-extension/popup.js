/**
 * Popup Script for Hamgam Extension (Ultra-Lightweight & Offline)
 */

document.addEventListener("DOMContentLoaded", async () => {
  const solarDateText = document.getElementById("solarDateText");
  const gregorianDateText = document.getElementById("gregorianDateText");
  const eventsList = document.getElementById("eventsList");
  const tasksList = document.getElementById("tasksList");
  const eventsCount = document.getElementById("eventsCount");
  const tasksCount = document.getElementById("tasksCount");
  const noteInput = document.getElementById("noteInput");
  const btnSaveNote = document.getElementById("btnSaveNote");
  const btnSync = document.getElementById("btnSync");
  const syncStatus = document.getElementById("syncStatus");

  // Prayer times elements
  const citySelect = document.getElementById("citySelect");
  const nextPrayerBadge = document.getElementById("nextPrayerBadge");

  // Calculate today's dates
  const todayJalali = PersianDateUtil.getTodayJalali();
  const todayGregorian = PersianDateUtil.getTodayGregorian();
  const jalaliFormatted = PersianDateUtil.formatJalali(todayJalali);
  const dow = PersianDateUtil.getPersianDayOfWeek(todayJalali.year, todayJalali.month, todayJalali.day);

  solarDateText.textContent = `${PersianDateUtil.WEEKDAYS[dow]}، ${PersianDateUtil.toPersianDigits(todayJalali.day)} ${PersianDateUtil.PERSIAN_MONTHS[todayJalali.month - 1]} ${PersianDateUtil.toPersianDigits(todayJalali.year)}`;
  gregorianDateText.textContent = `${PersianDateUtil.GREGORIAN_MONTHS[todayGregorian.month - 1]} ${todayGregorian.day}, ${todayGregorian.year} (${PersianDateUtil.formatGregorian(todayGregorian)})`;

  // Populate Iranian cities dropdown
  if (typeof IRANIAN_CITIES !== 'undefined' && citySelect) {
    citySelect.innerHTML = IRANIAN_CITIES.map(c => `<option value="${c.nameFa}">${c.nameFa}</option>`).join("");
    
    // Get stored city or default to Tehran
    let savedCity = "تهران";
    try {
      if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local) {
        const stored = await chrome.storage.local.get(["selectedCity"]);
        if (stored.selectedCity) savedCity = stored.selectedCity;
      }
    } catch (_) {}
    citySelect.value = savedCity;

    function updatePrayerTimes() {
      const city = IRANIAN_CITIES.find(c => c.nameFa === citySelect.value) || IRANIAN_CITIES[0];
      const times = PrayerTimesEngine.calculate(city.lat, city.lng, new Date());
      const next = PrayerTimesEngine.getNextPrayer(times, new Date());

      document.getElementById("t-fajr").textContent = PersianDateUtil.toPersianDigits(times.fajr);
      document.getElementById("t-sunrise").textContent = PersianDateUtil.toPersianDigits(times.sunrise);
      document.getElementById("t-dhuhr").textContent = PersianDateUtil.toPersianDigits(times.dhuhr);
      document.getElementById("t-sunset").textContent = PersianDateUtil.toPersianDigits(times.sunset);
      document.getElementById("t-maghrib").textContent = PersianDateUtil.toPersianDigits(times.maghrib);
      document.getElementById("t-midnight").textContent = PersianDateUtil.toPersianDigits(times.midnight);

      // Highlight current/next
      ["p-fajr", "p-sunrise", "p-dhuhr", "p-sunset", "p-maghrib", "p-midnight"].forEach(id => {
        document.getElementById(id)?.classList.remove("highlight");
      });
      if (next.name.includes("صبح")) document.getElementById("p-fajr")?.classList.add("highlight");
      else if (next.name.includes("طلوع")) document.getElementById("p-sunrise")?.classList.add("highlight");
      else if (next.name.includes("ظهر")) document.getElementById("p-dhuhr")?.classList.add("highlight");
      else if (next.name.includes("غروب")) document.getElementById("p-sunset")?.classList.add("highlight");
      else if (next.name.includes("مغرب")) document.getElementById("p-maghrib")?.classList.add("highlight");
      else if (next.name.includes("نیمه‌شب")) document.getElementById("p-midnight")?.classList.add("highlight");

      const hours = Math.floor(next.remainingMinutes / 60);
      const mins = next.remainingMinutes % 60;
      const remText = hours > 0 ? `${hours}س و ${mins}د` : `${mins}د`;
      nextPrayerBadge.textContent = `${next.name} (${PersianDateUtil.toPersianDigits(remText)})`;
    }

    updatePrayerTimes();
    citySelect.addEventListener("change", async () => {
      updatePrayerTimes();
      if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local) {
        await chrome.storage.local.set({ selectedCity: citySelect.value });
      }
    });
  }

  // Load and render data from chrome.storage.local
  async function loadData() {
    const data = await SyncClient.getLocalData();

    // Events for today
    const todayEvents = (data.events || []).filter(
      (e) => !e.isDeleted && e.persianDate === jalaliFormatted
    );
    eventsCount.textContent = PersianDateUtil.toPersianDigits(todayEvents.length);

    if (todayEvents.length > 0) {
      eventsList.innerHTML = todayEvents
        .map(
          (e) => `
        <div class="item-card" style="border-right-color: ${e.colorHex || '#d97706'}">
          <div>
            <strong>${e.title}</strong>
            ${e.startTime ? `<div style="font-size: 10px; color: #64748b">${PersianDateUtil.toPersianDigits(e.startTime)}</div>` : ""}
          </div>
          <span style="font-size: 10px; background: #fef3c7; padding: 2px 6px; border-radius: 4px; color: ${e.colorHex || '#d97706'}">${e.category || "رویداد"}</span>
        </div>
      `
        )
        .join("");
    } else {
      eventsList.innerHTML = `<div class="empty-state">رویدادی برای امروز ثبت نشده است.</div>`;
    }

    // Tasks for today or pending
    const pendingTasks = (data.tasks || []).filter((t) => !t.isDeleted);
    tasksCount.textContent = PersianDateUtil.toPersianDigits(pendingTasks.filter((t) => !t.isCompleted).length);

    if (pendingTasks.length > 0) {
      tasksList.innerHTML = pendingTasks
        .slice(0, 5)
        .map(
          (t) => `
        <div class="item-card task-item ${t.isCompleted ? 'completed' : ''}">
          <label style="display: flex; align-items: center; gap: 6px; cursor: pointer;">
            <input type="checkbox" ${t.isCompleted ? "checked" : ""} data-task-id="${t.id}" class="task-checkbox">
            <span>${t.title}</span>
          </label>
          <span style="font-size: 9px; color: #64748b;">${t.priority || "عادی"}</span>
        </div>
      `
        )
        .join("");

      // Bind checkboxes
      document.querySelectorAll(".task-checkbox").forEach((cb) => {
        cb.addEventListener("change", async (ev) => {
          const taskId = ev.target.getAttribute("data-task-id");
          const task = (data.tasks || []).find((x) => x.id === taskId);
          if (task) {
            task.isCompleted = ev.target.checked;
            task.updatedAt = Date.now();
            await SyncClient.saveLocalData(data.events, data.tasks, data.notes);
            loadData();
          }
        });
      });
    } else {
      tasksList.innerHTML = `<div class="empty-state">کاری برای امروز ثبت نشده است.</div>`;
    }
  }

  await loadData();

  // Save Quick Note
  btnSaveNote.addEventListener("click", async () => {
    const text = noteInput.value.trim();
    if (!text) return;

    const data = await SyncClient.getLocalData();
    const newNote = {
      id: "note_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7),
      title: "یادداشت سریع",
      content: text,
      persianDate: jalaliFormatted,
      colorHex: "#FEF3C7",
      isPinned: false,
      updatedAt: Date.now(),
      isDeleted: false
    };

    const notes = data.notes || [];
    notes.unshift(newNote);
    await SyncClient.saveLocalData(data.events, data.tasks, notes);

    noteInput.value = "";
    syncStatus.textContent = "یادداشت ذخیره شد (آفلاین)";
    setTimeout(() => {
      syncStatus.textContent = "آماده همگام‌سازی محلی";
    }, 2500);
  });

  // Sync Button
  btnSync.addEventListener("click", async () => {
    btnSync.disabled = true;
    syncStatus.textContent = "درحال اتصال به گوشی...";
    try {
      const serverUrl = await SyncClient.getServerUrl();
      const result = await SyncClient.syncWithAndroid(serverUrl);
      if (result.success) {
        syncStatus.textContent = `همگام شد! (${PersianDateUtil.toPersianDigits(result.eventsCount)} رویداد)`;
        await loadData();
      } else {
        syncStatus.textContent = "خطا در اتصال: " + (result.error || "سرور پاسخ نداد");
      }
    } catch (e) {
      syncStatus.textContent = "عدم دسترسی به IP گوشی";
    } finally {
      btnSync.disabled = false;
    }
  });
});
