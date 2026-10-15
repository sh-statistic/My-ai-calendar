/**
 * Hamgam Google New Tab with Windows 7 Draggable Aero Glass Gadgets
 * 100% Exact match to official Chrome UI with Dynamic Google User Profile & Daily Doodle
 */

document.addEventListener("DOMContentLoaded", async () => {
  // =========================================================================
  // 1. DYNAMIC USER GOOGLE AVATAR & DAILY DOODLE
  // =========================================================================
  const avatarImg = document.getElementById("googleAvatarImg");
  const avatarText = document.getElementById("avatarTextContainer");
  const mainLogo = document.getElementById("mainGoogleLogo");

  async function loadUserGoogleProfileAndDoodle() {
    // 1. Load cached avatar from chrome.storage.local
    if (typeof chrome !== "undefined" && chrome.storage && chrome.storage.local) {
      try {
        const stored = await chrome.storage.local.get(["userGoogleAvatar"]);
        if (stored.userGoogleAvatar && avatarImg && avatarText) {
          avatarImg.src = stored.userGoogleAvatar;
          avatarImg.style.display = "block";
          avatarText.style.display = "none";
        }
      } catch (_) {}
    }

    // 2. Fetch official Google page to dynamically read user's profile photo and daily doodle
    try {
      const res = await fetch("https://www.google.com", { credentials: "include" });
      if (res.ok) {
        const html = await res.text();

        // Dynamically find user's Google account avatar URL
        // Google uses: https://lh3.googleusercontent.com/ogw/... or https://lh3.googleusercontent.com/a/...
        const avatarMatch = html.match(/https:\/\/lh3\.googleusercontent\.com\/(?:ogw|a)\/[^"'\s<>&]+/);
        if (avatarMatch && avatarImg && avatarText) {
          const freshAvatar = avatarMatch[0].split("=")[0] + "=s64-p";
          avatarImg.src = freshAvatar;
          avatarImg.style.display = "block";
          avatarText.style.display = "none";
          if (typeof chrome !== "undefined" && chrome.storage && chrome.storage.local) {
            chrome.storage.local.set({ userGoogleAvatar: freshAvatar });
          }
        }

        // Dynamically find Google's Daily Doodle if active today
        const doodleMatch = html.match(/(?:https:\/\/www\.google\.com)?\/logos\/doodles\/[0-9]+\/[^"'\s<>]+\.(?:png|gif|webp|jpg)/i);
        if (doodleMatch && mainLogo) {
          const doodleUrl = doodleMatch[0].startsWith("http") ? doodleMatch[0] : "https://www.google.com" + doodleMatch[0];
          mainLogo.src = doodleUrl;
          mainLogo.classList.add("is-doodle");
        }
      }
    } catch (_) {
      // Offline fallback: retains standard logo and cached profile
    }
  }

  loadUserGoogleProfileAndDoodle();

  // =========================================================================
  // 2. GOOGLE SEARCH, APPS DRAWER & LIVE SHORTCUTS
  // =========================================================================
  const searchInput = document.getElementById("googleSearchInput");
  const btnSearchAdd = document.getElementById("btnSearchAdd");
  const btnMic = document.getElementById("btnMicSearch");
  const btnLens = document.getElementById("btnLensSearch");
  const btnAiMode = document.getElementById("btnAiMode");
  const chipDoMoreAi = document.getElementById("chipDoMoreAi");
  const chipCreateImages = document.getElementById("chipCreateImages");
  const shortcutsContainer = document.getElementById("shortcutsContainer");
  const btnGoogleApps = document.getElementById("btnGoogleApps");
  const googleAppsDropdown = document.getElementById("googleAppsDropdown");

  function performGoogleSearch(query) {
    const q = (query || "").trim();
    if (q) {
      window.location.href = `https://www.google.com/search?q=${encodeURIComponent(q)}`;
    } else {
      window.location.href = "https://www.google.com";
    }
  }

  if (searchInput) {
    searchInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        performGoogleSearch(searchInput.value);
      }
    });
  }

  // هدایت مستقیم به صفحه اصلی گوگل: https://www.google.com/
  const GOOGLE_HOME_PAGE = "https://www.google.com/";

  // 1. کلیک روی دکمه '+' نوار جستجو -> انتقال مستقیم به سایت https://www.google.com/
  if (btnSearchAdd) {
    btnSearchAdd.addEventListener("click", () => {
      window.location.href = GOOGLE_HOME_PAGE;
    });
  }

  // 2. کلیک روی دکمه 'AI Mode' -> انتقال مستقیم به سایت https://www.google.com/
  if (btnAiMode) {
    btnAiMode.addEventListener("click", () => {
      window.location.href = GOOGLE_HOME_PAGE;
    });
  }

  // 3. کلیک روی چیپ 'Do more with AI Mode' -> انتقال مستقیم به سایت https://www.google.com/
  if (chipDoMoreAi) {
    chipDoMoreAi.addEventListener("click", () => {
      window.location.href = GOOGLE_HOME_PAGE;
    });
  }

  // 4. کلیک روی چیپ 'Create images' -> انتقال مستقیم به سایت https://www.google.com/
  if (chipCreateImages) {
    chipCreateImages.addEventListener("click", () => {
      window.location.href = GOOGLE_HOME_PAGE;
    });
  }

  if (btnMic) {
    btnMic.addEventListener("click", () => {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      if (SpeechRecognition) {
        const recognition = new SpeechRecognition();
        recognition.lang = "fa-IR";
        recognition.start();
        searchInput.placeholder = "در حال گوش دادن...";
        recognition.onresult = (event) => {
          const transcript = event.results[0][0].transcript;
          searchInput.value = transcript;
          performGoogleSearch(transcript);
        };
        recognition.onerror = () => {
          searchInput.placeholder = "Ask Google";
        };
      } else {
        performGoogleSearch(searchInput.value);
      }
    });
  }

  if (btnLens) {
    btnLens.addEventListener("click", () => {
      window.location.href = "https://lens.google.com";
    });
  }

  if (btnGoogleApps && googleAppsDropdown) {
    btnGoogleApps.addEventListener("click", (e) => {
      e.stopPropagation();
      const isOpen = googleAppsDropdown.style.display === "block";
      googleAppsDropdown.style.display = isOpen ? "none" : "block";
    });
    document.addEventListener("click", (e) => {
      if (!googleAppsDropdown.contains(e.target) && e.target !== btnGoogleApps) {
        googleAppsDropdown.style.display = "none";
      }
    });
  }

  // Populate Real Top Sites from chrome.topSites API with Remove capability
  async function loadTopSites() {
    if (!shortcutsContainer) return;

    // Load hidden/deleted shortcut URLs from chrome.storage.local
    let hiddenUrls = [];
    let customShortcuts = [];
    try {
      if (typeof chrome !== "undefined" && chrome.storage && chrome.storage.local) {
        const stored = await chrome.storage.local.get(["hiddenShortcuts", "customShortcuts"]);
        if (stored.hiddenShortcuts && Array.isArray(stored.hiddenShortcuts)) {
          hiddenUrls = stored.hiddenShortcuts;
        }
        if (stored.customShortcuts && Array.isArray(stored.customShortcuts)) {
          customShortcuts = stored.customShortcuts;
        }
      }
    } catch (_) {}

    const brandLogos = {
      "youtube.com": `<svg viewBox="0 0 24 24" width="22" height="22"><path fill="#ff0000" d="M21.58 7.19a2.5 2.5 0 0 0-1.76-1.77C18.26 5 12 5 12 5s-6.26 0-7.82.42A2.5 2.5 0 0 0 2.42 7.2 26.2 26.2 0 0 0 2 12a26.2 26.2 0 0 0 .42 4.81 2.5 2.5 0 0 0 1.76 1.77C5.74 19 12 19 12 19s6.26 0 7.82-.42a2.5 2.5 0 0 0 1.76-1.77C22 15.26 22 12 22 12s0-3.26-.42-4.81z"/><polygon fill="#fff" points="10 15 15 12 10 9"/></svg>`,
      "chatgpt.com": `<svg viewBox="0 0 24 24" width="22" height="22" fill="#10a37f"><path d="M22.28 9.87a5.98 5.98 0 0 0-.52-4.91 6.05 6.05 0 0 0-6.51-2.9A6.06 6.06 0 0 0 10.5.25a6.05 6.05 0 0 0-5.77 4.22 6.02 6.02 0 0 0-4.08 2.95 6.05 6.05 0 0 0 .75 7.1 5.98 5.98 0 0 0 .52 4.91 6.05 6.05 0 0 0 6.51 2.9A6.06 6.06 0 0 0 13.5 23.75a6.05 6.05 0 0 0 5.77-4.22 6.02 6.02 0 0 0 4.08-2.95 6.05 6.05 0 0 0-.75-7.1zM13.5 22.2a4.5 4.5 0 0 1-2.93-1.07l.15-.08 4.88-2.82a.77.77 0 0 0 .39-.67v-6.9l2.08 1.2a.1.1 0 0 1 .05.08v5.76a4.52 4.52 0 0 1-4.62 4.5zm-8.8-3.83a4.5 4.5 0 0 1-.58-3.08l.15.09 4.88 2.82c.24.14.39.4.39.67v6.9l-2.08-1.2a.1.1 0 0 1-.05-.08v-5.76a4.52 4.52 0 0 1-2.79-.36zM3.4 9.17a4.5 4.5 0 0 1 2.35-2.01v6.08a.77.77 0 0 0 .39.67l5.97 3.45-2.08 1.2a.1.1 0 0 1-.1 0l-4.99-2.88a4.52 4.52 0 0 1-1.54-6.51zm15.14 2.82-5.97-3.45 2.08-1.2a.1.1 0 0 1 .1 0l4.99 2.88a4.52 4.52 0 0 1-1.2 8.2v-6.08a.77.77 0 0 0-.39-.67z"/></svg>`,
      "gemini.google.com": `<svg viewBox="0 0 24 24" width="22" height="22"><path fill="url(#geminiGrad)" d="M12 2c.4 5.3 4.7 9.6 10 10-5.3.4-9.6 4.7-10 10-.4-5.3-4.7-9.6-10-10 5.3-.4 9.6-4.7 10-10z"/><defs><linearGradient id="geminiGrad" x1="2" y1="2" x2="22" y2="22" gradientUnits="userSpaceOnUse"><stop stop-color="#4285F4"/><stop offset="0.5" stop-color="#9B72CB"/><stop offset="1" stop-color="#D96570"/></linearGradient></defs></svg>`,
      "grok.com": `<svg viewBox="0 0 24 24" width="22" height="22" fill="#000"><rect x="3" y="3" width="18" height="18" rx="4" fill="#000"/><path d="M8 8l8 8M16 8l-8 8" stroke="#fff" stroke-width="2.5" stroke-linecap="round"/></svg>`,
      "claude.ai": `<svg viewBox="0 0 24 24" width="22" height="22"><path fill="#D97757" d="M12 2a1.5 1.5 0 0 1 1.5 1.5v3.086l2.182-2.182a1.5 1.5 0 1 1 2.122 2.122L15.62 8.707h3.086a1.5 1.5 0 1 1 0 3H15.62l2.182 2.182a1.5 1.5 0 0 1-2.122 2.122L13.5 13.83v3.086a1.5 1.5 0 1 1-3 0V13.83l-2.182 2.182a1.5 1.5 0 0 1-2.122-2.122L8.38 11.707H5.293a1.5 1.5 0 0 1 0-3H8.38L6.196 6.525A1.5 1.5 0 1 1 8.318 4.404L10.5 6.586V3.5A1.5 1.5 0 0 1 12 2z"/></svg>`
    };

    let defaultSites = [
      { title: "youtube.com", url: "https://www.youtube.com", domain: "youtube.com" },
      { title: "chatgpt.com", url: "https://chatgpt.com", domain: "chatgpt.com" },
      { title: "gemini.google.com", url: "https://gemini.google.com", domain: "gemini.google.com" },
      { title: "grok.com", url: "https://grok.com", domain: "grok.com" },
      { title: "claude.ai", url: "https://claude.ai", domain: "claude.ai" }
    ];

    let sitesToRender = [...defaultSites];

    if (typeof chrome !== "undefined" && chrome.topSites && chrome.topSites.get) {
      try {
        const realSites = await new Promise((resolve) => chrome.topSites.get(resolve));
        if (realSites && realSites.length > 0) {
          sitesToRender = realSites.map((s) => {
            let domain = "";
            try { domain = new URL(s.url).hostname.replace("www.", ""); } catch (_) { domain = s.url; }
            return {
              title: s.title || domain,
              url: s.url,
              domain: domain
            };
          });
        }
      } catch (_) {}
    }

    // اضافه کردن میانبرهای سفارشی ساخته‌شده توسط کاربر
    if (customShortcuts.length > 0) {
      const customUrls = new Set(customShortcuts.map((s) => s.url));
      sitesToRender = [...customShortcuts, ...sitesToRender.filter((s) => !customUrls.has(s.url))];
    }

    // Filter out hidden/deleted shortcuts
    sitesToRender = sitesToRender.filter((s) => !hiddenUrls.includes(s.url) && !hiddenUrls.includes(s.domain));

    // Limit to max 8 shortcuts
    sitesToRender = sitesToRender.slice(0, 8);

    function getCleanDomain(urlStr, domainStr) {
      if (domainStr && !domainStr.startsWith("http")) return domainStr.replace(/^www\./i, "");
      try {
        let u = (urlStr || "").trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://" + u;
        return new URL(u).hostname.replace(/^www\./i, "");
      } catch (_) {
        return (urlStr || "").replace(/^https?:\/\//i, "").replace(/^www\./i, "").split("/")[0] || "site";
      }
    }

    let html = sitesToRender
      .map((s) => {
        let iconContent = "";
        const cleanDom = getCleanDomain(s.url, s.domain);

        if (brandLogos[cleanDom] || brandLogos[s.domain]) {
          iconContent = `<div class="shortcut-brand-svg">${brandLogos[cleanDom] || brandLogos[s.domain]}</div>`;
        } else {
          const googleFavicon = `https://www.google.com/s2/favicons?domain=${encodeURIComponent(cleanDom)}&sz=128`;
          const ddgFavicon = `https://icons.duckduckgo.com/ip3/${encodeURIComponent(cleanDom)}.ico`;
          const firstChar = (s.title || cleanDom || "●").trim().charAt(0).toUpperCase();

          iconContent = `
            <img src="${googleFavicon}" 
                 alt="${s.title}" 
                 loading="lazy"
                 onerror="if(!this.dataset.triedFallback){this.dataset.triedFallback='1';this.src='${ddgFavicon}';}else{this.style.display='none';this.nextElementSibling.style.display='flex';}">
            <span class="shortcut-fallback-letter" style="display:none;">${firstChar}</span>
          `;
        }

        return `
        <div class="shortcut-tile" title="${s.title}" data-url="${s.url}" data-domain="${cleanDom}">
          <button class="shortcut-remove-btn" title="حذف این میانبر" aria-label="حذف">✕</button>
          <a href="${s.url}" target="_top" class="shortcut-tile-link">
            <div class="shortcut-icon-circle">${iconContent}</div>
            <span class="shortcut-title">${s.title}</span>
          </a>
        </div>
      `;
      })
      .join("");

    // Add '+' Add shortcut button at the end
    html += `
      <div class="shortcut-tile" id="btnAddShortcutTile" style="cursor: pointer;" title="افزودن میانبر جدید">
        <div class="shortcut-icon-circle">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#5f6368" stroke-width="2.2" stroke-linecap="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
        </div>
        <span class="shortcut-title">Add shortcut</span>
      </div>
    `;

    shortcutsContainer.innerHTML = html;

    // Remove shortcut handler (حذف دائمی میانبر)
    shortcutsContainer.querySelectorAll(".shortcut-remove-btn").forEach((btn) => {
      btn.addEventListener("click", async (e) => {
        e.preventDefault();
        e.stopPropagation();
        const tile = btn.closest(".shortcut-tile");
        if (!tile) return;
        const targetUrl = tile.getAttribute("data-url");
        const targetDomain = tile.getAttribute("data-domain");

        tile.style.transform = "scale(0.8)";
        tile.style.opacity = "0";
        setTimeout(() => tile.remove(), 180);

        if (typeof chrome !== "undefined" && chrome.storage && chrome.storage.local) {
          try {
            const stored = await chrome.storage.local.get(["hiddenShortcuts", "customShortcuts"]);
            const list = Array.isArray(stored.hiddenShortcuts) ? stored.hiddenShortcuts : [];
            if (targetUrl && !list.includes(targetUrl)) list.push(targetUrl);
            if (targetDomain && !list.includes(targetDomain)) list.push(targetDomain);

            let customs = Array.isArray(stored.customShortcuts) ? stored.customShortcuts : [];
            customs = customs.filter((s) => s.url !== targetUrl);

            await chrome.storage.local.set({ hiddenShortcuts: list, customShortcuts: customs });
          } catch (_) {}
        }
      });
    });

    // باز کردن پنجره مودال افزودن میانبر
    const btnAddTile = document.getElementById("btnAddShortcutTile");
    if (btnAddTile) {
      btnAddTile.addEventListener("click", () => {
        openShortcutModal();
      });
    }
  }

  // مدیریت پنجره مودال افزودن میانبر (Add Shortcut Modal)
  const shortcutModal = document.getElementById("shortcutModal");
  const btnCloseShortcutModal = document.getElementById("btnCloseShortcutModal");
  const btnCancelShortcut = document.getElementById("btnCancelShortcut");
  const btnSaveShortcut = document.getElementById("btnSaveShortcut");
  const shortcutNameInput = document.getElementById("shortcutNameInput");
  const shortcutUrlInput = document.getElementById("shortcutUrlInput");

  function openShortcutModal() {
    if (!shortcutModal) return;
    if (shortcutNameInput) shortcutNameInput.value = "";
    if (shortcutUrlInput) shortcutUrlInput.value = "";
    shortcutModal.style.display = "flex";
    setTimeout(() => {
      if (shortcutNameInput) shortcutNameInput.focus();
    }, 50);
  }

  function closeShortcutModal() {
    if (shortcutModal) shortcutModal.style.display = "none";
  }

  if (btnCloseShortcutModal) btnCloseShortcutModal.addEventListener("click", closeShortcutModal);
  if (btnCancelShortcut) btnCancelShortcut.addEventListener("click", closeShortcutModal);
  if (shortcutModal) {
    shortcutModal.addEventListener("click", (e) => {
      if (e.target === shortcutModal) closeShortcutModal();
    });
  }

  async function handleSaveNewShortcut() {
    let rawUrl = (shortcutUrlInput ? shortcutUrlInput.value : "").trim();
    if (!rawUrl) {
      alert("لطفاً آدرس وب‌سایت را وارد کنید.");
      if (shortcutUrlInput) shortcutUrlInput.focus();
      return;
    }

    let fullUrl = rawUrl.startsWith("http://") || rawUrl.startsWith("https://") ? rawUrl : `https://${rawUrl}`;
    let domain = "";
    try {
      domain = new URL(fullUrl).hostname.replace(/^www\./i, "");
    } catch (_) {
      domain = rawUrl.replace(/^https?:\/\//i, "").replace(/^www\./i, "").split("/")[0];
    }

    let title = (shortcutNameInput ? shortcutNameInput.value : "").trim();
    if (!title) {
      title = domain;
    }

    // ذخیره در chrome.storage.local
    if (typeof chrome !== "undefined" && chrome.storage && chrome.storage.local) {
      try {
        const stored = await chrome.storage.local.get(["customShortcuts", "hiddenShortcuts"]);
        let customList = Array.isArray(stored.customShortcuts) ? stored.customShortcuts : [];
        let hiddenList = Array.isArray(stored.hiddenShortcuts) ? stored.hiddenShortcuts : [];

        hiddenList = hiddenList.filter((u) => u !== fullUrl && u !== domain);
        customList = customList.filter((s) => s.url !== fullUrl);
        customList.unshift({ title, url: fullUrl, domain });

        await chrome.storage.local.set({ customShortcuts: customList, hiddenShortcuts: hiddenList });
      } catch (_) {}
    }

    closeShortcutModal();
    await loadTopSites();
  }

  if (btnSaveShortcut) {
    btnSaveShortcut.addEventListener("click", handleSaveNewShortcut);
  }
  if (shortcutUrlInput) {
    shortcutUrlInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter") handleSaveNewShortcut();
    });
  }
  if (shortcutNameInput) {
    shortcutNameInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && shortcutUrlInput) shortcutUrlInput.focus();
    });
  }

  loadTopSites();

  // =========================================================================
  // 3. WINDOWS 7 ANALOG CLOCK (به وقت تهران)
  // =========================================================================
  const clockHourHand = document.getElementById("clockHourHand");
  const clockMinHand = document.getElementById("clockMinHand");
  const clockSecHand = document.getElementById("clockSecHand");
  const w7DigitalTehran = document.getElementById("w7DigitalTehran");

  function getTehranTime() {
    const now = new Date();
    const utcMs = now.getTime() + now.getTimezoneOffset() * 60000;
    // Iran Standard Time is UTC + 3:30 (3.5 * 3600000 ms)
    return new Date(utcMs + 3.5 * 3600000);
  }

  function updateTehranClock() {
    const t = getTehranTime();
    const hours = t.getHours();
    const minutes = t.getMinutes();
    const seconds = t.getSeconds();

    const secDeg = (seconds / 60) * 360;
    const minDeg = ((minutes + seconds / 60) / 60) * 360;
    const hourDeg = (((hours % 12) + minutes / 60) / 12) * 360;

    if (clockSecHand) clockSecHand.setAttribute("transform", `rotate(${secDeg} 100 100)`);
    if (clockMinHand) clockMinHand.setAttribute("transform", `rotate(${minDeg} 100 100)`);
    if (clockHourHand) clockHourHand.setAttribute("transform", `rotate(${hourDeg} 100 100)`);

    if (w7DigitalTehran) {
      const pad = (n) => String(n).padStart(2, "0");
      const timeStr = `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
      w7DigitalTehran.textContent = typeof PersianDateUtil !== "undefined" ? PersianDateUtil.toPersianDigits(timeStr) : timeStr;
    }
  }

  updateTehranClock();
  setInterval(updateTehranClock, 1000);

  // =========================================================================
  // 4. WINDOWS 7 DRAGGABLE GADGETS SYSTEM (Aero Glass, Drag & Drop, Persistence)
  // =========================================================================
  const gadgets = document.querySelectorAll(".w7-gadget");
  const modalGallery = document.getElementById("gadgetGalleryModal");
  const btnCloseGallery = document.getElementById("btnCloseGallery");
  const btnDoneGallery = document.getElementById("btnDoneGallery");
  const btnResetPositions = document.getElementById("btnResetPositions");
  const btnCustomize = document.getElementById("btnCustomize");

  // Load saved positions & visibility from chrome.storage.local
  async function loadGadgetStates() {
    const defaultPositions = {
      clock: { top: 75, left: 24, right: null, bottom: null, visible: true, min: false },
      calendar: { top: 75, right: 24, left: null, bottom: null, visible: true, min: false },
      prayer: { top: 310, left: 24, right: null, bottom: null, visible: true, min: false },
      tasks: { top: 480, right: 24, left: null, bottom: null, visible: true, min: false },
      note: { top: 540, left: 24, right: null, bottom: null, visible: true, min: false }
    };

    let savedStates = defaultPositions;
    try {
      if (typeof chrome !== "undefined" && chrome.storage && chrome.storage.local) {
        const stored = await chrome.storage.local.get(["w7_gadgets_state"]);
        if (stored.w7_gadgets_state) {
          savedStates = { ...defaultPositions, ...stored.w7_gadgets_state };
        }
      }
    } catch (_) {}

    gadgets.forEach((g) => {
      const id = g.getAttribute("data-gadget-id");
      const st = savedStates[id] || defaultPositions[id];
      if (st) {
        g.style.top = st.top !== null ? `${st.top}px` : "auto";
        g.style.left = st.left !== null ? `${st.left}px` : "auto";
        g.style.right = st.right !== null ? `${st.right}px` : "auto";
        g.style.bottom = st.bottom !== null ? `${st.bottom}px` : "auto";
        g.style.display = st.visible ? "block" : "none";
        if (st.min) g.classList.add("minimized");
        else g.classList.remove("minimized");
      }
    });

    document.querySelectorAll(".gallery-toggle-item input").forEach((chk) => {
      const targetId = chk.getAttribute("data-target");
      const targetEl = document.getElementById(targetId);
      if (targetEl) {
        chk.checked = targetEl.style.display !== "none";
      }
    });
  }

  async function saveGadgetState() {
    const states = {};
    gadgets.forEach((g) => {
      const id = g.getAttribute("data-gadget-id");
      const rect = g.getBoundingClientRect();
      states[id] = {
        top: Math.max(10, Math.round(rect.top)),
        left: Math.max(10, Math.round(rect.left)),
        right: null,
        bottom: null,
        visible: g.style.display !== "none",
        min: g.classList.contains("minimized")
      };
    });

    if (typeof chrome !== "undefined" && chrome.storage && chrome.storage.local) {
      await chrome.storage.local.set({ w7_gadgets_state: states });
    }
  }

  // Make Gadgets Draggable
  gadgets.forEach((gadget) => {
    const isClock = gadget.id === "gadgetClock";
    const header = gadget.querySelector(".w7-gadget-header");
    const dragHandle = header || (isClock ? gadget : null);
    if (!dragHandle) return;

    let isDragging = false;
    let startX = 0, startY = 0;
    let initialLeft = 0, initialTop = 0;

    dragHandle.addEventListener("mousedown", (e) => {
      if (
        e.target.classList.contains("w7-btn-min") ||
        e.target.classList.contains("w7-btn-close") ||
        e.target.classList.contains("w7-clock-hover-close")
      ) return;

      isDragging = true;
      gadget.classList.add("dragging");

      const rect = gadget.getBoundingClientRect();
      startX = e.clientX;
      startY = e.clientY;
      initialLeft = rect.left;
      initialTop = rect.top;

      gadget.style.right = "auto";
      gadget.style.bottom = "auto";
      gadget.style.left = `${initialLeft}px`;
      gadget.style.top = `${initialTop}px`;

      function onMouseMove(ev) {
        if (!isDragging) return;
        const dx = ev.clientX - startX;
        const dy = ev.clientY - startY;

        const newLeft = Math.max(10, Math.min(window.innerWidth - gadget.offsetWidth - 10, initialLeft + dx));
        const newTop = Math.max(10, Math.min(window.innerHeight - gadget.offsetHeight - 10, initialTop + dy));

        gadget.style.left = `${newLeft}px`;
        gadget.style.top = `${newTop}px`;
      }

      function onMouseUp() {
        if (isDragging) {
          isDragging = false;
          gadget.classList.remove("dragging");
          saveGadgetState();
        }
        document.removeEventListener("mousemove", onMouseMove);
        document.removeEventListener("mouseup", onMouseUp);
      }

      document.addEventListener("mousemove", onMouseMove);
      document.addEventListener("mouseup", onMouseUp);
    });

    // Minimize button
    const btnMin = gadget.querySelector(".w7-btn-min");
    if (btnMin) {
      btnMin.addEventListener("click", () => {
        gadget.classList.toggle("minimized");
        saveGadgetState();
      });
    }

    // Close button (for standard framed gadgets)
    const btnClose = gadget.querySelector(".w7-btn-close");
    if (btnClose) {
      btnClose.addEventListener("click", () => {
        gadget.style.display = "none";
        saveGadgetState();
        loadGadgetStates();
      });
    }
  });

  // Clock specific hover close button (تغییر ۴: بستن ساعت بدون کادر)
  const btnCloseClockHover = document.getElementById("btnCloseClockHover");
  if (btnCloseClockHover) {
    btnCloseClockHover.addEventListener("click", (e) => {
      e.stopPropagation();
      const gadgetClock = document.getElementById("gadgetClock");
      if (gadgetClock) {
        gadgetClock.style.display = "none";
        saveGadgetState();
        loadGadgetStates();
      }
    });
  }

  // Gadget Gallery Modal Controls
  function openGallery() {
    if (modalGallery) modalGallery.style.display = "flex";
  }
  function closeGallery() {
    if (modalGallery) modalGallery.style.display = "none";
  }

  if (btnCustomize) btnCustomize.addEventListener("click", openGallery);
  if (btnCloseGallery) btnCloseGallery.addEventListener("click", closeGallery);
  if (btnDoneGallery) btnDoneGallery.addEventListener("click", closeGallery);

  document.querySelectorAll(".gallery-toggle-item input").forEach((chk) => {
    chk.addEventListener("change", (e) => {
      const targetId = e.target.getAttribute("data-target");
      const targetEl = document.getElementById(targetId);
      if (targetEl) {
        targetEl.style.display = e.target.checked ? "block" : "none";
        saveGadgetState();
      }
    });
  });

  if (btnResetPositions) {
    btnResetPositions.addEventListener("click", async () => {
      if (typeof chrome !== "undefined" && chrome.storage && chrome.storage.local) {
        await chrome.storage.local.remove(["w7_gadgets_state"]);
      }
      location.reload();
    });
  }

  const btnRestoreShortcuts = document.getElementById("btnRestoreShortcuts");
  if (btnRestoreShortcuts) {
    btnRestoreShortcuts.addEventListener("click", async () => {
      if (typeof chrome !== "undefined" && chrome.storage && chrome.storage.local) {
        await chrome.storage.local.remove(["hiddenShortcuts"]);
      }
      await loadTopSites();
    });
  }

  await loadGadgetStates();

  // =========================================================================
  // 4. SOLAR CALENDAR GADGET (تقویم هجری شمسی)
  // =========================================================================
  const headerMonthTitle = document.getElementById("headerMonthTitle");
  const headerGregorianSpan = document.getElementById("headerGregorianSpan");
  const btnPrevMonth = document.getElementById("btnPrevMonth");
  const btnNextMonth = document.getElementById("btnNextMonth");
  const btnToday = document.getElementById("btnToday");
  const calendarGrid = document.getElementById("calendarGrid");
  const selectedDateTitle = document.getElementById("selectedDateTitle");
  const selectedGregorianDate = document.getElementById("selectedGregorianDate");
  const dayEventsList = document.getElementById("dayEventsList");

  const todayJalali = PersianDateUtil.getTodayJalali();
  let currentYear = todayJalali.year;
  let currentMonth = todayJalali.month;
  let selectedDate = { ...todayJalali };

  // Task filters & state (Matching Mobile App)
  let taskScopeFilter = "day"; // "day" | "all"
  let taskStatusFilter = "all"; // "all" | "pending" | "done"

  // Notes state
  let noteSearchQuery = "";

  let localEvents = [];
  let localTasks = [];
  let localNotes = [];

  function renderCalendar() {
    if (!calendarGrid) return;
    const daysInMonth = PersianDateUtil.getDaysInJalaliMonth(currentYear, currentMonth);
    const firstDow = PersianDateUtil.getPersianDayOfWeek(currentYear, currentMonth, 1);

    if (headerMonthTitle) {
      headerMonthTitle.textContent = `${PersianDateUtil.PERSIAN_MONTHS[currentMonth - 1]} ${PersianDateUtil.toPersianDigits(currentYear)}`;
    }
    const firstG = PersianDateUtil.jalaliToGregorian(currentYear, currentMonth, 1);
    const lastG = PersianDateUtil.jalaliToGregorian(currentYear, currentMonth, daysInMonth);
    if (headerGregorianSpan) {
      headerGregorianSpan.textContent = `(${firstG.day} ${PersianDateUtil.GREGORIAN_MONTHS[firstG.month - 1]} - ${lastG.day} ${PersianDateUtil.GREGORIAN_MONTHS[lastG.month - 1]})`;
    }

    calendarGrid.innerHTML = "";

    PersianDateUtil.WEEKDAY_ABBR.forEach((abbr, idx) => {
      const hCell = document.createElement("div");
      hCell.className = `cal-w7-cell header ${idx === 6 ? "friday" : ""}`;
      hCell.textContent = abbr;
      calendarGrid.appendChild(hCell);
    });

    for (let i = 0; i < firstDow; i++) {
      const empty = document.createElement("div");
      empty.className = "cal-w7-cell empty";
      calendarGrid.appendChild(empty);
    }

    for (let day = 1; day <= daysInMonth; day++) {
      const cellJalali = { year: currentYear, month: currentMonth, day };
      const formattedJalali = PersianDateUtil.formatJalali(cellJalali);

      const isToday = currentYear === todayJalali.year && currentMonth === todayJalali.month && day === todayJalali.day;
      const isSelected = currentYear === selectedDate.year && currentMonth === selectedDate.month && day === selectedDate.day;

      const dow = (firstDow + day - 1) % 7;
      const isFriday = dow === 6;
      const isHoliday = isFriday || (typeof PersianOccasions !== "undefined" && PersianOccasions.isHoliday(currentMonth, day, currentYear));

      // بررسی وجود رویداد، وظیفه یا یادداشت برای نمایش دایره نشانگر روی تقویم
      const hasEvents = localEvents.some((e) => !e.isDeleted && PersianDateUtil.normalizePersianDate(e.persianDate) === formattedJalali);
      const hasTasks = localTasks.some((t) => !t.isDeleted && PersianDateUtil.normalizePersianDate(t.persianDueDate) === formattedJalali);
      const hasNotes = localNotes.some((n) => !n.isDeleted && PersianDateUtil.normalizePersianDate(n.persianDate) === formattedJalali);
      const hasSomething = hasEvents || hasTasks || hasNotes;

      const cell = document.createElement("div");
      cell.className = `cal-w7-cell ${isSelected ? "selected" : ""} ${isToday ? "today" : ""} ${isHoliday ? "holiday" : ""} ${hasSomething ? "has-event" : ""}`;
      cell.innerHTML = `<span>${PersianDateUtil.toPersianDigits(day)}</span>${hasSomething ? '<span class="cal-dot-indicator"></span>' : ''}`;

      cell.addEventListener("click", () => {
        selectedDate = { ...cellJalali };
        // همگام با اپلیکیشن: بازه وظایف روی روز انتخابی تنظیم می‌شود
        taskScopeFilter = "day";
        updateTaskFilterChipsUI();
        renderCalendar();
        renderSelectedDayInfo();
        renderTasks();
      });

      calendarGrid.appendChild(cell);
    }
  }

  // =========================================================================
  // TOAST NOTIFICATION HELPER
  // =========================================================================
  const syncToast = document.getElementById("syncToast");
  const syncToastMsg = document.getElementById("syncToastMsg");
  const syncToastIcon = document.getElementById("syncToastIcon");
  let toastTimer = null;

  function showToast(message, type = "success") {
    if (!syncToast) return;
    clearTimeout(toastTimer);
    syncToast.className = `sync-toast show ${type}`;
    if (syncToastMsg) syncToastMsg.textContent = message;
    if (syncToastIcon) {
      syncToastIcon.textContent = type === "success" ? "✓" : type === "error" ? "✕" : "ℹ️";
    }
    toastTimer = setTimeout(() => {
      syncToast.className = "sync-toast";
    }, 3500);
  }

  function renderSelectedDayInfo() {
    if (!selectedDateTitle) return;
    const formatted = PersianDateUtil.formatJalali(selectedDate);
    const selG = PersianDateUtil.jalaliToGregorian(selectedDate.year, selectedDate.month, selectedDate.day);
    const dowIdx = PersianDateUtil.getPersianDayOfWeek(selectedDate.year, selectedDate.month, selectedDate.day);

    selectedDateTitle.textContent = `${PersianDateUtil.WEEKDAYS[dowIdx]} ${PersianDateUtil.toPersianDigits(formatted)}`;
    if (selectedGregorianDate) {
      selectedGregorianDate.textContent = `میلادی: ${selG.day} ${PersianDateUtil.GREGORIAN_MONTHS[selG.month - 1]} ${selG.year}`;
    }

    const occasions = typeof PersianOccasions !== "undefined" ? PersianOccasions.getOccasions(selectedDate.month, selectedDate.day, selectedDate.year) : [];
    const dayEvents = localEvents.filter((e) => !e.isDeleted && PersianDateUtil.normalizePersianDate(e.persianDate) === formatted);
    const dayTasks = localTasks.filter((t) => !t.isDeleted && PersianDateUtil.normalizePersianDate(t.persianDueDate) === formatted);
    const dayNotes = localNotes.filter((n) => !n.isDeleted && PersianDateUtil.normalizePersianDate(n.persianDate) === formatted);

    let html = "";
    if (occasions.length > 0) {
      html += occasions
        .map(
          (o) =>
            `<div style="font-weight: 600; margin-bottom: 2px; color: ${o.isHoliday ? "#dc2626" : "#b45309"}">• ${o.title}</div>`
        )
        .join("");
    }
    if (dayEvents.length > 0) {
      html += `<div class="cal-section-subtitle">📅 رویدادها (${PersianDateUtil.toPersianDigits(dayEvents.length)}):</div>`;
      html += dayEvents
        .map(
          (e) => `
        <div class="cal-event-row" style="border-right-color: ${e.colorHex || '#d97706'}">
          <div class="cal-event-content">
            <span class="cal-event-title">${e.title}</span>
            ${e.startTime ? `<span class="cal-event-time">⏰ ${PersianDateUtil.toPersianDigits(e.startTime)}${e.endTime ? ' تا ' + PersianDateUtil.toPersianDigits(e.endTime) : ''}</span>` : ""}
          </div>
          <button class="cal-event-delete-btn" data-event-id="${e.id}" title="حذف رویداد">✕</button>
        </div>
      `
        )
        .join("");
    }
    if (dayTasks.length > 0) {
      const pendingCount = dayTasks.filter(t => !t.isCompleted).length;
      html += `<div class="cal-section-subtitle">✓ وظایف (${PersianDateUtil.toPersianDigits(dayTasks.length)} - در انتظار: ${PersianDateUtil.toPersianDigits(pendingCount)}):</div>`;
      html += dayTasks
        .map(
          (t) => {
            const pClass = t.priority === "بالا" ? "priority-high" : (t.priority === "پایین" ? "priority-low" : "priority-medium");
            const borderColor = t.priority === "بالا" ? "#ef4444" : (t.priority === "پایین" ? "#10b981" : "#f59e0b");
            return `
            <div class="cal-task-row ${t.isCompleted ? 'completed' : ''}" style="border-right-color: ${borderColor}">
              <div class="cal-task-row-left">
                <input type="checkbox" ${t.isCompleted ? 'checked' : ''} data-task-id="${t.id}" class="cal-task-checkbox">
                <span>${t.title}</span>
              </div>
              <button class="cal-event-delete-btn" data-cal-task-id="${t.id}" title="حذف وظیفه">✕</button>
            </div>
            `;
          }
        )
        .join("");
    }
    if (dayNotes.length > 0) {
      html += `<div class="cal-section-subtitle">📝 یادداشت‌ها:</div>`;
      html += dayNotes
        .map(
          (n) => `
        <div class="cal-event-row" style="border-right-color: #eab308; background: #fefce8;">
          <div class="cal-event-content">
            <span class="cal-event-title" style="color: #854d0e;">${n.title ? n.title + ': ' : ''}${n.content}</span>
          </div>
        </div>
      `
        )
        .join("");
    }
    if (!html) {
      html = `<span style="color: var(--text-muted); font-size: 10px;">رویداد یا وظیفه‌ای برای این روز ثبت نشده است.</span>`;
    }

    if (dayEventsList) {
      dayEventsList.innerHTML = html;

      // Bind delete buttons for calendar events
      dayEventsList.querySelectorAll(".cal-event-delete-btn[data-event-id]").forEach((btn) => {
        btn.addEventListener("click", async (ev) => {
          ev.stopPropagation();
          const evId = btn.getAttribute("data-event-id");
          const evItem = localEvents.find((x) => x.id === evId);
          if (evItem) {
            evItem.isDeleted = true;
            evItem.updatedAt = Date.now();
            await SyncClient.saveLocalData(localEvents, localTasks, localNotes);
            renderSelectedDayInfo();
            renderCalendar();
            showToast("رویداد با موفقیت حذف شد", "info");
          }
        });
      });

      // Bind checkboxes for tasks directly in calendar info
      dayEventsList.querySelectorAll(".cal-task-checkbox").forEach((cb) => {
        cb.addEventListener("change", async (ev) => {
          const taskId = ev.target.getAttribute("data-task-id");
          const task = localTasks.find((x) => x.id === taskId);
          if (task) {
            task.isCompleted = ev.target.checked;
            task.updatedAt = Date.now();
            await SyncClient.saveLocalData(localEvents, localTasks, localNotes);
            renderSelectedDayInfo();
            renderTasks();
          }
        });
      });

      // Bind delete buttons for tasks inside calendar info
      dayEventsList.querySelectorAll(".cal-event-delete-btn[data-cal-task-id]").forEach((btn) => {
        btn.addEventListener("click", async (ev) => {
          ev.stopPropagation();
          const taskId = btn.getAttribute("data-cal-task-id");
          const task = localTasks.find((x) => x.id === taskId);
          if (task) {
            task.isDeleted = true;
            task.updatedAt = Date.now();
            await SyncClient.saveLocalData(localEvents, localTasks, localNotes);
            renderSelectedDayInfo();
            renderCalendar();
            renderTasks();
            showToast("وظیفه با موفقیت حذف شد", "info");
          }
        });
      });
    }
  }

  // =========================================================================
  // CALENDAR ADD-EVENT MODAL
  // =========================================================================
  const addEventModal = document.getElementById("addEventModal");
  const btnOpenAddEvent = document.getElementById("btnOpenAddEvent");
  const btnCloseEventModal = document.getElementById("btnCloseEventModal");
  const btnCancelEvent = document.getElementById("btnCancelEvent");
  const btnSaveEvent = document.getElementById("btnSaveEvent");
  const eventTitleInput = document.getElementById("eventTitleInput");
  const eventDateInput = document.getElementById("eventDateInput");
  const eventStartTimeInput = document.getElementById("eventStartTimeInput");
  const eventEndTimeInput = document.getElementById("eventEndTimeInput");
  const eventCategorySelect = document.getElementById("eventCategorySelect");

  function openEventModal() {
    if (!addEventModal) return;
    const formatted = PersianDateUtil.formatJalali(selectedDate);
    if (eventDateInput) eventDateInput.value = formatted;
    if (eventTitleInput) eventTitleInput.value = "";
    if (eventStartTimeInput) eventStartTimeInput.value = "";
    if (eventEndTimeInput) eventEndTimeInput.value = "";
    addEventModal.style.display = "flex";
    if (eventTitleInput) eventTitleInput.focus();
  }

  function closeEventModal() {
    if (addEventModal) addEventModal.style.display = "none";
  }

  if (btnOpenAddEvent) btnOpenAddEvent.addEventListener("click", openEventModal);
  if (btnCloseEventModal) btnCloseEventModal.addEventListener("click", closeEventModal);
  if (btnCancelEvent) btnCancelEvent.addEventListener("click", closeEventModal);

  if (btnSaveEvent) {
    btnSaveEvent.addEventListener("click", async () => {
      const title = (eventTitleInput?.value || "").trim();
      if (!title) {
        alert("لطفاً عنوان رویداد را وارد کنید.");
        return;
      }
      const pDate = (eventDateInput?.value || "").trim() || PersianDateUtil.formatJalali(selectedDate);
      const sTime = (eventStartTimeInput?.value || "").trim();
      const eTime = (eventEndTimeInput?.value || "").trim();
      const cat = eventCategorySelect?.value || "کاری";

      const colorMap = {
        "کاری": "#2563EB",
        "شخصی": "#16A34A",
        "مهم": "#DC2626",
        "تحصیلی": "#7C3AED",
        "عمومی": "#D97706"
      };

      const newEvent = {
        id: "evt_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7),
        title: title,
        description: "",
        persianDate: PersianDateUtil.normalizePersianDate(pDate),
        gregorianDate: "",
        startTime: sTime,
        endTime: eTime,
        category: cat,
        colorHex: colorMap[cat] || "#D97706",
        updatedAt: Date.now(),
        isDeleted: false
      };

      localEvents.unshift(newEvent);
      await SyncClient.saveLocalData(localEvents, localTasks, localNotes);
      closeEventModal();
      renderCalendar();
      renderSelectedDayInfo();
      showToast("رویداد جدید ذخیره شد ✓ (آماده همگام‌سازی با گوشی)", "success");
    });
  }

  if (btnPrevMonth) {
    btnPrevMonth.addEventListener("click", () => {
      if (currentMonth === 1) {
        currentMonth = 12;
        currentYear--;
      } else {
        currentMonth--;
      }
      renderCalendar();
    });
  }

  if (btnNextMonth) {
    btnNextMonth.addEventListener("click", () => {
      if (currentMonth === 12) {
        currentMonth = 1;
        currentYear++;
      } else {
        currentMonth++;
      }
      renderCalendar();
    });
  }

  if (btnToday) {
    btnToday.addEventListener("click", () => {
      currentYear = todayJalali.year;
      currentMonth = todayJalali.month;
      selectedDate = { ...todayJalali };
      taskScopeFilter = "day";
      updateTaskFilterChipsUI();
      renderCalendar();
      renderSelectedDayInfo();
      renderTasks();
    });
  }

  // =========================================================================
  // 5. PRAYER TIMES GADGET (اوقات شرعی آفلاین)
  // =========================================================================
  const dashCitySelect = document.getElementById("dashCitySelect");
  const dashNextPrayerBadge = document.getElementById("dashNextPrayerBadge");

  if (typeof IRANIAN_CITIES !== "undefined" && dashCitySelect) {
    dashCitySelect.innerHTML = IRANIAN_CITIES.map((c) => `<option value="${c.nameFa}">${c.nameFa}</option>`).join("");

    let savedCity = "تهران";
    try {
      if (typeof chrome !== "undefined" && chrome.storage && chrome.storage.local) {
        const stored = await chrome.storage.local.get(["selectedCity"]);
        if (stored.selectedCity) savedCity = stored.selectedCity;
      }
    } catch (_) {}
    dashCitySelect.value = savedCity;

    function updatePrayerTimes() {
      const city = IRANIAN_CITIES.find((c) => c.nameFa === dashCitySelect.value) || IRANIAN_CITIES[0];
      const times = PrayerTimesEngine.calculate(city.lat, city.lng, new Date());
      const next = PrayerTimesEngine.getNextPrayer(times, new Date());

      document.getElementById("dt-fajr").textContent = PersianDateUtil.toPersianDigits(times.fajr);
      document.getElementById("dt-sunrise").textContent = PersianDateUtil.toPersianDigits(times.sunrise);
      document.getElementById("dt-dhuhr").textContent = PersianDateUtil.toPersianDigits(times.dhuhr);
      document.getElementById("dt-sunset").textContent = PersianDateUtil.toPersianDigits(times.sunset);
      document.getElementById("dt-maghrib").textContent = PersianDateUtil.toPersianDigits(times.maghrib);
      document.getElementById("dt-midnight").textContent = PersianDateUtil.toPersianDigits(times.midnight);

      ["dc-fajr", "dc-sunrise", "dc-dhuhr", "dc-sunset", "dc-maghrib", "dc-midnight"].forEach((id) => {
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
      const remText = hours > 0 ? `${hours}س و ${mins}د` : `${mins}د`;
      if (dashNextPrayerBadge) {
        dashNextPrayerBadge.textContent = `${next.name} (${PersianDateUtil.toPersianDigits(remText)})`;
      }
    }

    updatePrayerTimes();
    setInterval(updatePrayerTimes, 60000);
    dashCitySelect.addEventListener("change", async () => {
      updatePrayerTimes();
      if (typeof chrome !== "undefined" && chrome.storage && chrome.storage.local) {
        await chrome.storage.local.set({ selectedCity: dashCitySelect.value });
      }
    });
  }

  // =========================================================================
  // 6. TASKS GADGET (چک‌لیست وظایف، فیلتر تاریخ/وضعیت و اولویت‌ها مشابه اپ)
  // =========================================================================
  const newtabTaskList = document.getElementById("newtabTaskList");
  const newTaskInput = document.getElementById("newTaskInput");
  const newTaskPriority = document.getElementById("newTaskPriority");
  const newTaskTimeInput = document.getElementById("newTaskTimeInput");
  const btnAddTask = document.getElementById("btnAddTask");
  const pendingCountBadge = document.getElementById("pendingCountBadge");
  const taskDateLabel = document.getElementById("taskDateLabel");
  const btnTaskPrevDay = document.getElementById("btnTaskPrevDay");
  const btnTaskNextDay = document.getElementById("btnTaskNextDay");
  const btnTaskToday = document.getElementById("btnTaskToday");

  // Chips
  const chipScopeDay = document.getElementById("chipScopeDay");
  const chipScopeAll = document.getElementById("chipScopeAll");
  const chipStatusAll = document.getElementById("chipStatusAll");
  const chipStatusPending = document.getElementById("chipStatusPending");
  const chipStatusDone = document.getElementById("chipStatusDone");

  // Progress Bar
  const taskProgressRatio = document.getElementById("taskProgressRatio");
  const taskProgressPercent = document.getElementById("taskProgressPercent");
  const taskProgressFill = document.getElementById("taskProgressFill");

  function changeSelectedDate(offsetDays) {
    const g = PersianDateUtil.jalaliToGregorian(selectedDate.year, selectedDate.month, selectedDate.day);
    const d = new Date(g.year, g.month - 1, g.day);
    d.setDate(d.getDate() + offsetDays);
    const newJ = PersianDateUtil.gregorianToJalali(d.getFullYear(), d.getMonth() + 1, d.getDate());
    selectedDate = newJ;
    currentYear = newJ.year;
    currentMonth = newJ.month;
    taskScopeFilter = "day";
    updateTaskFilterChipsUI();
    renderCalendar();
    renderSelectedDayInfo();
    renderTasks();
  }

  if (btnTaskPrevDay) btnTaskPrevDay.addEventListener("click", () => changeSelectedDate(-1));
  if (btnTaskNextDay) btnTaskNextDay.addEventListener("click", () => changeSelectedDate(1));
  if (btnTaskToday) {
    btnTaskToday.addEventListener("click", () => {
      selectedDate = { ...todayJalali };
      currentYear = todayJalali.year;
      currentMonth = todayJalali.month;
      taskScopeFilter = "day";
      updateTaskFilterChipsUI();
      renderCalendar();
      renderSelectedDayInfo();
      renderTasks();
    });
  }

  function updateTaskFilterChipsUI() {
    [chipScopeDay, chipScopeAll].forEach(c => c && c.classList.remove("active"));
    if (taskScopeFilter === "day" && chipScopeDay) chipScopeDay.classList.add("active");
    if (taskScopeFilter === "all" && chipScopeAll) chipScopeAll.classList.add("active");

    [chipStatusAll, chipStatusPending, chipStatusDone].forEach(c => c && c.classList.remove("active"));
    if (taskStatusFilter === "all" && chipStatusAll) chipStatusAll.classList.add("active");
    if (taskStatusFilter === "pending" && chipStatusPending) chipStatusPending.classList.add("active");
    if (taskStatusFilter === "done" && chipStatusDone) chipStatusDone.classList.add("active");
  }

  if (chipScopeDay) {
    chipScopeDay.addEventListener("click", () => {
      taskScopeFilter = "day";
      updateTaskFilterChipsUI();
      renderTasks();
    });
  }
  if (chipScopeAll) {
    chipScopeAll.addEventListener("click", () => {
      taskScopeFilter = "all";
      updateTaskFilterChipsUI();
      renderTasks();
    });
  }
  if (chipStatusAll) {
    chipStatusAll.addEventListener("click", () => {
      taskStatusFilter = "all";
      updateTaskFilterChipsUI();
      renderTasks();
    });
  }
  if (chipStatusPending) {
    chipStatusPending.addEventListener("click", () => {
      taskStatusFilter = "pending";
      updateTaskFilterChipsUI();
      renderTasks();
    });
  }
  if (chipStatusDone) {
    chipStatusDone.addEventListener("click", () => {
      taskStatusFilter = "done";
      updateTaskFilterChipsUI();
      renderTasks();
    });
  }

  function renderTasks() {
    if (!newtabTaskList) return;

    const formattedSelected = PersianDateUtil.formatJalali(selectedDate);
    const formattedToday = PersianDateUtil.formatJalali(todayJalali);
    const isViewingToday = formattedSelected === formattedToday;

    // به‌روزرسانی برچسب تاریخ در بالای ویجت وظایف
    if (taskDateLabel) {
      if (taskScopeFilter === "day") {
        const dow = PersianDateUtil.getPersianDayOfWeek(selectedDate.year, selectedDate.month, selectedDate.day);
        const dayName = PersianDateUtil.WEEKDAYS[dow];
        taskDateLabel.textContent = isViewingToday
          ? `امروز (${PersianDateUtil.toPersianDigits(selectedDate.day)} ${PersianDateUtil.PERSIAN_MONTHS[selectedDate.month - 1]})`
          : `${dayName}، ${PersianDateUtil.toPersianDigits(selectedDate.day)} ${PersianDateUtil.PERSIAN_MONTHS[selectedDate.month - 1]}`;
      } else {
        taskDateLabel.textContent = "همه زمان‌ها (کل کارها)";
      }
    }

    const allActiveTasks = localTasks.filter((t) => !t.isDeleted);

    // ۱. فیلتر بازه زمانی (روز انتخابی تقویم یا همه)
    let filteredTasks = allActiveTasks;
    if (taskScopeFilter === "day") {
      filteredTasks = filteredTasks.filter(
        (t) => PersianDateUtil.normalizePersianDate(t.persianDueDate) === formattedSelected
      );
    }

    // آمار کارهای بازه انتخابی برای نشانگر و پروگرس بار
    const totalInScope = filteredTasks.length;
    const completedInScope = filteredTasks.filter((t) => t.isCompleted).length;
    const pendingInScope = totalInScope - completedInScope;

    if (pendingCountBadge) {
      pendingCountBadge.textContent = PersianDateUtil.toPersianDigits(pendingInScope);
    }

    // به‌روزرسانی نوار پیشرفت (مشابه اپلیکیشن اندروید)
    if (taskProgressRatio && taskProgressPercent && taskProgressFill) {
      const percent = totalInScope > 0 ? Math.round((completedInScope / totalInScope) * 100) : 0;
      taskProgressRatio.textContent = `${PersianDateUtil.toPersianDigits(completedInScope)} از ${PersianDateUtil.toPersianDigits(totalInScope)} کار`;
      taskProgressPercent.textContent = `${PersianDateUtil.toPersianDigits(percent)}٪`;
      taskProgressFill.style.width = `${percent}%`;
    }

    // ۲. فیلتر وضعیت (همه، در انتظار، انجام‌شده)
    if (taskStatusFilter === "pending") {
      filteredTasks = filteredTasks.filter((t) => !t.isCompleted);
    } else if (taskStatusFilter === "done") {
      filteredTasks = filteredTasks.filter((t) => t.isCompleted);
    }

    if (filteredTasks.length === 0) {
      const msg = taskScopeFilter === "day"
        ? (isViewingToday ? "کاری برای امروز ثبت نشده است." : "کاری برای این تاریخ ثبت نشده است.")
        : "هیچ کاری در فهرست وجود ندارد.";
      newtabTaskList.innerHTML = `<p style="font-size: 11px; color: var(--text-muted); text-align: center; padding: 12px;">${msg}</p>`;
      return;
    }

    newtabTaskList.innerHTML = filteredTasks
      .map(
        (t) => {
          const priority = t.priority || "متوسط";
          const pClass = priority === "بالا" ? "priority-high" : (priority === "پایین" ? "priority-low" : "priority-medium");
          const pColor = priority === "بالا" ? "#ef4444" : (priority === "پایین" ? "#10b981" : "#f59e0b");

          const showDueDate = taskScopeFilter === "all" && t.persianDueDate;

          return `
          <div class="w7-task-item ${t.isCompleted ? "completed" : ""} ${pClass}">
            <div class="w7-task-item-left">
              <input type="checkbox" ${t.isCompleted ? "checked" : ""} data-id="${t.id}" class="task-check-w7">
              <div style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                <span>${t.title}</span>
                ${showDueDate ? `<span class="w7-task-due-date">📅 سررسید: ${PersianDateUtil.toPersianDigits(t.persianDueDate)}</span>` : ""}
              </div>
              <span class="w7-task-time-badge" style="color: ${pColor}; font-weight: bold;">${priority}</span>
            </div>
            <button class="w7-task-delete-btn" data-task-id="${t.id}" title="حذف وظیفه">✕</button>
          </div>
        `;
        }
      )
      .join("");

    // Checkbox toggling
    newtabTaskList.querySelectorAll(".task-check-w7").forEach((cb) => {
      cb.addEventListener("change", async (ev) => {
        const id = ev.target.getAttribute("data-id");
        const item = localTasks.find((x) => x.id === id);
        if (item) {
          item.isCompleted = ev.target.checked;
          item.updatedAt = Date.now();
          await SyncClient.saveLocalData(localEvents, localTasks, localNotes);
          renderTasks();
          renderSelectedDayInfo();
        }
      });
    });

    // Delete buttons
    newtabTaskList.querySelectorAll(".w7-task-delete-btn").forEach((btn) => {
      btn.addEventListener("click", async (ev) => {
        ev.stopPropagation();
        const id = btn.getAttribute("data-task-id");
        const item = localTasks.find((x) => x.id === id);
        if (item) {
          item.isDeleted = true;
          item.updatedAt = Date.now();
          await SyncClient.saveLocalData(localEvents, localTasks, localNotes);
          renderTasks();
          renderCalendar();
          renderSelectedDayInfo();
          showToast("وظیفه با موفقیت حذف شد", "info");
        }
      });
    });
  }

  if (btnAddTask && newTaskInput) {
    btnAddTask.addEventListener("click", async () => {
      const text = newTaskInput.value.trim();
      if (!text) return;
      const timeVal = (newTaskTimeInput?.value || "").trim();
      const priorityVal = newTaskPriority?.value || "متوسط";

      // تاریخ سررسید دقیقاً همان تاریخی است که کاربر روی تقویم انتخاب کرده است
      const taskDueDate = PersianDateUtil.formatJalali(selectedDate);

      const newTask = {
        id: "task_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7),
        title: timeVal ? `${text} (ساعت ${PersianDateUtil.toPersianDigits(timeVal)})` : text,
        isCompleted: false,
        persianDueDate: taskDueDate,
        priority: priorityVal,
        category: "عمومی",
        updatedAt: Date.now(),
        isDeleted: false
      };
      localTasks.unshift(newTask);
      await SyncClient.saveLocalData(localEvents, localTasks, localNotes);
      newTaskInput.value = "";
      if (newTaskTimeInput) newTaskTimeInput.value = "";
      renderTasks();
      renderCalendar();
      renderSelectedDayInfo();
      showToast(`کار جدید برای ${PersianDateUtil.toPersianDigits(taskDueDate)} ذخیره شد ✓`, "success");
    });
  }

  // =========================================================================
  // 7. NOTES GADGET (لیست یادداشت‌ها، پین کردن و جستجو مشابه اپلیکیشن اندروید)
  // =========================================================================
  const newtabNotesList = document.getElementById("newtabNotesList");
  const notesCountBadge = document.getElementById("notesCountBadge");
  const noteSearchInput = document.getElementById("noteSearchInput");
  const newNoteTitleInput = document.getElementById("newNoteTitleInput");
  const newNoteContentInput = document.getElementById("newNoteContentInput");
  const btnAddNote = document.getElementById("btnAddNote");
  const w7StickyStatus = document.getElementById("w7StickyStatus");

  if (noteSearchInput) {
    noteSearchInput.addEventListener("input", (e) => {
      noteSearchQuery = (e.target.value || "").trim().toLowerCase();
      renderNotes();
    });
  }

  function renderNotes() {
    if (!newtabNotesList) return;
    let activeNotes = localNotes.filter((n) => !n.isDeleted);
    if (notesCountBadge) notesCountBadge.textContent = PersianDateUtil.toPersianDigits(activeNotes.length);

    // فیلتر جستجو مشابه صفحه NotesScreen اپ
    if (noteSearchQuery) {
      activeNotes = activeNotes.filter((n) =>
        (n.title && n.title.toLowerCase().includes(noteSearchQuery)) ||
        (n.content && n.content.toLowerCase().includes(noteSearchQuery))
      );
    }

    // مرتب‌سازی: یادداشت‌های پین‌شده در ابتدا + سپس بر اساس آخرین به‌روزرسانی
    activeNotes.sort((a, b) => {
      if (a.isPinned && !b.isPinned) return -1;
      if (!a.isPinned && b.isPinned) return 1;
      return (b.updatedAt || 0) - (a.updatedAt || 0);
    });

    if (activeNotes.length === 0) {
      const emptyMsg = noteSearchQuery ? "یادداشتی مطابق با جستجو پیدا نشد." : "یادداشتی ذخیره نشده است.";
      newtabNotesList.innerHTML = `<p style="font-size: 11px; color: var(--text-muted); text-align: center; padding: 10px;">${emptyMsg}</p>`;
      return;
    }

    newtabNotesList.innerHTML = activeNotes
      .map(
        (n) => `
      <div class="w7-note-card ${n.isPinned ? 'pinned' : ''}" style="background: ${n.colorHex || '#fef9c3'}">
        <div class="w7-note-card-header">
          <span class="w7-note-card-title">
            <button class="w7-note-pin-btn ${n.isPinned ? 'pinned' : ''}" data-pin-id="${n.id}" title="${n.isPinned ? 'برداشتن سنجاق' : 'سنجاق کردن به بالا'}">
              ${n.isPinned ? '📌' : '📍'}
            </button>
            <span>${n.title || "بدون عنوان"}</span>
          </span>
          <div style="display: flex; align-items: center; gap: 4px;">
            ${n.persianDate ? `<span class="w7-note-card-date">${PersianDateUtil.toPersianDigits(n.persianDate)}</span>` : ""}
            <button class="w7-note-delete-btn" data-note-id="${n.id}" title="حذف یادداشت">✕</button>
          </div>
        </div>
        <div class="w7-note-card-content">${n.content || ""}</div>
      </div>
    `
      )
      .join("");

    // دکمه‌های پین کردن
    newtabNotesList.querySelectorAll(".w7-note-pin-btn").forEach((btn) => {
      btn.addEventListener("click", async (ev) => {
        ev.stopPropagation();
        const nId = btn.getAttribute("data-pin-id");
        const item = localNotes.find((x) => x.id === nId);
        if (item) {
          item.isPinned = !item.isPinned;
          item.updatedAt = Date.now();
          await SyncClient.saveLocalData(localEvents, localTasks, localNotes);
          renderNotes();
          showToast(item.isPinned ? "یادداشت سنجاق شد 📌" : "سنجاق یادداشت برداشته شد", "info");
        }
      });
    });

    // دکمه‌های حذف یادداشت
    newtabNotesList.querySelectorAll(".w7-note-delete-btn").forEach((btn) => {
      btn.addEventListener("click", async (ev) => {
        ev.stopPropagation();
        const nId = btn.getAttribute("data-note-id");
        const item = localNotes.find((x) => x.id === nId);
        if (item) {
          item.isDeleted = true;
          item.updatedAt = Date.now();
          await SyncClient.saveLocalData(localEvents, localTasks, localNotes);
          renderNotes();
          renderCalendar();
          renderSelectedDayInfo();
          showToast("یادداشت با موفقیت حذف شد", "info");
        }
      });
    });
  }

  // ثبت یادداشت جدید
  if (btnAddNote) {
    btnAddNote.addEventListener("click", async () => {
      const title = (newNoteTitleInput?.value || "").trim();
      const content = (newNoteContentInput?.value || "").trim();

      if (!content && !title) {
        alert("لطفاً متن یادداشت را وارد کنید.");
        return;
      }

      if (w7StickyStatus) w7StickyStatus.textContent = "در حال ذخیره...";

      const newNote = {
        id: "note_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7),
        title: title || "یادداشت لپ‌تاپ",
        content: content,
        persianDate: PersianDateUtil.formatJalali(selectedDate),
        colorHex: "#FEF3C7",
        isPinned: false,
        updatedAt: Date.now(),
        isDeleted: false
      };

      localNotes.unshift(newNote);
      await SyncClient.saveLocalData(localEvents, localTasks, localNotes);

      if (newNoteTitleInput) newNoteTitleInput.value = "";
      if (newNoteContentInput) newNoteContentInput.value = "";
      if (w7StickyStatus) {
        w7StickyStatus.textContent = "ذخیره شد ✓";
        setTimeout(() => { w7StickyStatus.textContent = "آماده"; }, 2500);
      }

      renderNotes();
      renderCalendar();
      renderSelectedDayInfo();
      showToast("یادداشت جدید ذخیره شد ✓ (آماده ارسال به گوشی)", "success");
    });
  }

  // =========================================================================
  // 8. HEADER DIRECT TWO-WAY SYNC WITH ANDROID PHONE
  // =========================================================================
  const btnHeaderSync = document.getElementById("btnHeaderSync");
  const headerSyncText = document.getElementById("headerSyncText");

  if (btnHeaderSync) {
    btnHeaderSync.addEventListener("click", async () => {
      btnHeaderSync.disabled = true;
      btnHeaderSync.classList.add("spinning");
      if (headerSyncText) headerSyncText.textContent = "درحال شناسایی گوشی...";

      try {
        let result = await SyncClient.syncWithAndroid();

        if (!result.success) {
          // در صورت عدم شناسایی خودکار، آدرس دقیق از کاربر پرسیده می‌شود
          const savedUrl = await SyncClient.getServerUrl();
          const manualUrl = prompt(
            "اتصال خودکار به گوشی برقرار نشد.\nلطفاً آدرس IP سروری که در گوشی نمایش داده شده را وارد کنید:\n(مثلاً: http://192.168.43.1:8080)",
            savedUrl || "http://192.168.43.1:8080"
          );
          if (manualUrl && manualUrl.trim()) {
            const cleanManual = SyncClient.cleanServerUrl(manualUrl);
            if (headerSyncText) headerSyncText.textContent = "درحال اتصال...";
            await SyncClient.setServerUrl(cleanManual);
            result = await SyncClient.syncWithAndroid(cleanManual);
          }
        }

        if (result.success) {
          await refreshData();
          if (headerSyncText) headerSyncText.textContent = "همگام شد ✓";
          showToast(`همگام‌سازی انجام شد! (${result.eventsCount} رویداد، ${result.tasksCount} کار، ${result.notesCount} یادداشت)`, "success");
          setTimeout(() => {
            if (headerSyncText) headerSyncText.textContent = "همگام‌سازی با گوشی";
          }, 4000);
        } else {
          if (headerSyncText) headerSyncText.textContent = "خطا در اتصال";
          showToast("عدم امکان ارتباط با سرور گوشی: " + (result.error || "پاسخی دریافت نشد"), "error");
          setTimeout(() => {
            if (headerSyncText) headerSyncText.textContent = "همگام‌سازی با گوشی";
          }, 4000);
        }
      } catch (err) {
        if (headerSyncText) headerSyncText.textContent = "خطا در اتصال";
        showToast("خطا در شبکه: " + err.message, "error");
      } finally {
        btnHeaderSync.disabled = false;
        btnHeaderSync.classList.remove("spinning");
      }
    });
  }

  // Load and refresh initial data
  async function refreshData() {
    const data = await SyncClient.getLocalData();
    localEvents = data.events || [];
    localTasks = data.tasks || [];
    localNotes = data.notes || [];
    renderCalendar();
    renderSelectedDayInfo();
    renderTasks();
    renderNotes();
  }

  await PersianOccasions.loadOfficialData(currentYear);
  await refreshData();
});
