import { apiFetch } from "./http.js";

export function fetchAdminChannels({ search = "", category, page = 1, limit = 50 } = {}) {
  const q = new URLSearchParams();
  if (search) q.set("search", search);
  if (category && category !== "Semua") q.set("category", category);
  q.set("page", String(page));
  q.set("limit", String(limit));
  return apiFetch(`/api/admin/channels?${q}`);
}

export function fetchChannelCategories() {
  return apiFetch("/api/admin/channels/categories/list");
}

export function importChannelsFromJson(data, mode = "replace") {
  return apiFetch("/api/admin/channels/import", {
    method: "POST",
    body: JSON.stringify({ data, mode }),
  });
}

export function createChannel(body) {
  return apiFetch("/api/admin/channels", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updateChannel(id, body) {
  return apiFetch(`/api/admin/channels/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export function toggleChannel(id, field = "active") {
  return apiFetch(`/api/admin/channels/${id}/toggle`, {
    method: "PATCH",
    body: JSON.stringify({ field }),
  });
}

export function deleteChannel(id) {
  return apiFetch(`/api/admin/channels/${id}`, { method: "DELETE" });
}
