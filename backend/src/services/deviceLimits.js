import { db } from "../config/database.js";
import { getServerSettingsSync } from "./serverSettings.js";

/** Batas perangkat user: langganan aktif → paket → default server. */
export async function getUserMaxDevices(userId) {
  const sub = await db.query(
    `SELECT COALESCE(s.max_devices, p.max_devices) AS max_devices
     FROM subscriptions s
     LEFT JOIN packages p ON p.slug = LOWER(s.plan)
     WHERE s.user_id = $1 AND s.status = 'active' AND s.expires_at > NOW()
     ORDER BY s.expires_at DESC
     LIMIT 1`,
    [userId],
  );

  if (sub.rows[0]?.max_devices != null) {
    return Math.max(1, parseInt(sub.rows[0].max_devices, 10));
  }

  const freePkg = await db.query(
    "SELECT max_devices FROM packages WHERE slug = 'free' AND active = true LIMIT 1",
  );
  if (freePkg.rows[0]?.max_devices != null) {
    return Math.max(1, parseInt(freePkg.rows[0].max_devices, 10));
  }

  return getServerSettingsSync().maxDevices ?? 1;
}
