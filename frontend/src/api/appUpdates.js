import { API_BASE } from "./client.js";
import { getToken } from "./auth.js";
import { apiFetch } from "./http.js";

const ADMIN_KEY = import.meta.env.VITE_ADMIN_API_KEY || "";

export async function getAppUpdates() {
  return apiFetch("/api/admin/app-updates");
}

export async function deleteAppUpdate(id) {
  return apiFetch(`/api/admin/app-updates/${id}`, { method: "DELETE" });
}

export async function createAppUpdate(formData) {
  // We cannot use apiFetch directly because it forces Content-Type: application/json
  // We need multipart/form-data for the file upload.
  const headers = {};
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  } else if (ADMIN_KEY) {
    headers["X-Admin-Key"] = ADMIN_KEY;
  }

  const res = await fetch(`${API_BASE}/api/admin/app-updates`, {
    method: "POST",
    headers,
    body: formData,
  });

  const data = await res.json().catch(() => ({}));

  if (!res.ok) {
    throw new Error(data.error || `HTTP ${res.status}`);
  }
  return data;
}
