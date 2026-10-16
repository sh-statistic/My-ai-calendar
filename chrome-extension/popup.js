/**
 * Popup Script for Hamgam Extension (Ultra-Lightweight & Offline)
 */

document.addEventListener("DOMContentLoaded", async () => {
  const solarDateText = document.getElementById("solarDateText");
  const gregorianDateText = document.getElementById("gregorianDateText");
  const occasionBadge = document.getElementById("occasionBadge");
  const eventsList = document.getElementById("eventsList");
  const tasksList = document.getElementById("tasksList");
  const eventsCount = document.getElementById("eventsCount");
  const tasksCount = document.getElementById("tasksCount");
  const noteInput = document.getElementById("noteInput");
  const btnSaveNote = document.getElementById("btnSaveNote");
  const btnSync = document.getElementById("btnSync");
  const btnFullSync = document.getElementById("btnFullSync");
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
  gregorianDateText.textContent = `${PersianDateUtil.GREGORIAN_MONTHS[todayGregorian.month - 1]} ${todayGregorian.day}, ${todayGregorian.year}`;

  // Check Occasions / Holidays
  if (dow === 6) { // Friday
    occasionBadge.style.display = "inline-block";
    occasionBadge.textContent = "تعطیل رسمی (جمعه)";
  }

  // Populate Iranian cities dropdown
  if (typeof IRANIAN_CITIES !== 'undefined' && citySelect) {
    citySelect.innerHTML = IRANIAN_CITIES.map(c => `<option value="${c.nameFa}">${c.nameFa}</option>`).join("");
    
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

      const hours = Math.floor(next.remainingMinutes / 60);
      const mins = next.remainingMinutes % 60;
      const remText = hours > 0 ? `${hours}س و ${mins}د` : `${mins}د`;
      if (nextPrayerBadge) {
        nextPrayerBadge.textContent = `${next.name} (${PersianDateUtil.toPersianDigits(remText)})`;
      }
    }

    updatePrayerTimes();
    setInterval(updatePrayerTimes, 60000);
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
      (e) => !e.isDeleted && PersianDateUtil.normalizePersianDate(e.persianDate) === jalaliFormatted
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

    // Tasks Scope Filter
    const tabTodayTasks = document.getElementById("tabTodayTasks");
    const tabAllTasks = document.getElementById("tabAllTasks");
    let currentTaskScope = "today";

    if (tabTodayTasks && !tabTodayTasks.dataset.bound) {
      tabTodayTasks.dataset.bound = "true";
      tabTodayTasks.addEventListener("click", () => {
        currentTaskScope = "today";
        tabTodayTasks.classList.add("active");
        if (tabAllTasks) tabAllTasks.classList.remove("active");
        renderTasksList();
      });
    }
    if (tabAllTasks && !tabAllTasks.dataset.bound) {
      tabAllTasks.dataset.bound = "true";
      tabAllTasks.addEventListener("click", () => {
        currentTaskScope = "all";
        tabAllTasks.classList.add("active");
        if (tabTodayTasks) tabTodayTasks.classList.remove("active");
        renderTasksList();
      });
    }

    function renderTasksList() {
      const allActive = (data.tasks || []).filter((t) => !t.isDeleted);
      let tasksToShow = allActive;

      if (currentTaskScope === "today") {
        tasksToShow = allActive.filter(
          (t) => PersianDateUtil.normalizePersianDate(t.persianDueDate) === jalaliFormatted
        );
      }

      const pendingCount = tasksToShow.filter((t) => !t.isCompleted).length;
      tasksCount.textContent = PersianDateUtil.toPersianDigits(pendingCount);

      if (tasksToShow.length > 0) {
        tasksList.innerHTML = tasksToShow
          .slice(0, 8)
          .map(
            (t) => {
              const priority = t.priority || "متوسط";
              const pClass = priority === "بالا" ? "priority-high" : (priority === "پایین" ? "priority-low" : "priority-medium");
              const showDate = currentTaskScope === "all" && t.persianDueDate;
              return `
              <div class="item-card task-item ${t.isCompleted ? 'completed' : ''} ${pClass}">
                <label style="display: flex; align-items: center; gap: 6px; cursor: pointer; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1;">
                  <input type="checkbox" ${t.isCompleted ? "checked" : ""} data-task-id="${t.id}" class="task-checkbox">
                  <span style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${t.title}</span>
                  ${showDate ? `<span style="font-size: 9px; color: #64748b; margin-right: 4px;">(${PersianDateUtil.toPersianDigits(t.persianDueDate)})</span>` : ""}
                </label>
                <span style="font-size: 9px; font-weight: bold; color: ${priority === 'بالا' ? '#ef4444' : (priority === 'پایین' ? '#10b981' : '#f59e0b')}; margin-right: 4px;">${priority}</span>
              </div>
            `;
            }
          )
          .join("");

        // Bind checkboxes
        tasksList.querySelectorAll(".task-checkbox").forEach((cb) => {
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
        const emptyMsg = currentTaskScope === "today" ? "کاری برای امروز ثبت نشده است." : "هیچ وظیفه‌ای ثبت نشده است.";
        tasksList.innerHTML = `<div class="empty-state">${emptyMsg}</div>`;
      }
    }

    renderTasksList();

    // Notes
    const popupNotesList = document.getElementById("popupNotesList");
    const notesCount = document.getElementById("notesCount");
    let activeNotes = (data.notes || []).filter((n) => !n.isDeleted);
    if (notesCount) notesCount.textContent = PersianDateUtil.toPersianDigits(activeNotes.length);

    // مرتب‌سازی: یادداشت‌های پین‌شده در اولویت قرار می‌گیرند
    activeNotes.sort((a, b) => {
      if (a.isPinned && !b.isPinned) return -1;
      if (!a.isPinned && b.isPinned) return 1;
      return (b.updatedAt || 0) - (a.updatedAt || 0);
    });

    if (popupNotesList) {
      if (activeNotes.length > 0) {
        popupNotesList.innerHTML = activeNotes
          .slice(0, 5)
          .map(
            (n) => `
          <div class="item-card" style="border-right-color: ${n.colorHex || '#f59e0b'}; background: ${n.isPinned ? '#FEF08A' : '#fefce8'};">
            <div>
              <strong>${n.isPinned ? '📌 ' : ''}${n.title || "یادداشت"}</strong>
              <div style="font-size: 10px; color: #854d0e; white-space: pre-wrap;">${n.content}</div>
            </div>
            ${n.persianDate ? `<span style="font-size: 9px; color: #a16207;">${PersianDateUtil.toPersianDigits(n.persianDate)}</span>` : ""}
          </div>
        `
          )
          .join("");
      } else {
        popupNotesList.innerHTML = `<div class="empty-state">یادداشتی ذخیره نشده است.</div>`;
      }
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
    await loadData();
    setTimeout(() => {
      syncStatus.textContent = "آماده همگام‌سازی محلی";
    }, 2500);
  });

  // Save Quick Task in Popup
  const popupTaskInput = document.getElementById("popupTaskInput");
  const popupTaskPriority = document.getElementById("popupTaskPriority");
  const btnPopupAddTask = document.getElementById("btnPopupAddTask");
  if (btnPopupAddTask && popupTaskInput) {
    btnPopupAddTask.addEventListener("click", async () => {
      const text = popupTaskInput.value.trim();
      if (!text) return;
      const priorityVal = popupTaskPriority ? popupTaskPriority.value : "متوسط";

      const data = await SyncClient.getLocalData();
      const newTask = {
        id: "task_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7),
        title: text,
        isCompleted: false,
        persianDueDate: jalaliFormatted,
        priority: priorityVal,
        category: "عمومی",
        updatedAt: Date.now(),
        isDeleted: false
      };

      const tasks = data.tasks || [];
      tasks.unshift(newTask);
      await SyncClient.saveLocalData(data.events, tasks, data.notes);

      popupTaskInput.value = "";
      syncStatus.textContent = "کار ذخیره شد (آفلاین)";
      await loadData();
      setTimeout(() => {
        syncStatus.textContent = "آماده همگام‌سازی محلی";
      }, 2500);
    });
  }

  // Wi-Fi Sync Button
  btnSync.addEventListener("click", async () => {
    btnSync.disabled = true;
    syncStatus.textContent = "درحال شناسایی گوشی...";
    try {
      let result = await SyncClient.syncWithAndroid();

      if (!result.success) {
        const savedUrl = await SyncClient.getServerUrl();
        const manualUrl = prompt(
          "اتصال خودکار به گوشی برقرار نشد.\nلطفاً آدرس IP سروری که در گوشی نمایش داده شده را وارد کنید:\n(مثلاً: http://192.168.43.1:8080)",
          savedUrl || "http://192.168.43.1:8080"
        );
        if (manualUrl && manualUrl.trim()) {
          const cleanManual = SyncClient.cleanServerUrl(manualUrl);
          syncStatus.textContent = "درحال اتصال...";
          await SyncClient.setServerUrl(cleanManual);
          result = await SyncClient.syncWithAndroid(cleanManual);
        }
      }

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

  // Full Sync Page Button (باز کردن صفحه کامل همگام‌سازی در تب جدید)
  if (btnFullSync) {
    btnFullSync.addEventListener("click", () => {
      if (typeof chrome !== "undefined" && chrome.tabs && chrome.tabs.create) {
        chrome.tabs.create({ url: "sync.html" });
      } else {
        window.open("sync.html", "_blank");
      }
    });
  }
});
