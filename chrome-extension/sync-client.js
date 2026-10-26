/**
 * Zero-Cost Serverless Offline Sync Client for Chrome Extension
 * Uses chrome.storage.local for 100% offline data persistence on device
 * Syncs directly with Android embedded HTTP server over local Wi-Fi / Hotspot
 */

const SyncClient = {
  DEFAULT_SERVER_URL: "http://192.168.43.1:8080",

  /**
   * تمیزکاری و استانداردسازی آدرس سرور (پروتکل، هاست و پورت)
   */
  cleanServerUrl(url) {
    if (!url) return "";
    let clean = url.trim();
    if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
      clean = "http://" + clean;
    }
    try {
      const u = new URL(clean);
      return `${u.protocol}//${u.host}`;
    } catch {
      return clean.replace(/\/+$/, "");
    }
  },

  async getStorage(keys) {
    if (typeof chrome !== "undefined" && chrome.storage && chrome.storage.local) {
      return new Promise((resolve) => chrome.storage.local.get(keys, resolve));
    }
    const res = {};
    keys.forEach((k) => {
      const val = localStorage.getItem(k);
      try {
        res[k] = val ? JSON.parse(val) : undefined;
      } catch {
        res[k] = val;
      }
    });
    return res;
  },

  async setStorage(obj) {
    if (typeof chrome !== "undefined" && chrome.storage && chrome.storage.local) {
      return new Promise((resolve) => chrome.storage.local.set(obj, resolve));
    }
    Object.keys(obj).forEach((k) => {
      localStorage.setItem(k, JSON.stringify(obj[k]));
    });
  },

  async getServerUrl() {
    const data = await this.getStorage(["serverUrl"]);
    return this.cleanServerUrl(data.serverUrl || this.DEFAULT_SERVER_URL);
  },

  async setServerUrl(url) {
    const clean = this.cleanServerUrl(url);
    if (clean) {
      await this.setStorage({ serverUrl: clean });
    }
    return clean;
  },

  /**
   * شناسایی خودکار آدرس سرور گوشی از روی تب‌های باز در مرورگر کروم
   * (اگر کاربر آدرس سرور گوشی را در نوار آدرس یکی از تب‌ها باز کرده باشد)
   */
  async detectServerUrlFromOpenTabs() {
    if (typeof chrome !== "undefined" && chrome.tabs && chrome.tabs.query) {
      try {
        const tabs = await new Promise((resolve) => chrome.tabs.query({}, resolve));
        for (const tab of tabs || []) {
          if (tab.url) {
            const match = tab.url.match(/^(https?:\/\/(?:192\.168\.\d+\.\d+|10\.\d+\.\d+\.\d+|172\.(?:1[6-9]|2\d|3[0-1])\.\d+\.\d+|localhost):8080)/i);
            if (match) {
              const detected = this.cleanServerUrl(match[1]);
              await this.setServerUrl(detected);
              return detected;
            }
          }
        }
      } catch (_) {}
    }
    return null;
  },

  /**
   * Smart auto-discovery of Android server across Cable (USB/ADB), Hotspot, USB Tethering and Wi-Fi subnets
   */
  async findActiveServerUrl() {
    // ۰. اولویت اول: بررسی اتصال با کابل (Localhost / ADB Port Forwarding)
    const localCableUrls = ["http://localhost:8080", "http://127.0.0.1:8080"];
    for (const localUrl of localCableUrls) {
      const ping = await this.pingServer(localUrl, 800);
      if (ping && ping.status === "online") {
        await this.setServerUrl(localUrl);
        return localUrl;
      }
    }

    // ۱. بررسی تب‌های باز شده در مرورگر کروم
    const tabUrl = await this.detectServerUrlFromOpenTabs();
    if (tabUrl) {
      const ping = await this.pingServer(tabUrl, 1500);
      if (ping && ping.status === "online") return tabUrl;
    }

    // ۲. بررسی آخرین آدرس موفق ذخیره‌شده
    const saved = await this.getServerUrl();
    if (saved && !localCableUrls.includes(saved)) {
      const ping = await this.pingServer(saved, 1500);
      if (ping && ping.status === "online") return saved;
    }

    // ۳. اولویت سوم: تست اتصال با کابل از طریق تترینگ اندروید (USB Tethering)
    const usbTetheringUrls = [
      "http://192.168.42.129:8080",
      "http://192.168.42.1:8080",
      "http://192.168.42.2:8080"
    ];
    for (const tetherUrl of usbTetheringUrls) {
      const pingTether = await this.pingServer(tetherUrl, 1000);
      if (pingTether && pingTether.status === "online") {
        await this.setServerUrl(tetherUrl);
        return tetherUrl;
      }
    }

    // ۴. تست هات‌اسپات نقطه اتصال پیش‌فرض اندروید (192.168.43.1)
    const hotspotUrl = "http://192.168.43.1:8080";
    if (saved !== hotspotUrl) {
      const pingHotspot = await this.pingServer(hotspotUrl, 1500);
      if (pingHotspot && pingHotspot.status === "online") {
        await this.setServerUrl(hotspotUrl);
        return hotspotUrl;
      }
    }

    // ۵. اسکن موازی آدرس‌های متداول وای‌فای (مودم‌های خانگی و سیم‌کارتی متداول در ایران)
    const candidateIps = [
      "http://192.168.1.100:8080", "http://192.168.1.101:8080", "http://192.168.1.102:8080",
      "http://192.168.1.103:8080", "http://192.168.1.104:8080", "http://192.168.1.105:8080",
      "http://192.168.1.2:8080", "http://192.168.1.3:8080", "http://192.168.1.4:8080", "http://192.168.1.5:8080",
      "http://192.168.0.100:8080", "http://192.168.0.101:8080", "http://192.168.0.102:8080", "http://192.168.0.105:8080",
      "http://192.168.0.2:8080", "http://192.168.0.3:8080",
      "http://192.168.8.100:8080", "http://192.168.8.101:8080", "http://192.168.8.2:8080", // مودم‌های ایرانسل و همراه اول
      "http://192.168.31.100:8080", "http://192.168.31.2:8080" // روترهای شیائومی
    ];

    let found = null;
    const checks = candidateIps.map(async (u) => {
      if (found) return;
      const res = await this.pingServer(u, 1500);
      if (res && res.status === "online") {
        found = u;
      }
    });

    await Promise.all(checks);
    if (found) {
      await this.setServerUrl(found);
      return found;
    }

    return saved || "http://localhost:8080";
  },

  /**
   * بررسی اختصاصی اتصال با کابل (USB)
   */
  async checkUsbConnection() {
    const cableCandidates = [
      "http://localhost:8080",
      "http://127.0.0.1:8080",
      "http://192.168.42.129:8080",
      "http://192.168.42.1:8080"
    ];

    for (const url of cableCandidates) {
      const res = await this.pingServer(url, 1200);
      if (res && res.status === "online") {
        await this.setServerUrl(url);
        return { success: true, url, status: res };
      }
    }
    return { success: false };
  },

  async getLocalData() {
    const data = await this.getStorage(["events", "tasks", "notes"]);
    return {
      deviceId: "Chrome-Extension",
      timestamp: Date.now(),
      events: data.events || [],
      tasks: data.tasks || [],
      notes: data.notes || []
    };
  },

  async saveLocalData(events, tasks, notes) {
    await this.setStorage({ events, tasks, notes, lastLocalUpdate: Date.now() });
  },

  /**
   * Ping server: ارجاع به Background Service Worker در صفحات رابط کاربری
   * جهت عبور تضمینی از موانع CORS و Private Network Access مرورگر کروم
   */
  async pingServer(serverUrl, timeoutMs = 3500) {
    const clean = this.cleanServerUrl(serverUrl || (await this.getServerUrl()));

    if (typeof window !== "undefined" && typeof chrome !== "undefined" && chrome.runtime && chrome.runtime.sendMessage) {
      try {
        const bgRes = await new Promise((resolve) => {
          chrome.runtime.sendMessage(
            { action: "ping_server", serverUrl: clean, timeoutMs },
            (resp) => {
              if (chrome.runtime.lastError || !resp) {
                resolve(null);
              } else {
                resolve(resp);
              }
            }
          );
        });
        if (bgRes && bgRes.status === "online") return bgRes;
      } catch (_) {}
    }

    return this.directPingServer(clean, timeoutMs);
  },

  async directPingServer(serverUrl, timeoutMs = 3500) {
    const url = this.cleanServerUrl(serverUrl || (await this.getServerUrl()));
    if (!url) return null;

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    try {
      const res = await fetch(`${url}/api/status`, {
        method: "GET",
        headers: { Accept: "application/json" },
        signal: controller.signal
      });
      clearTimeout(timeoutId);
      if (res.ok) {
        return await res.json();
      }
      return null;
    } catch (_) {
      clearTimeout(timeoutId);
      return null;
    }
  },

  /**
   * Primary Wi-Fi / Hotspot Local Sync with timeout and live step callbacks
   */
  async syncWithAndroid(customUrl = null, onProgress = () => {}) {
    onProgress(`آماده‌سازی برای همگام‌سازی...`);
    let serverUrl = customUrl ? this.cleanServerUrl(customUrl) : await this.findActiveServerUrl();
    if (!serverUrl) {
      serverUrl = await this.getServerUrl();
    }
    serverUrl = this.cleanServerUrl(serverUrl);
    await this.setServerUrl(serverUrl);

    // اگر در پنجره صفحه وب هستیم، برای عبور از موانع امنیتی CORS / PNA، عملیات را از طریق Background Service Worker اجرا می‌کنیم
    if (typeof window !== "undefined" && typeof chrome !== "undefined" && chrome.runtime && chrome.runtime.sendMessage) {
      onProgress(`ارسال درخواست همگام‌سازی به سرویس پس‌زمینه افزونه (${serverUrl})...`);
      try {
        const bgResult = await new Promise((resolve) => {
          chrome.runtime.sendMessage(
            { action: "sync_now", serverUrl: serverUrl },
            (response) => {
              if (chrome.runtime.lastError) {
                resolve({ success: false, error: chrome.runtime.lastError.message });
              } else {
                resolve(response || { success: false, error: "پاسخی از سرویس پس‌زمینه دریافت نشد" });
              }
            }
          );
        });

        if (bgResult && bgResult.success) {
          onProgress(`همگام‌سازی با موفقیت انجام شد ✓`);
          return bgResult;
        } else if (bgResult && bgResult.error) {
          onProgress(`تلاش با اتصال مستقیم...`);
        }
      } catch (err) {
        onProgress(`خطای ارتباط با Background: ${err.message}`);
      }
    }

    // Direct sync fallback
    return this.directSyncWithAndroid(serverUrl, onProgress);
  },

  async directSyncWithAndroid(serverUrl, onProgress = () => {}) {
    onProgress(`آماده‌سازی داده‌های محلی افزونه...`);
    const localData = await this.getLocalData();

    onProgress(`ارسال داده‌ها به گوشی (${serverUrl}/api/sync)...`);

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 8000);

    try {
      const response = await fetch(`${serverUrl}/api/sync`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json; charset=utf-8",
          Accept: "application/json"
        },
        body: JSON.stringify(localData),
        signal: controller.signal
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        throw new Error(`خطای سرور گوشی: کد ${response.status} (${response.statusText})`);
      }

      onProgress(`داده‌ها با موفقیت توسط گوشی پردازش شد. دریافت نسخه یکپارچه...`);
      const mergedData = await response.json();

      // ذخیره داده‌های ادغام‌شده در storage محلی افزونه
      await this.setStorage({
        events: mergedData.events || [],
        tasks: mergedData.tasks || [],
        notes: mergedData.notes || [],
        lastSyncTimestamp: Date.now(),
        lastSyncStatus: "success",
        serverUrl: serverUrl
      });

      onProgress(`ذخیره در حافظه افزونه تکمیل شد ✓`);

      return {
        success: true,
        eventsCount: (mergedData.events || []).length,
        tasksCount: (mergedData.tasks || []).length,
        notesCount: (mergedData.notes || []).length,
        timestamp: Date.now()
      };
    } catch (error) {
      clearTimeout(timeoutId);
      let errMsg = error.message;
      if (error.name === "AbortError") {
        errMsg = "تایم‌اوت (پاسخی دریافت نشد). آیا گوشی و لپ‌تاپ به یک شبکه Wi-Fi متصل هستند؟";
      } else if (errMsg.includes("Failed to fetch") || errMsg.includes("NetworkError")) {
        errMsg = `عدم امکان اتصال به گوشی در ${serverUrl}. لطفاً مطمئن شوید سرور در گوشی روشن است یا اگر فیلترشکن لپ‌تاپ روشن است، گزینه Allow LAN Traffic را فعال کنید.`;
      }

      await this.setStorage({
        lastSyncStatus: "error",
        lastSyncError: errMsg
      });

      return {
        success: false,
        error: errMsg
      };
    }
  }
};
