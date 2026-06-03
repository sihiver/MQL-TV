import { apiFetch } from "./http.js";

export function fetchServerSettings() {
  return apiFetch("/api/admin/settings");
}

export function saveServerSettings(body) {
  return apiFetch("/api/admin/settings", {
    method: "PUT",
    body: JSON.stringify(body),
  });
}
