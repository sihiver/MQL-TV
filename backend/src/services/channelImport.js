import { db } from "../config/database.js";
import { redis } from "../config/redis.js";

const BATCH_SIZE = 200;

async function clearChannelCache() {
  const keys = await redis.keys("channels:*");
  if (keys.length) await redis.del(...keys);
}

/**
 * Insert batch channel ke DB.
 * @param {Array} rows - { name, stream_url, category, logo_url, drm_type, drm_key, user_agent, referer, source_category, epg_id? }
 */
export async function importChannelRows(rows, { mode = "replace", meta = {} } = {}) {
  if (!rows?.length) {
    throw new Error("Tidak ada channel untuk diimpor");
  }

  const client = await db.connect();
  try {
    await client.query("BEGIN");

    if (mode === "replace") {
      await client.query("TRUNCATE channels RESTART IDENTITY CASCADE");
    }

    for (let i = 0; i < rows.length; i += BATCH_SIZE) {
      const batch = rows.slice(i, i + BATCH_SIZE);
      const values = [];
      const params = [];

      batch.forEach((ch, j) => {
        const b = j * 10;
        values.push(
          `($${b + 1},$${b + 2},$${b + 3},$${b + 4},$${b + 5},$${b + 6},$${b + 7},$${b + 8},$${b + 9},$${b + 10}, true, true)`,
        );
        params.push(
          ch.name,
          ch.stream_url,
          ch.category,
          ch.logo_url,
          ch.drm_type,
          ch.drm_key,
          ch.user_agent,
          ch.referer,
          ch.source_category,
          ch.epg_id || null,
        );
      });

      await client.query(
        `INSERT INTO channels (
           name, stream_url, category, logo_url,
           drm_type, drm_key, user_agent, referer, source_category, epg_id,
           active, is_live
         ) VALUES ${values.join(", ")}`,
        params,
      );
    }

    await client.query("COMMIT");
  } catch (err) {
    await client.query("ROLLBACK");
    throw err;
  } finally {
    client.release();
  }

  await clearChannelCache();

  const { rows: countRows } = await db.query("SELECT COUNT(*)::int AS c FROM channels");
  return {
    imported: rows.length,
    totalInDb: countRows[0].c,
    mode,
    meta,
  };
}
