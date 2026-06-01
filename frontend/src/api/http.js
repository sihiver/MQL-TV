import { API_BASE } from "./client.js";

const ADMIN_KEY = import.meta.env.VITE_ADMIN_API_KEY || "";

export async function apiFetch(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...options.headers,
  };
  if (ADMIN_KEY) headers["X-Admin-Key"] = ADMIN_KEY;

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const data = await res.json().catch(() => ({}));

  if (!res.ok) {
    throw new Error(data.error || `HTTP ${res.status}`);
  }
  return data;
}
