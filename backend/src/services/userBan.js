import { db } from "../config/database.js";
import { redis } from "../config/redis.js";

const banKey = (userId) => `banned:${userId}`;

/** Cek apakah user diblokir (Redis dulu, fallback DB). */
export async function isUserBanned(userId) {
  if ((await redis.get(banKey(userId))) === "1") return true;

  const row = await db.query("SELECT banned FROM users WHERE id = $1", [userId]);
  if (!row.rows.length) return true;

  const banned = !!row.rows[0].banned;
  if (banned) await redis.set(banKey(userId), "1");
  return banned;
}

/** Terapkan status ban & cabut refresh token agar sesi lama tidak bisa dipakai. */
export async function applyUserBanState(userId, banned) {
  if (banned) {
    await redis.set(banKey(userId), "1");
    await redis.del(`refresh:${userId}`);
  } else {
    await redis.del(banKey(userId));
  }
}
