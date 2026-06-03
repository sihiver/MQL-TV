const rawApiBase = import.meta.env.VITE_API_URL?.trim();
const useDevProxy =
  import.meta.env.DEV && import.meta.env.VITE_USE_PROXY !== "false";

// Dev + proxy Vite: request same-origin (/api, /health) → hindari CORS
// Production: kosongkan VITE_API_URL jika pakai reverse proxy, atau isi URL backend
const API_BASE = useDevProxy
  ? ""
  : rawApiBase
    ? rawApiBase.replace(/\/$/, "")
    : "";

let healthPromise = null;
let lastHealthAt = 0;
const HEALTH_CACHE_MS = 15_000;

export async function checkHealth({ force = false } = {}) {
  const now = Date.now();
  if (!force && healthPromise && now - lastHealthAt < HEALTH_CACHE_MS) {
    return healthPromise;
  }

  lastHealthAt = now;
  healthPromise = fetch(`${API_BASE}/health`, { credentials: "include" })
    .then(async (res) => {
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.error || "API offline");
      return data;
    })
    .catch((err) => {
      healthPromise = null;
      throw err;
    });

  return healthPromise;
}

export { API_BASE };
