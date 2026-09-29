/**
 * Zero-Cost Serverless Offline Sync Client for Chrome Extension
 * Uses chrome.storage.local for 100% offline data persistence on device
 * Syncs directly with Android embedded HTTP server over local Wi-Fi / Hotspot
 */

const SyncClient = {
  DEFAULT_SERVER_URL: "http://192.168.1.100:8080",

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

  async pingServer(serverUrl) {
    const url = serverUrl || (await this.getServerUrl());
    try {
      const res = await fetch(`${url}/api/status`, {
        method: "GET",
        headers: { Accept: "application/json" }
      });
      if (res.ok) {
        return await res.json();
      }
      return null;
    } catch (e) {
      return null;
    }
  },

  /**
   * Primary Wi-Fi / Hotspot Local Sync:
   * Sends local changes and receives merged dataset from Android Room database
   */
  async syncWithAndroid(customUrl = null) {
    const serverUrl = customUrl || (await this.getServerUrl());
    const localData = await this.getLocalData();

    try {
      const response = await fetch(`${serverUrl}/api/sync`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json; charset=utf-8",
          Accept: "application/json"
        },
        body: JSON.stringify(localData)
      });

      if (!response.ok) {
        throw new Error(`HTTP Error ${response.status}: ${response.statusText}`);
      }

      const mergedData = await response.json();

      // Update chrome.storage.local with merged items
      await this.setStorage({
        events: mergedData.events || [],
        tasks: mergedData.tasks || [],
        notes: mergedData.notes || [],
        lastSyncTimestamp: Date.now(),
        lastSyncStatus: "success"
      });

      return {
        success: true,
        eventsCount: (mergedData.events || []).length,
        tasksCount: (mergedData.tasks || []).length,
        notesCount: (mergedData.notes || []).length,
        timestamp: Date.now()
      };
    } catch (error) {
      await this.setStorage({
        lastSyncStatus: "error",
        lastSyncError: error.message
      });
      return {
        success: false,
        error: error.message
      };
    }
  },

  /**
   * Alternative: Web Bluetooth BLE sync when Wi-Fi is unavailable
   */
  async syncWithBluetooth() {
    if (!navigator.bluetooth) {
      throw new Error("Web Bluetooth API توسط مرورگر شما پشتیبانی نمی‌شود.");
    }

    const SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    const SYNC_CHAR_UUID = "0000fff1-0000-1000-8000-00805f9b34fb";

    try {
      const device = await navigator.bluetooth.requestDevice({
        filters: [{ services: [SERVICE_UUID] }]
      });

      const server = await device.gatt.connect();
      const service = await server.getPrimaryService(SERVICE_UUID);
      const characteristic = await service.getCharacteristic(SYNC_CHAR_UUID);

      const localData = await this.getLocalData();
      const jsonStr = JSON.stringify(localData);
      const encoder = new TextEncoder();
      await characteristic.writeValue(encoder.encode(jsonStr));

      return { success: true, deviceName: device.name || "Hamgam Android BLE" };
    } catch (err) {
      throw err;
    }
  }
};
