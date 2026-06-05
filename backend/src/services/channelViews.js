import { redis } from "../config/redis.js";

/** Jendela "sedang menonton" di dashboard (harus ada ping dalam interval ini). */
export const WATCH_ACTIVE_MINUTES = 3;

/** Hapus cache trending setelah ada tontonan baru agar unggulan langsung ter-update. */
export async function invalidateTrendingCache() {
  const keys = await redis.keys("channels:trending:v*");
  if (keys?.length) await redis.del(...keys);
}

/**
 * Catat mulai tontonan (analytics: maks. 1 baris baru per user+channel per 30 menit).
 */
export async function recordChannelView(db, userId, channelId) {
  const updated = await db.query(
    `UPDATE channel_views SET viewed_at = NOW()
     WHERE channel_id = $1 AND user_id = $2
       AND viewed_at > NOW() - INTERVAL '30 minutes'
     RETURNING id`,
    [channelId, userId],
  );
  if (updated.rowCount > 0) {
    invalidateTrendingCache().catch(() => {});
    return;
  }

  const inserted = await db.query(
    `INSERT INTO channel_views (channel_id, user_id)
     SELECT $1, $2
     WHERE NOT EXISTS (
       SELECT 1 FROM channel_views
       WHERE channel_id = $1
         AND user_id = $2
         AND viewed_at > NOW() - INTERVAL '30 minutes'
     )
     RETURNING id`,
    [channelId, userId],
  );
  if (inserted.rowCount > 0) {
    invalidateTrendingCache().catch(() => {});
  }
}

/** Perpanjang sesi tontonan aktif (dipanggil berkala dari app TV). */
export async function touchChannelView(db, userId, channelId) {
  const updated = await db.query(
    `UPDATE channel_views SET viewed_at = NOW()
     WHERE channel_id = $1 AND user_id = $2
       AND viewed_at > NOW() - INTERVAL '10 minutes'
     RETURNING id`,
    [channelId, userId],
  );
  if (updated.rowCount > 0) return;

  await db.query(
    `INSERT INTO channel_views (channel_id, user_id) VALUES ($1, $2)`,
    [channelId, userId],
  );
}

/**
 * User keluar player / app ke background.
 * Jangan hapus riwayat channel_views — dipakai untuk unggulan/trending.
 * Dashboard "sedang menonton" otomatis hilang setelah WATCH_ACTIVE_MINUTES tanpa ping.
 */
export async function clearChannelView(_db, _userId) {
  // no-op: riwayat tontonan tetap disimpan untuk perhitungan unggulan
}

/**
 * Channel paling banyak ditonton dalam N hari terakhir (sesuai paket langganan).
 */
export async function getTrendingChannels(db, access, { limit = 10, days = 30 } = {}) {
  if (!access.hasPackage) {
    return [];
  }

  const params = [];
  const conditions = [
    "c.active = true",
    "COALESCE(c.is_live, true) = true",
  ];

  if (!access.includesAll) {
    params.push(access.packageId);
    conditions.push(
      `c.id IN (SELECT channel_id FROM package_channels WHERE package_id = $${params.length})`,
    );
  }

  params.push(days);
  const daysIdx = params.length;
  params.push(limit);
  const limitIdx = params.length;

  const where = conditions.join(" AND ");

  // Satu baris per nama channel (hindari duplikat import M3U dengan id berbeda)
  const result = await db.query(
    `WITH channel_stats AS (
       SELECT c.id, c.name, c.category, c.logo_url, c.is_live, c.viewer_count,
              COUNT(cv.id)::int AS views_30d
       FROM channels c
       LEFT JOIN channel_views cv
         ON cv.channel_id = c.id
        AND cv.viewed_at > NOW() - make_interval(days => $${daysIdx}::int)
       WHERE ${where}
       GROUP BY c.id
     ),
     deduped AS (
       SELECT *,
              ROW_NUMBER() OVER (
                PARTITION BY LOWER(TRIM(name))
                ORDER BY views_30d DESC, id ASC
              ) AS rn
       FROM channel_stats
     )
     SELECT id, name, category, logo_url, is_live, viewer_count, views_30d
     FROM deduped
     WHERE rn = 1 AND views_30d > 0
     ORDER BY views_30d DESC, name ASC
     LIMIT $${limitIdx}`,
    params,
  );

  return result.rows;
}
