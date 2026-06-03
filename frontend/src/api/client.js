const rawApiBase = import.meta.env.VITE_API_URL?.trim();
// In production, prefer same-origin requests (via reverse proxy) when VITE_API_URL is not set.
const API_BASE = rawApiBase ? rawApiBase.replace(/\/$/, "") : "";

let healthPromise = null;
let lastHealthAt = 0;
const HEALTH_CACHE_MS = 15_000;

export async function checkHealth({ force = false } = {}) {
  const now = Date.now();
  if (!force && healthPromise && now - lastHealthAt < HEALTH_CACHE_MS) {
    return healthPromise;
  }

  lastHealthAt = now;
  healthPromise = fetch(`${API_BASE}/health`)
    .then((res) => {
      if (!res.ok) throw new Error("API offline");
      return res.json();
    })
    .catch((err) => {
      healthPromise = null;
      throw err;
    });

  return healthPromise;
}

export { API_BASE };
