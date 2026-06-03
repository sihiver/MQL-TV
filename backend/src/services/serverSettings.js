import { db } from "../config/database.js";
import { parseDurationToSeconds } from "../utils/duration.js";

export const SETTINGS_DEFAULTS = {
  maxDevices: 3,
  rateLimit: 100,
  jwtExpiry: "1d",
  streamExpiry: "6h",
  epgSync: "6h",
  m3uRefresh: "12h",
  allowRegistration: true,
  requireEmailVerify: false,
  maintenanceMode: false,
  debugMode: false,
};

const ALLOWED_KEYS = new Set(Object.keys(SETTINGS_DEFAULTS));

let cached = { ...SETTINGS_DEFAULTS };
let cacheAt = 0;
const CACHE_MS = 15_000;

function maskUrl(url) {
  if (!url) return "—";
  return String(url).replace(/:([^:@/]+)@/, ":••••@");
}

function mergeSettings(stored = {}) {
  const out = { ...SETTINGS_DEFAULTS };
  for (const key of ALLOWED_KEYS) {
    if (stored[key] !== undefined && stored[key] !== null) {
      out[key] = stored[key];
    }
  }
  if (typeof out.maxDevices === "string") out.maxDevices = parseInt(out.maxDevices, 10) || SETTINGS_DEFAULTS.maxDevices;
  if (typeof out.rateLimit === "string") out.rateLimit = parseInt(out.rateLimit, 10) || SETTINGS_DEFAULTS.rateLimit;
  out.maxDevices = Math.max(1, Math.min(20, out.maxDevices));
  out.rateLimit = Math.max(10, Math.min(10_000, out.rateLimit));
  return out;
}

export function getServerSettingsSync() {
  return cached;
}

export async function getServerSettings() {
  if (Date.now() - cacheAt < CACHE_MS) return cached;
  try {
    const result = await db.query("SELECT data FROM server_settings WHERE id = 1");
    cached = mergeSettings(result.rows[0]?.data || {});
  } catch {
    cached = { ...SETTINGS_DEFAULTS };
  }
  cacheAt = Date.now();
  return cached;
}

export async function updateServerSettings(partial) {
  const current = await getServerSettings();
  const next = { ...current };
  for (const [key, value] of Object.entries(partial || {})) {
    if (!ALLOWED_KEYS.has(key)) continue;
    next[key] = value;
  }
  const merged = mergeSettings(next);
  await db.query(
    `INSERT INTO server_settings (id, data, updated_at)
     VALUES (1, $1::jsonb, NOW())
     ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data, updated_at = NOW()`,
    [JSON.stringify(merged)],
  );
  cached = merged;
  cacheAt = Date.now();
  return merged;
}

export function getPublicSettingsPayload(settings) {
  return {
    ...settings,
    dbUrl: maskUrl(process.env.DATABASE_URL),
    redisUrl: maskUrl(process.env.REDIS_URL),
    jwtExpirySeconds: parseDurationToSeconds(settings.jwtExpiry),
    streamExpirySeconds: parseDurationToSeconds(settings.streamExpiry) || 4 * 3600,
  };
}

/** Muat ke cache saat startup. */
export async function initServerSettings() {
  await getServerSettings();
}
