import { apiFetch } from "./http.js";

export function fetchSubscriptions({ filter = "Semua", search = "" } = {}) {
  const q = new URLSearchParams();
  if (filter) q.set("filter", filter);
  if (search) q.set("search", search);
  const qs = q.toString();
  return apiFetch(`/api/admin/subscriptions${qs ? `?${qs}` : ""}`);
}

export function createSubscription(body) {
  return apiFetch("/api/admin/subscriptions", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updateSubscription(id, body) {
  return apiFetch(`/api/admin/subscriptions/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export function renewSubscription(id, months = 1) {
  return apiFetch(`/api/admin/subscriptions/${id}/renew`, {
    method: "PATCH",
    body: JSON.stringify({ months }),
  });
}

export function deleteSubscription(id) {
  return apiFetch(`/api/admin/subscriptions/${id}`, { method: "DELETE" });
}
