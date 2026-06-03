import { apiFetch } from "./http.js";

export function fetchEpgStatus() {
  return apiFetch("/api/admin/epg/status");
}

export function fetchEpgMapping({ search = "", filter = "all", page = 1, limit = 50 } = {}) {
  const q = new URLSearchParams();
  if (search) q.set("search", search);
  if (filter && filter !== "all") q.set("filter", filter);
  q.set("page", String(page));
  q.set("limit", String(limit));
  return apiFetch(`/api/admin/epg/mapping?${q}`);
}

export function searchEpgXmltvSources(search = "", limit = 40) {
  const q = new URLSearchParams();
  if (search) q.set("search", search);
  q.set("limit", String(limit));
  return apiFetch(`/api/admin/epg/xmltv-sources?${q}`);
}

export function refreshEpgXmltvSources() {
  return apiFetch("/api/admin/epg/xmltv-sources?refresh=1&limit=1");
}

export function updateChannelEpgMapping(channelId, epgId) {
  return apiFetch(`/api/admin/epg/mapping/${channelId}`, {
    method: "PATCH",
    body: JSON.stringify({ epgId: epgId || null }),
  });
}

export function autoMapEpg(onlyEmpty = true) {
  return apiFetch("/api/admin/epg/auto-map", {
    method: "POST",
    body: JSON.stringify({ onlyEmpty }),
  });
}

export function syncEpg(url) {
  return apiFetch("/api/admin/epg/sync", {
    method: "POST",
    body: JSON.stringify(url ? { url } : {}),
  });
}
