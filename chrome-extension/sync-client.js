/**
 * Zero-Cost Serverless Offline Sync Client for Chrome Extension
 * Uses chrome.storage.local for 100% offline data persistence on device
 * Syncs directly with Android embedded HTTP server over local Wi-Fi / Hotspot / Web Bluetooth BLE
 */

const SyncClient = {
  DEFAULT_SERVER_URL: "http://192.168.43.1:8080",

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
    return data.serverUrl || this.DEFAULT_SERVER_URL;
  },

  async setServerUrl(url) {
    let clean = url.trim().replace(/\/+$/, "");
    if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
      clean = "http://" + clean;
    }
    await this.setStorage({ serverUrl: clean });
    return clean;
  },

  /**
   * Smart auto-discovery of Android server across Hotspot and Wi-Fi subnets
   */
  async findActiveServerUrl() {
    const saved = await this.getServerUrl();
    if (saved) {
      const ping = await this.pingServer(saved, 1000);
      if (ping && ping.status === "online") return saved;
    }

    // 1. تست هات‌اسپات پیش‌فرض اندروید (192.168.43.1)
    const hotspotUrl = "http://192.168.43.1:8080";
    if (saved !== hotspotUrl) {
      const pingHotspot = await this.pingServer(hotspotUrl, 1000);
      if (pingHotspot && pingHotspot.status === "online") {
        await this.setServerUrl(hotspotUrl);
        return hotspotUrl;
      }
    }

    // 2. اسکن موازی سایر آدرس‌های متداول در شبکه
    const candidateIps = [
      "http://192.168.1.100:8080",
      "http://192.168.1.101:8080",
      "http://192.168.1.102:8080",
      "http://192.168.1.105:8080",
      "http://192.168.0.100:8080",
      "http://192.168.0.101:8080",
      "http://192.168.0.102:8080",
      "http://192.168.43.1:8080"
    ];

    let found = null;
    const checks = candidateIps.map(async (u) => {
      if (found) return;
      const res = await this.pingServer(u, 1200);
      if (res && res.status === "online") {
        found = u;
      }
    });

    await Promise.all(checks);
    if (found) {
      await this.setServerUrl(found);
      return found;
    }

    return saved || this.DEFAULT_SERVER_URL;
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

  async pingServer(serverUrl, timeoutMs = 3500) {
    let url = serverUrl || (await this.getServerUrl());
    url = (url || "").trim().replace(/\/+$/, "");
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      url = "http://" + url;
    }
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
    onProgress(`شناسایی خودکار سرور گوشی...`);
    let serverUrl = customUrl;
    if (!serverUrl) {
      serverUrl = await this.findActiveServerUrl();
    }
    serverUrl = (serverUrl || "").trim().replace(/\/+$/, "");
    if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
      serverUrl = "http://" + serverUrl;
    }
    onProgress(`آماده‌سازی داده‌های محلی افزونه...`);
    const localData = await this.getLocalData();

    onProgress(`ارسال داده‌ها به گوشی (${serverUrl}/api/sync)...`);

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 6000);

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
        lastSyncStatus: "success"
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
        errMsg = "تایم‌اوت (پاسخی دریافت نشد). گوشی در دسترس نیست یا آی‌پی وارد شده نادرست است.";
      } else if (errMsg.includes("Failed to fetch") || errMsg.includes("NetworkError")) {
        errMsg = "عدم امکان اتصال به گوشی (اتصال شبکه برقرار نشد). آیا برنامه در گوشی باز است؟ اگر فیلترشکن لپ‌تاپ روشن است، گزینه Allow LAN Traffic را فعال کنید.";
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
  },

  /**
   * Complete Two-Way Web Bluetooth BLE sync:
   * 1. Sends Chrome extension changes to Android Room DB via Bluetooth characteristic write
   * 2. Reads latest merged Android database via Bluetooth characteristic read
   */
  async syncWithBluetooth() {
    if (!navigator.bluetooth) {
      throw new Error("مرورگر شما از قابلیت Web Bluetooth پشتیبانی نمی‌کند یا دسترسی به آن در این محیط محدود است.");
    }

    const SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    const SYNC_CHAR_UUID = "0000fff1-0000-1000-8000-00805f9b34fb";

    try {
      let device;
      try {
        device = await navigator.bluetooth.requestDevice({
          filters: [
            { services: [SERVICE_UUID] },
            { namePrefix: "Hamgam" }
          ],
          optionalServices: [SERVICE_UUID]
        });
      } catch (_) {
        // Fallback: جستجوی گسترده تمامی دستگاه‌های در دسترس با سرویس اختصاصی
        device = await navigator.bluetooth.requestDevice({
          acceptAllDevices: true,
          optionalServices: [SERVICE_UUID]
        });
      }

      if (!device) {
        throw new Error("دستگاهی برای اتصال انتخاب نشد.");
      }

      if (!device.gatt) {
        throw new Error("دستگاه انتخاب شده از پروتکل GATT پشتیبانی نمی‌کند.");
      }

      const server = await device.gatt.connect();
      const service = await server.getPrimaryService(SERVICE_UUID);
      const characteristic = await service.getCharacteristic(SYNC_CHAR_UUID);

      // 1. ارسال تغییرات محلی افزونه به گوشی
      const localData = await this.getLocalData();
      const jsonStr = JSON.stringify(localData);
      const encoder = new TextEncoder();
      const dataBytes = encoder.encode(jsonStr);

      try {
        if (characteristic.writeValueWithResponse) {
          await characteristic.writeValueWithResponse(dataBytes);
        } else {
          await characteristic.writeValue(dataBytes);
        }
      } catch (_) {
        // ادامه برای خواندن
      }

      // 2. دریافت داده‌های همگام‌شده دیتابیس از گوشی
      const value = await characteristic.readValue();
      const decoder = new TextDecoder("utf-8");
      const receivedJson = decoder.decode(value);

      if (receivedJson && receivedJson.trim().startsWith("{")) {
        const mergedData = JSON.parse(receivedJson);
        await this.setStorage({
          events: mergedData.events || [],
          tasks: mergedData.tasks || [],
          notes: mergedData.notes || [],
          lastSyncTimestamp: Date.now(),
          lastSyncStatus: "success"
        });

        return {
          success: true,
          deviceName: device.name || "گوشی اندروید",
          eventsCount: (mergedData.events || []).length,
          tasksCount: (mergedData.tasks || []).length,
          notesCount: (mergedData.notes || []).length
        };
      }

      return { success: true, deviceName: device.name || "گوشی اندروید" };
    } catch (err) {
      if (err.name === "NotFoundError" || (err.message && err.message.includes("User cancelled"))) {
        throw new Error("پنجره انتخاب دستگاه لغو شد یا گوشی در لیست بلوتوث پیدا نشد.");
      }
      if (err.name === "SecurityError") {
        throw new Error("دسترسی به بلوتوث توسط مرورگر مسدود شده است.");
      }
      throw err;
    }
  }
};
