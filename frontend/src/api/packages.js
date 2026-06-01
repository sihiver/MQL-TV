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
