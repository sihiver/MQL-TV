import { apiFetch } from "./http.js";

const TOKEN_KEY = "nv_admin_token";
const USER_KEY = "nv_admin_user";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setSession(token, user) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export async function loginAdmin(email, password) {
  const data = await apiFetch("/api/auth/admin/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
    skipAuth: true,
  });
  setSession(data.token, data.user);
  return data;
}

export async function fetchMe() {
  return apiFetch("/api/auth/me");
}

export async function logout() {
  const token = getToken();
  if (token) {
    try {
      await apiFetch("/api/auth/logout", { method: "POST" });
    } catch {
      /* abaikan jika token sudah invalid */
    }
  }
  clearSession();
}
