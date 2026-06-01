/**
 * Catat tontonan channel (maks. 1 entri per user+channel per 30 menit).
 */
export async function recordChannelView(db, userId, channelId) {
  await db.query(
    `INSERT INTO channel_views (channel_id, user_id)
     SELECT $1, $2
     WHERE NOT EXISTS (
       SELECT 1 FROM channel_views
       WHERE channel_id = $1
         AND user_id = $2
         AND viewed_at > NOW() - INTERVAL '30 minutes'
     )`,
    [channelId, userId],
  );
}

/**
 * Channel paling banyak ditonton dalam N hari terakhir (sesuai paket langganan).
 */
export async function getTrendingChannels(db, access, { limit = 10, days = 30 } = {}) {
  if (!access.hasPackage) {
    return [];
  }

  const params = [];
  const conditions = ["c.active = true"];

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
                ORDER BY views_30d DESC, viewer_count DESC, id ASC
              ) AS rn
       FROM channel_stats
     )
     SELECT id, name, category, logo_url, is_live, viewer_count, views_30d
     FROM deduped
     WHERE rn = 1
     ORDER BY views_30d DESC, viewer_count DESC, name ASC
     LIMIT $${limitIdx}`,
    params,
  );

  return result.rows;
}
