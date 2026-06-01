import { apiFetch } from "./http.js";

export function fetchUsers({ search = "", filter = "Semua" } = {}) {
  const q = new URLSearchParams();
  if (search) q.set("search", search);
  if (filter) q.set("filter", filter);
  const qs = q.toString();
  return apiFetch(`/api/admin/users${qs ? `?${qs}` : ""}`);
}

export function fetchUser(id) {
  return apiFetch(`/api/admin/users/${id}`);
}

export function createUser(body) {
  return apiFetch("/api/admin/users", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updateUser(id, body) {
  return apiFetch(`/api/admin/users/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export function deleteUser(id) {
  return apiFetch(`/api/admin/users/${id}`, { method: "DELETE" });
}

export function setUserBanned(id, banned) {
  return apiFetch(`/api/admin/users/${id}/ban`, {
    method: "PATCH",
    body: JSON.stringify({ banned }),
  });
}
