import { apiFetch } from "./http.js";

export function fetchPackages({ active, search = "" } = {}) {
  const q = new URLSearchParams();
  if (active !== undefined) q.set("active", String(active));
  if (search) q.set("search", search);
  const qs = q.toString();
  return apiFetch(`/api/admin/packages${qs ? `?${qs}` : ""}`);
}

export function createPackage(body) {
  return apiFetch("/api/admin/packages", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updatePackage(id, body) {
  return apiFetch(`/api/admin/packages/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export function deletePackage(id) {
  return apiFetch(`/api/admin/packages/${id}`, { method: "DELETE" });
}

export function fetchPackageChannels(packageId, { search = "", page = 1, limit = 50 } = {}) {
  const q = new URLSearchParams({ page: String(page), limit: String(limit) });
  if (search) q.set("search", search);
  return apiFetch(`/api/admin/packages/${packageId}/channels?${q}`);
}

export function fetchPackageChannelsAvailable(
  packageId,
  { search = "", category, page = 1, limit = 30 } = {},
) {
  const q = new URLSearchParams({ page: String(page), limit: String(limit) });
  if (search) q.set("search", search);
  if (category) q.set("category", category);
  return apiFetch(`/api/admin/packages/${packageId}/channels/available?${q}`);
}

export function addChannelsToPackage(packageId, channelIds) {
  return apiFetch(`/api/admin/packages/${packageId}/channels`, {
    method: "POST",
    body: JSON.stringify({ channelIds }),
  });
}

export function addPackageChannelsByCategory(packageId, categories) {
  return apiFetch(`/api/admin/packages/${packageId}/channels/by-category`, {
    method: "POST",
    body: JSON.stringify({ categories }),
  });
}

export function removeChannelFromPackage(packageId, channelId) {
  return apiFetch(`/api/admin/packages/${packageId}/channels/${channelId}`, {
    method: "DELETE",
  });
}

export function clearPackageChannels(packageId) {
  return apiFetch(`/api/admin/packages/${packageId}/channels`, { method: "DELETE" });
}
