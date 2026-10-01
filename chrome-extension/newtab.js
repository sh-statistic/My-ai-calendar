/**
 * Hamgam New Tab Full-Screen Dashboard (Ultra-Lightweight & Offline)
 */

document.addEventListener("DOMContentLoaded", async () => {
  // Elements
  const headerMonthTitle = document.getElementById("headerMonthTitle");
  const headerGregorianSpan = document.getElementById("headerGregorianSpan");
  const btnPrevMonth = document.getElementById("btnPrevMonth");
  const btnNextMonth = document.getElementById("btnNextMonth");
  const btnToday = document.getElementById("btnToday");
  const calendarGrid = document.getElementById("calendarGrid");
  const selectedDateTitle = document.getElementById("selectedDateTitle");
  const selectedGregorianDate = document.getElementById("selectedGregorianDate");
  const dayEventsList = document.getElementById("dayEventsList");
  const newEventTitle = document.getElementById("newEventTitle");
  const newEventTime = document.getElementById("newEventTime");
  const btnAddEvent = document.getElementById("btnAddEvent");

  const newtabTaskList = document.getElementById("newtabTaskList");
  const newTaskInput = document.getElementById("newTaskInput");
  const btnAddTask = document.getElementById("btnAddTask");
  const pendingCountBadge = document.getElementById("pendingCountBadge");

  const newtabNotesGrid = document.getElementById("newtabNotesGrid");
  const newNoteInput = document.getElementById("newNoteInput");
  const btnAddNote = document.getElementById("btnAddNote");

  const inputServerIp = document.getElementById("inputServerIp");
  const btnSyncNow = document.getElementById("btnSyncNow");
  const btnBleSync = document.getElementById("btnBleSync");
  const serverStatusDot = document.getElementById("serverStatusDot");

  // Prayer Dashboard Elements
  const dashCitySelect = document.getElementById("dashCitySelect");
  const dashNextPrayerBadge = document.getElementById("dashNextPrayerBadge");

  // State
  const todayJalali = PersianDateUtil.getTodayJalali();
  let currentYear = todayJalali.year;
  let currentMonth = todayJalali.month;
  let selectedDate = { ...todayJalali };

  let localEvents = [];
  let localTasks = [];
  let localNotes = [];

  // Init Server IP
  const savedUrl = await SyncClient.getServerUrl();
  inputServerIp.value = savedUrl;

  const dashQrImg = document.getElementById("dashQrImg");
  const dashQrUrl = document.getElementById("dashQrUrl");
  function updateQrDisplay() {
    if (dashQrImg && inputServerIp.value) {
      dashQrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=140x140&data=${encodeURIComponent(inputServerIp.value)}`;
      if (dashQrUrl) dashQrUrl.textContent = inputServerIp.value;
    }
  }
  updateQrDisplay();

  inputServerIp.addEventListener("change", async () => {
    await SyncClient.setServerUrl(inputServerIp.value);
    updateQrDisplay();
    checkServerConnection();
  });

  async function checkServerConnection() {
    const status = await SyncClient.pingServer(inputServerIp.value);
    if (status) {
      serverStatusDot.classList.add("online");
      serverStatusDot.title = `سرور اندروید متصل است (${status.app || 'Hamgam'})`;
    } else {
      serverStatusDot.classList.remove("online");
      serverStatusDot.title = "عدم ارتباط با سرور محلی گوشی";
    }
  }

  // Initialize Offline Prayer Times
  if (typeof IRANIAN_CITIES !== 'undefined' && dashCitySelect) {
    dashCitySelect.innerHTML = IRANIAN_CITIES.map(c => `<option value="${c.nameFa}">${c.nameFa}</option>`).join("");
    
    let savedCity = "تهران";
    try {
      if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local) {
        const stored = await chrome.storage.local.get(["selectedCity"]);
        if (stored.selectedCity) savedCity = stored.selectedCity;
      }
    } catch (_) {}
    dashCitySelect.value = savedCity;

    function updateDashPrayerTimes() {
      const city = IRANIAN_CITIES.find(c => c.nameFa === dashCitySelect.value) || IRANIAN_CITIES[0];
      const times = PrayerTimesEngine.calculate(city.lat, city.lng, new Date());
      const next = PrayerTimesEngine.getNextPrayer(times, new Date());

      document.getElementById("dt-fajr").textContent = PersianDateUtil.toPersianDigits(times.fajr);
      document.getElementById("dt-sunrise").textContent = PersianDateUtil.toPersianDigits(times.sunrise);
      document.getElementById("dt-dhuhr").textContent = PersianDateUtil.toPersianDigits(times.dhuhr);
      document.getElementById("dt-sunset").textContent = PersianDateUtil.toPersianDigits(times.sunset);
      document.getElementById("dt-maghrib").textContent = PersianDateUtil.toPersianDigits(times.maghrib);
      document.getElementById("dt-midnight").textContent = PersianDateUtil.toPersianDigits(times.midnight);

      ["dc-fajr", "dc-sunrise", "dc-dhuhr", "dc-sunset", "dc-maghrib", "dc-midnight"].forEach(id => {
        document.getElementById(id)?.classList.remove("active");
      });
      if (next.name.includes("صبح")) document.getElementById("dc-fajr")?.classList.add("active");
      else if (next.name.includes("طلوع")) document.getElementById("dc-sunrise")?.classList.add("active");
      else if (next.name.includes("ظهر")) document.getElementById("dc-dhuhr")?.classList.add("active");
      else if (next.name.includes("غروب")) document.getElementById("dc-sunset")?.classList.add("active");
      else if (next.name.includes("مغرب")) document.getElementById("dc-maghrib")?.classList.add("active");
      else if (next.name.includes("نیمه‌شب")) document.getElementById("dc-midnight")?.classList.add("active");

      const hours = Math.floor(next.remainingMinutes / 60);
      const mins = next.remainingMinutes % 60;
      const remText = hours > 0 ? `${hours} ساعت و ${mins} دقیقه` : `${mins} دقیقه`;
      dashNextPrayerBadge.textContent = `${next.name}: ${PersianDateUtil.toPersianDigits(next.time)} (${PersianDateUtil.toPersianDigits(remText)})`;
    }

    updateDashPrayerTimes();
    dashCitySelect.addEventListener("change", async () => {
      updateDashPrayerTimes();
      if (typeof chrome !== 'undefined' && chrome.storage && chrome.storage.local) {
        await chrome.storage.local.set({ selectedCity: dashCitySelect.value });
      }
    });
  }

  // Load from local storage
  async function refreshData() {
    const data = await SyncClient.getLocalData();
    localEvents = data.events || [];
    localTasks = data.tasks || [];
    localNotes = data.notes || [];

    renderCalendar();
    renderTasks();
    renderNotes();
    renderSelectedDayEvents();
  }

  // Calendar Rendering
  function renderCalendar() {
    const daysInMonth = PersianDateUtil.getDaysInJalaliMonth(currentYear, currentMonth);
    const firstDow = PersianDateUtil.getPersianDayOfWeek(currentYear, currentMonth, 1);

    headerMonthTitle.textContent = `${PersianDateUtil.PERSIAN_MONTHS[currentMonth - 1]} ${PersianDateUtil.toPersianDigits(currentYear)}`;

    const firstG = PersianDateUtil.jalaliToGregorian(currentYear, currentMonth, 1);
    const lastG = PersianDateUtil.jalaliToGregorian(currentYear, currentMonth, daysInMonth);
    headerGregorianSpan.textContent = `(${firstG.day} ${PersianDateUtil.GREGORIAN_MONTHS[firstG.month - 1]} - ${lastG.day} ${PersianDateUtil.GREGORIAN_MONTHS[lastG.month - 1]} ${firstG.year})`;

    calendarGrid.innerHTML = "";

    // Headers
    PersianDateUtil.WEEKDAY_ABBR.forEach((abbr, idx) => {
      const headerDiv = document.createElement("div");
      headerDiv.className = `weekday-header ${idx === 6 ? "friday" : ""}`;
      headerDiv.textContent = abbr;
      calendarGrid.appendChild(headerDiv);
    });

    // Empty lead cells
    for (let i = 0; i < firstDow; i++) {
      const empty = document.createElement("div");
      calendarGrid.appendChild(empty);
    }

    // Days
    for (let day = 1; day <= daysInMonth; day++) {
      const cellJalali = { year: currentYear, month: currentMonth, day };
      const cellGregorian = PersianDateUtil.jalaliToGregorian(currentYear, currentMonth, day);
      const formattedJalali = PersianDateUtil.formatJalali(cellJalali);

      const isToday = currentYear === todayJalali.year && currentMonth === todayJalali.month && day === todayJalali.day;
      const isSelected = currentYear === selectedDate.year && currentMonth === selectedDate.month && day === selectedDate.day;

      const hasEvents = localEvents.some((e) => !e.isDeleted && e.persianDate === formattedJalali);
      const hasTasks = localTasks.some((t) => !t.isDeleted && t.persianDueDate === formattedJalali);

      const dow = (firstDow + day - 1) % 7;
      const isFriday = dow === 6;
      const isHoliday = isFriday || (typeof PersianOccasions !== "undefined" && PersianOccasions.isHoliday(currentMonth, day));

      const cell = document.createElement("div");
      cell.className = `calendar-cell ${isSelected ? "selected" : ""} ${isToday ? "today" : ""} ${isHoliday ? "holiday" : ""}`;
      cell.innerHTML = `
        <span class="solar-day" style="${isHoliday ? "color: var(--danger); font-weight: bold;" : ""}">${PersianDateUtil.toPersianDigits(day)}</span>
        <span class="gregorian-day">${cellGregorian.day}</span>
        ${hasEvents || hasTasks ? '<span class="event-dot"></span>' : ""}
      `;

      cell.addEventListener("click", () => {
        selectedDate = { ...cellJalali };
        renderCalendar();
        renderSelectedDayEvents();
      });

      calendarGrid.appendChild(cell);
    }
  }

  function renderSelectedDayEvents() {
    const formatted = PersianDateUtil.formatJalali(selectedDate);
    const selG = PersianDateUtil.jalaliToGregorian(selectedDate.year, selectedDate.month, selectedDate.day);
    const dowIdx = PersianDateUtil.getPersianDayOfWeek(selectedDate.year, selectedDate.month, selectedDate.day);
    const dowName = PersianDateUtil.WEEKDAYS[dowIdx];

    selectedDateTitle.textContent = `${dowName} ${PersianDateUtil.toPersianDigits(formatted)}`;
    selectedGregorianDate.textContent = `تاریخ میلادی: ${selG.day} ${PersianDateUtil.GREGORIAN_MONTHS[selG.month - 1]} ${selG.year}`;

    const occasions = typeof PersianOccasions !== "undefined" ? PersianOccasions.getOccasions(selectedDate.month, selectedDate.day) : [];
    let occasionsHtml = "";
    if (occasions.length > 0) {
      occasionsHtml = `
        <div style="background: var(--bg-card); border: 1px solid var(--border-color); border-radius: 8px; padding: 8px 12px; margin-bottom: 8px;">
          ${occasions.map(o => `
            <div style="display: flex; align-items: center; gap: 6px; font-size: 11px; margin-bottom: 2px;">
              <span style="font-size: 9px; padding: 1px 6px; border-radius: 4px; font-weight: bold; background: ${o.isHoliday ? 'var(--danger-light, #fee2e2)' : '#fef3c7'}; color: ${o.isHoliday ? 'var(--danger, #dc2626)' : '#b45309'};">${o.isHoliday ? 'تعطیل رسمی' : 'مناسبت'}</span>
              <span>${o.title}</span>
            </div>
          `).join("")}
        </div>
      `;
    }

    const dayEvents = localEvents.filter((e) => !e.isDeleted && e.persianDate === formatted);

    let eventsHtml = "";
    if (dayEvents.length === 0) {
      eventsHtml = `<p style="color: var(--text-muted); font-size: 12px;">رویدادی برای این روز ثبت نشده است.</p>`;
    } else {
      eventsHtml = dayEvents
        .map(
          (e) => `
        <div style="background: var(--bg-main); border: 1px solid var(--border-color); padding: 8px 12px; border-radius: 8px; border-right: 4px solid ${e.colorHex || 'var(--primary)'}; display: flex; justify-content: space-between; align-items: center;">
          <div>
            <strong>${e.title}</strong>
            ${e.description ? `<p style="font-size: 11px; color: var(--text-muted);">${e.description}</p>` : ""}
          </div>
          <span style="font-size: 11px; color: var(--primary);">${e.startTime ? PersianDateUtil.toPersianDigits(e.startTime) : ""}</span>
        </div>
      `
        )
        .join("");
    }

    dayEventsList.innerHTML = occasionsHtml + eventsHtml;
  }

  // Navigation Listeners
  btnPrevMonth.addEventListener("click", () => {
    if (currentMonth === 1) {
      currentMonth = 12;
      currentYear -= 1;
    } else {
      currentMonth -= 1;
    }
    renderCalendar();
  });

  btnNextMonth.addEventListener("click", () => {
    if (currentMonth === 12) {
      currentMonth = 1;
      currentYear += 1;
    } else {
      currentMonth += 1;
    }
    renderCalendar();
  });

  btnToday.addEventListener("click", () => {
    currentYear = todayJalali.year;
    currentMonth = todayJalali.month;
    selectedDate = { ...todayJalali };
    renderCalendar();
    renderSelectedDayEvents();
  });

  // Add Event
  btnAddEvent.addEventListener("click", async () => {
    const title = newEventTitle.value.trim();
    if (!title) return;

    const time = newEventTime.value;
    const formattedDate = PersianDateUtil.formatJalali(selectedDate);
    const gDate = PersianDateUtil.jalaliToGregorian(selectedDate.year, selectedDate.month, selectedDate.day);

    const newEv = {
      id: "ev_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7),
      title,
      description: "",
      persianDate: formattedDate,
      gregorianDate: PersianDateUtil.formatGregorian(gDate),
      startTime: time,
      endTime: "",
      category: "کاری",
      colorHex: "#F59E0B",
      updatedAt: Date.now(),
      isDeleted: false
    };

    localEvents.push(newEv);
    await SyncClient.saveLocalData(localEvents, localTasks, localNotes);
    newEventTitle.value = "";
    renderCalendar();
    renderSelectedDayEvents();
  });

  // Render Tasks
  function renderTasks() {
    const activeTasks = localTasks.filter((t) => !t.isDeleted);
    const pending = activeTasks.filter((t) => !t.isCompleted).length;
    pendingCountBadge.textContent = `${PersianDateUtil.toPersianDigits(pending)} کار در انتظار`;

    if (activeTasks.length === 0) {
      newtabTaskList.innerHTML = `<p style="color: var(--text-muted); font-size: 12px; padding: 12px; text-align: center;">هیچ کاری در فهرست وجود ندارد.</p>`;
      return;
    }

    newtabTaskList.innerHTML = activeTasks
      .map(
        (t) => `
      <div class="task-row ${t.isCompleted ? 'done' : ''}">
        <label style="display: flex; align-items: center; gap: 8px; cursor: pointer;">
          <input type="checkbox" ${t.isCompleted ? "checked" : ""} data-id="${t.id}" class="task-check">
          <span>${t.title}</span>
        </label>
        <span style="font-size: 10px; color: var(--text-muted);">${t.persianDueDate ? PersianDateUtil.toPersianDigits(t.persianDueDate) : ""}</span>
      </div>
    `
      )
      .join("");

    document.querySelectorAll(".task-check").forEach((cb) => {
      cb.addEventListener("change", async (ev) => {
        const id = ev.target.getAttribute("data-id");
        const item = localTasks.find((x) => x.id === id);
        if (item) {
          item.isCompleted = ev.target.checked;
          item.updatedAt = Date.now();
          await SyncClient.saveLocalData(localEvents, localTasks, localNotes);
          renderTasks();
        }
      });
    });
  }

  // Add Task
  btnAddTask.addEventListener("click", async () => {
    const text = newTaskInput.value.trim();
    if (!text) return;

    const newTask = {
      id: "task_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7),
      title: text,
      isCompleted: false,
      persianDueDate: PersianDateUtil.formatJalali(selectedDate),
      priority: "متوسط",
      category: "عمومی",
      updatedAt: Date.now(),
      isDeleted: false
    };

    localTasks.unshift(newTask);
    await SyncClient.saveLocalData(localEvents, localTasks, localNotes);
    newTaskInput.value = "";
    renderTasks();
  });

  // Render Notes
  function renderNotes() {
    const activeNotes = localNotes.filter((n) => !n.isDeleted);
    if (activeNotes.length === 0) {
      newtabNotesGrid.innerHTML = `<p style="grid-column: span 2; color: var(--text-muted); font-size: 12px; text-align: center; padding: 12px;">هنوز یادداشتی ثبت نشده است.</p>`;
      return;
    }

    newtabNotesGrid.innerHTML = activeNotes
      .map(
        (n) => `
      <div class="sticky-note" style="background: ${n.colorHex || '#FEF3C7'};">
        <strong>${n.title || 'یادداشت'}</strong>
        <p>${n.content}</p>
        <span style="font-size: 9px; color: #64748b; margin-top: 4px; display: block;">${n.persianDate || ''}</span>
      </div>
    `
      )
      .join("");
  }

  // Add Note
  btnAddNote.addEventListener("click", async () => {
    const text = newNoteInput.value.trim();
    if (!text) return;

    const colors = ["#FEF3C7", "#E0F2FE", "#DCFCE7", "#FCE7F3"];
    const note = {
      id: "note_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7),
      title: "یادداشت جدید",
      content: text,
      persianDate: PersianDateUtil.formatJalali(todayJalali),
      colorHex: colors[Math.floor(Math.random() * colors.length)],
      isPinned: false,
      updatedAt: Date.now(),
      isDeleted: false
    };

    localNotes.unshift(note);
    await SyncClient.saveLocalData(localEvents, localTasks, localNotes);
    newNoteInput.value = "";
    renderNotes();
  });

  // Wi-Fi / Hotspot Sync Now Button
  btnSyncNow.addEventListener("click", async () => {
    btnSyncNow.disabled = true;
    btnSyncNow.textContent = "درحال تبادل داده...";
    const res = await SyncClient.syncWithAndroid(inputServerIp.value);
    if (res.success) {
      btnSyncNow.textContent = "همگام‌سازی موفق ✓";
      serverStatusDot.classList.add("online");
      await refreshData();
    } else {
      btnSyncNow.textContent = "خطا در اتصال ✕";
      serverStatusDot.classList.remove("online");
      alert(`همگام‌سازی ناموفق بود:\n${res.error || "سرور در آدرس وارد شده پاسخگو نیست. مطمئن شوید گوشی به همان شبکه وای‌فای یا هات‌اسپات متصل است و سرور در برنامه اندروید روشن است."}`);
    }
    setTimeout(() => {
      btnSyncNow.disabled = false;
      btnSyncNow.textContent = "همگام‌سازی فوری ⟳";
    }, 2500);
  });

  // Bluetooth BLE Sync Button
  btnBleSync.addEventListener("click", async () => {
    try {
      btnBleSync.disabled = true;
      btnBleSync.textContent = "جستجوی بلوتوث...";
      const res = await SyncClient.syncWithBluetooth();
      alert(`اتصال بلوتوث موفق به ${res.deviceName}`);
    } catch (e) {
      alert("خطا در همگام‌سازی بلوتوث: " + e.message);
    } finally {
      btnBleSync.disabled = false;
      btnBleSync.textContent = "بلوتوث BLE";
    }
  });

  // Initial load
  await refreshData();
  checkServerConnection();
  setInterval(checkServerConnection, 15000);
});
