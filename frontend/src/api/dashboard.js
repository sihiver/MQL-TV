import { apiFetch } from "./http.js";

export function fetchDashboardStats() {
  return apiFetch("/api/admin/stats");
}
