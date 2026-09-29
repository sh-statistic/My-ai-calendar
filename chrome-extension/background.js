/**
 * Background Service Worker for Hamgam Extension
 * Zero-Cost Serverless Offline Architecture
 */

importScripts("persian-date.js", "sync-client.js");

chrome.runtime.onInstalled.addListener(() => {
  // Set periodic alarm every 30 minutes for optional background sync if on same Wi-Fi
  chrome.alarms.create("hamgam-periodic-sync", { periodInMinutes: 30 });
  updateBadge();
});

chrome.alarms.onAlarm.addListener(async (alarm) => {
  if (alarm.name === "hamgam-periodic-sync") {
    try {
      const serverUrl = await SyncClient.getServerUrl();
      const status = await SyncClient.pingServer(serverUrl);
      if (status) {
        await SyncClient.syncWithAndroid(serverUrl);
        updateBadge();
      }
    } catch (_) {}
  }
});

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === "sync_now") {
    SyncClient.syncWithAndroid(request.serverUrl)
      .then((res) => {
        updateBadge();
        sendResponse(res);
      })
      .catch((err) => sendResponse({ success: false, error: err.message }));
    return true; // Keep message channel open for async response
  }

  if (request.action === "get_status") {
    SyncClient.pingServer(request.serverUrl)
      .then((res) => sendResponse(res))
      .catch(() => sendResponse(null));
    return true;
  }
});

async function updateBadge() {
  try {
    const data = await SyncClient.getLocalData();
    const pendingTasks = (data.tasks || []).filter((t) => !t.isCompleted && !t.isDeleted).length;
    if (pendingTasks > 0) {
      chrome.action.setBadgeText({ text: String(pendingTasks) });
      chrome.action.setBadgeBackgroundColor({ color: "#F59E0B" });
    } else {
      chrome.action.setBadgeText({ text: "" });
    }
  } catch (_) {}
}
