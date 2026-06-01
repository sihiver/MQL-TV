import { API_BASE } from "./client.js";
import { clearSession, getToken } from "./auth.js";

const ADMIN_KEY = import.meta.env.VITE_ADMIN_API_KEY || "";

export async function apiFetch(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  const token = !options.skipAuth ? getToken() : null;
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  } else if (ADMIN_KEY) {
    headers["X-Admin-Key"] = ADMIN_KEY;
  }

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
  const data = await res.json().catch(() => ({}));

  if (res.status === 401 && !options.skipAuth) {
    clearSession();
    window.dispatchEvent(new Event("auth:logout"));
  }

  if (!res.ok) {
    throw new Error(data.error || `HTTP ${res.status}`);
  }
  return data;
}
