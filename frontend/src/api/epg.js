import { apiFetch } from "./http.js";

export function fetchEpgStatus() {
  return apiFetch("/api/admin/epg/status");
}

export function syncEpg(url) {
  return apiFetch("/api/admin/epg/sync", {
    method: "POST",
    body: JSON.stringify(url ? { url } : {}),
  });
}
